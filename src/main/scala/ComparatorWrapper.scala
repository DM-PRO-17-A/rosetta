package rosetta

import Chisel._

class ComparatorWrapper(dataWidth: Int, pixelsPerSlice: Int, slicesPerIteration: Int, thresholds: Array[Int]) extends RosettaAccelerator {
  val numMemPorts = 0
  val io = new RosettaAcceleratorIF(numMemPorts) {
    // values to be compared
    val input = Vec.fill(pixelsPerSlice*slicesPerIteration){UInt(INPUT, dataWidth)}
    // values to be sent to pooling
    val output = Vec.fill(pixelsPerSlice){UInt(INPUT, dataWidth)}
  }

  val w = Vec(weights.map(s => UInt(s, data_width)))

  for(j <- 0 to pixelsPerSlice){
      val in = Module(new Comparator(dataWidth)).io
      in.in0 := 
    }

  
}

class ComparatorWrapperTest(c: ComparatorWrapper) extends Tester(c) {
  val test = Array[Int](1, 2, 3, 4)
  poke(c.io.input, in0)
  poke(c.io.in1, in1)
  expect(c.io.output, 1)
  peek(c.io.output)
}


