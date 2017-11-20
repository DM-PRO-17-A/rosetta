package rosetta

import Chisel._

class ConvolutionBuffer(output_width: Int) extends RosettaAccelerator {
  val numMemPorts = 0
 
  val io = new RosettaAcceleratorIF(numMemPorts) {
    val input = Decoupled(Bits(INPUT, 1)).flip

    val output = Decoupled(Bits(OUTPUT, output_width))
  }

  io.output.valid := Bool(false)
  io.input.ready := Bool(true)

  val ready :: valid :: Nil = Enum(UInt(), 2) // Sexy
  val state = Reg(init = ready)

  val output_reg = Reg(init=Bits(1, width=output_width))
  io.output.bits := output_reg
  val counter = Reg(init=UInt(0, width=output_width))

  switch (state) {
    is (valid) {
      io.output.valid := Bool(true)
      when (io.output.ready) {
        state := ready
      }
    }

    is (ready) {
      printf("its ready")
      output_reg := output_reg << io.input.bits
      counter := counter + UInt(1)
      when (counter === UInt(output_width - 1)) {
        state := valid
      }
    }
  }


}

class ConvolutionBufferTests(c: ConvolutionBuffer) extends Tester(c) {
  val test = BigInt(1)
  val test2 = BigInt(0)
  val test3 = BigInt(0)

  poke(c.io.input.bits, test)
  step(1)

  poke(c.io.input.bits, test2)
  step(1)

  poke(c.io.input.bits, test3)
  step(1)

  peek(c.io.output.bits)
  peek(c.output_reg)

}
