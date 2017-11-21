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
  val output_width = filter_dim * channels * width // one row of 6*6*3

  val io = new RosettaAcceleratorIF(numMemPorts) {
    val input = Decoupled(Bits(INPUT, input_width)).flip // one row from the 32*32*3 img

    val output = Decoupled(Vec.fill(filter_dim){Bits(OUTPUT, output_width)}) // one window, 6*6*3
  }
  io.input.ready := Bool(true)
  io.output.valid := Bool(false)
  io.output.bits := Bits(0, width=output_width)

  val rows = Array.fill(filter_dim){Reg(init=Bits(width=input_width))}

  val row_counter = Reg(init=UInt(0, width=log2Up(input_dim)))
  val next_input = Reg(init=UInt(0, width=log2Up(stride)))
  val x_pos = Reg(init=(UInt(0, width=log2Up(output_dim + 1))))

  val init :: inc :: ready :: valid :: Nil = Enum(UInt(), 4) // Sexy
  val state = Reg(init = init)
  val x_start = input_width - 1
  val x_end = input_width - filter_row_width

  switch (x_pos) {
    for (x_i <- 0 until 14) {
      is (UInt(x_i)) {
        val offset = x_i * pixel_width * 2
        for (i <- 0 to filter_dim - 1) {
          io.output.bits(i) := rows(filter_dim - 1 - i)(x_start - offset, x_end - offset)
        }
      }
    }
  }

  switch (state) {
    is (init) {
      io.output.valid := Bool(false)
      io.input.ready := Bool(true)
      when (io.input.valid) {
        rows(0) := io.input.bits
        for (r <- 1 until filter_dim) {
          rows(r) := rows(r - 1)
        }
        row_counter := row_counter + UInt(1)

        when (row_counter === UInt(filter_dim - 1)) {
          state := valid
          io.input.ready := Bool(false)
        }
      }
    }

    is (ready) {
      io.output.valid := Bool(false)
      io.input.ready := Bool(true)
      when (io.input.valid) {

        rows(0) := io.input.bits
        for (r <- 1 until filter_dim) {
          rows(r) := rows(r - 1)
        }
        row_counter := row_counter + UInt(1)
        next_input := next_input + UInt(1)

        when (next_input === UInt(stride - 1)) {
          io.input.ready := Bool(false)
          x_pos := UInt(0)
          state := valid
        }
      }
    }


    is (valid) {
      io.output.valid := Bool(true)
      io.input.ready := Bool(false)
      when (io.output.ready) {
        when (x_pos === UInt(output_dim - 1)) {
          when (row_counter === UInt(output_width - 1)) {
            state := init
          } .otherwise {
            state := ready
            next_input := UInt(0)
            io.input.ready := Bool(true)
          }
        } .otherwise {
          x_pos := x_pos + UInt(1)
        }
      }
    }
  }


}

class WindowSelectorTests(c: WindowSelector) extends Tester(c) {
  def toBinary(i: Int, digits: Int = 8) =
        String.format("%" + digits + "s", i.toBinaryString).replace(' ', '0')

  val filter_dim = 6
  val stride = 2

  val input = (0 to 32*32*3).map(_ => toBinary((math.random * (30-20) + 20).toInt)).toArray.grouped(32*3).map(g => g.mkString).toArray
  for (y_i <- 0 until 14) {
    poke(c.io.output.ready, 0)
    step(1)
    if (y_i > 0) {
      expect(c.state, 2)
      expect(c.io.input.ready, 1)
      step(1)
      for (i <- 0 until stride) {
        poke(c.io.input.bits, BigInt(input(filter_dim + (y_i - 1) * 2 + i), 2))
        poke(c.io.input.valid, 1)
        step(1)
        poke(c.io.input.valid, 0)
        step(1)
      }

      for (k <- 0 until filter_dim) {
        println(y_i * 2 + k)
        expect(c.rows(filter_dim - 1 - k), BigInt(input(y_i * 2 + k).mkString, 2))
      }

      expect(c.io.output.valid, 1)
      expect(c.state, 3)
      expect(c.x_pos, 0)
      expect(c.row_counter, filter_dim + y_i * 2)
      for (x_i <- 0 until 14) {
        poke(c.io.output.ready, 0)
        step(1)
        for (i <- 0 until filter_dim) {
          expect(c.io.output.bits(i), BigInt(input(i+y_i*2).slice(3*8*x_i*2, 6*3*8 + 3*8*x_i*2).mkString, 2))
        }
        poke(c.io.output.ready, 1)
        step(1)
      }
    } else {
      expect(c.state, 0)
      poke(c.io.input.valid, 1)
      for (y_init <- 0 until filter_dim) {
        poke(c.io.input.bits, BigInt(input(y_init), 2))
        peek(c.row_counter)
        step(1)
      }
      poke(c.io.input.valid, 0)
      expect(c.state, 3)
      expect(c.io.output.valid, 1)
      step(1)
      for (k <- 0 until filter_dim) {
        expect(c.rows(filter_dim - 1 - k), BigInt(input(k).mkString, 2))
      }
      for (x_i <- 0 until 14) {
        poke(c.io.output.ready, 1)
        step(1)
        for (i <- 0 until filter_dim) {
          expect(c.io.output.bits(i), BigInt(input(i+y_i*2).slice(3*8*x_i*2, 6*3*8 + 3*8*x_i*2).mkString, 2))
        }
        poke(c.io.output.ready, 0)
        step(1)
      }
    }
  }
}
