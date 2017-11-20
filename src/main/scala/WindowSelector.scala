package rosetta

import Chisel._

class WindowSelector() extends RosettaAccelerator {
  val numMemPorts = 0
  val channels = 3
  val input_dim = 32
  val width = 8
  val pixel_width = channels * width

  val filter_dim = 5

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

  val next_input = Bool(true)
  when (next_input) {
    // Next!
    rows(0) := io.input.bits
    for (r <- 1 until filter_dim) {
      rows(r) := rows(r - 1)
    }
  }

  val row_counter = Reg(init=UInt(0, width=log2Up(5)))
  val x_pos = Reg(init=(UInt(0, width=log2Up(input_dim))))

  val init :: ready :: valid :: Nil = Enum(UInt(), 3) // Sexy
  val state = Reg(init = init)

  switch (state) {
    is (init) {
      io.input.ready := Bool(true)
      when (row_counter === UInt(5-1)) {
        next_input := Bool(false)
        state := valid
      } .otherwise {
        next_input := Bool(true)
        row_counter := row_counter + UInt(1)
      }
    }

    is (valid) {
      val offset = UInt(0) // x_pos * UInt(0) does not work. Neither does UInt(0) * UInt(0)
      val x_start = UInt(input_width - 1) - offset
      val x_end = UInt(input_width - filter_row_width) - offset
      io.output.bits := (0 until filter_dim).map(i => rows(i)(x_start, x_end)).foldLeft(Bits(width=output_width)){ (a, b) => Cat(a,b) }
      when (io.output.ready) {
        x_pos := x_pos + UInt(0)
      }
    }
  }


}

class WindowSelectorTests(c: WindowSelector) extends Tester(c) {
  def toBinary(i: Int, digits: Int = 8) =
        String.format("%" + digits + "s", i.toBinaryString).replace(' ', '0')


  val input = (1 to 32*3).map(i => toBinary(i)).toArray.mkString
  for (r <- 1 until 5) {
    poke(c.io.input.bits, BigInt(input, 2))
    step(1)
    println(input.slice(0, 3*8))
    peek(c.row_counter)
    step(1)
  }
  expect(c.io.output.bits, BigInt(input.slice(0, 5*3*8) * 5, 2))
}
