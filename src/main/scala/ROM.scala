package rosetta

import Chisel._

class ROM() extends RosettaAccelerator {
  val data = Array(0,1,0,1)
  val numMemPorts = 0

  val io = new RosettaAcceleratorIF(numMemPorts) {
    val dataOut = Vec.fill(data.length){ Bits(OUTPUT) }
  }

  data.zipWithIndex.foreach{ case (e, i) => io.dataOut(i) := Bits(e) }
}

class ROMTests(c: ROM) extends Tester(c) {
  expect(c.io.dataOut(0), 0)
  expect(c.io.dataOut(1), 1)
  expect(c.io.dataOut(2), 0)
  expect(c.io.dataOut(3), 1)
}

