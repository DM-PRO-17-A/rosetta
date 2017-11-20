package rosetta

import Chisel._

class WindowSelector() extends RosettaAccelerator {
  val numMemPorts = 0
  val channels = 3
  val input_dim = 32
  val width = 8
  val pixel_width = channels * width

  val filter_dim = 6
  val stride = 2

  val output_dim = (input_dim - filter_dim) / stride + 1
  println(output_dim)

  val input_width = input_dim * channels * width
  val filter_row_width = filter_dim * channels * width
  val output_width = filter_dim * filter_dim * channels * width

  val io = new RosettaAcceleratorIF(numMemPorts) {
    val input = Decoupled(Bits(INPUT, input_width)).flip // one row from the 32*32*3 img

    val output = Decoupled(Bits(OUTPUT, output_width)) // one window, 5*5*3
  }
  io.input.ready := Bool(true)
  io.output.bits := Bits(0, width=output_width)

  val rows = Array.fill(filter_dim){Reg(init=Bits(width=input_width))}

  val row_counter = Reg(init=UInt(0, width=log2Up(input_dim + 1)))
  val next_input = Reg(init=UInt(0, width=log2Up(stride + 1)))
  val x_pos = Reg(init=(UInt(0, width=log2Up(output_dim + 1))))

  val init :: ready :: valid :: Nil = Enum(UInt(), 3) // Sexy
  val state = Reg(init = init)
  val x_start = input_width - 1
  val x_end = input_width - filter_row_width


  switch (state) {
    is (init) {
      when (io.input.valid) {
        rows(0) := io.input.bits
        for (r <- 1 until filter_dim) {
          rows(r) := rows(r - 1)
        }

        row_counter := row_counter + UInt(1)
        when (row_counter === UInt(filter_dim-1)) {
          state := valid
          io.input.ready := Bool(false)
        } .otherwise {
          io.input.ready := Bool(true)

        }
      }
    }

    is (ready) {
      when (io.input.valid) {
        when (next_input === UInt(stride - 1)) {
          state := valid
          x_pos := UInt(0)
        }
        row_counter := row_counter + UInt(1)

        rows(0) := io.input.bits
        for (r <- 1 until filter_dim) {
          rows(r) := rows(r - 1)
        }
        next_input := next_input + UInt(1)
      }
    }

    is (valid) {
      switch (x_pos) {
        for (x_i <- 0 until 14) {
          is (UInt(x_i)) {
            val offset = x_i * pixel_width * 2
            io.output.bits := (filter_dim-1 to 0 by -1).map(i => rows(i)(x_start - offset, x_end - offset)).foldLeft(Bits(width=output_width)){ (a, b) => Cat(a,b) }
          }
        }
      }
      when (x_pos === UInt(output_dim)) {
        when (row_counter === UInt(input_dim)) {
          state := init
        } .otherwise {
          state := ready
          next_input := UInt(0)
        }
      }
      when (io.output.ready) {
        x_pos := x_pos + UInt(1)
      }
    }
  }


}

class WindowSelectorTests(c: WindowSelector) extends Tester(c) {
  def toBinary(i: Int, digits: Int = 8) =
        String.format("%" + digits + "s", i.toBinaryString).replace(' ', '0')

  val filter_dim = 6

  val input = (0 to 32*32*3).map(_ => toBinary((math.random * (30-20) + 20).toInt)).toArray.grouped(32*3).map(g => g.mkString).toArray
  poke(c.io.input.valid, 1)
  for (r <- 0 until filter_dim) {
    poke(c.io.input.bits, BigInt(input(r), 2))
    step(1)
  }
  poke(c.io.input.valid, 0)
  for (r <- 0 until filter_dim) {
    expect(c.rows(filter_dim - r - 1), BigInt(input(r), 2))
  }
  for (x_i <- 0 until 14) {
    poke(c.io.output.ready, 0)
    step(1)
    expect(c.io.output.bits, BigInt((0 until filter_dim).map(i => input(i).slice(3*8*x_i*2, 6*3*8 + 3*8*x_i*2)).mkString, 2))
    poke(c.io.output.ready, 1)
    step(1)
  }
  poke(c.io.output.ready, 0)
  for (y_i <- 0 until 13) {
    step(1)
    expect(c.state, 1)
    poke(c.io.input.valid, 1)
    poke(c.io.input.bits, BigInt(input(filter_dim + y_i * 2), 2))
    step(1)
    poke(c.io.input.bits, BigInt(input(filter_dim + y_i * 2 + 1), 2))
    step(1)
    poke(c.io.input.valid, 0)

    for (r <- 0 until filter_dim) {
      expect(c.rows(filter_dim - r - 1), BigInt(input(r + (y_i + 1) * 2), 2))
    }

    step(1)
    expect(c.state, 2)
    expect(c.x_pos, 0)

    for (x_i <- 0 until 14) {
      expect(c.io.output.bits, BigInt((0 until filter_dim).map(i => input(i+(y_i+1)*2).slice(3*8*x_i*2, 6*3*8 + 3*8*x_i*2)).mkString, 2))
      poke(c.io.output.ready, 1)
      step(1)
      poke(c.io.output.ready, 0)
      step(1)
    }
  }
  peek(c.row_counter)
  expect(c.state, 0)
}
