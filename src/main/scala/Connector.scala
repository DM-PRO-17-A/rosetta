package rosetta

import Chisel._

class AutoSimple(dataWidth: Int, valuesPerIteration: Int, thresholds: Array[Int], kernels: Int, weights1: Array[Array[Int]], weights2: Array[Array[Int]], input_size: Int, weight_size: Int, output_size: Int, input_width: Int) extends RosettaAccelerator {
  val numMemPorts = 0
  val io = new RosettaAcceleratorIF(numMemPorts) {
    // values to be compared
    val input = Decoupled(Vec.fill(input_size){UInt(INPUT, dataWidth)})
    // values to be output
    val output = Decoupled(Vec.fill(input_size){UInt(OUTPUT, dataWidth)})
  }

  val t = Vec(thresholds.map(s => UInt(s, dataWidth)))

  // sets all values in output to be 0, NECESSARY to avoid error
  for(i <- 0 until 3072) {
    io.output.bits := UInt(0)
  }

	val FC1 = Module(new FullyConnected(kernels, weights1, input_size, weight_size, output_size, input_width)).io
	FC1.input_data := io.input
	val BT = Module(new ComparatorWrapper(dataWidth, valuesPerIteration, thresholds)).io
	BT.input := FC1.output_data
	val FC2 = Module(new FullyConnected(kernels, weights2, input_size, weight_size, output_size, input_width)).io
	FC2.input_data.bits := BT.output.bits
	io.output := FC2.output_data
}

class AutoSimpleTest(c: AutoSimple) extends Tester(c) {
  
}


