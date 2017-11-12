package rosetta

import Chisel._

class AutoSimple(kernels_path: String, kernels_per_it: Int, input_per_it: Int, input_width: Int, dataWidth: Int, valuesPerIteration: Int) extends RosettaAccelerator {
  val numMemPorts = 0
  val io = new RosettaAcceleratorIF(numMemPorts) {
    // values to be compared
    val input = Flipped(Decoupled(Vec.fill(valuesPerIteration){SInt(INPUT, dataWidth)}))
    // values to be output
    val output = Decoupled(Vec.fill(valuesPerIteration){UInt(OUTPUT, dataWidth)})
  }

  // sets all values in output to be 0, NECESSARY to avoid error
  for(i <- 0 until 12) {
    io.output.bits := UInt(0)
  }

	val FC1 = Module(new FullyConnected(kernels_path: String, kernels_per_it: Int, input_per_it: Int, input_width: Int)).io
	FC1.input_data.bits := io.input.bits

	val BT = Module(new ComparatorWrapper(dataWidth, valuesPerIteration)).io
	BT.input.bits := FC1.output_data.bits

	val FC2 = Module(new FullyConnected(kernels_path: String, kernels_per_it: Int, input_per_it: Int, input_width: Int)).io
	FC2.input_data.bits := BT.output.bits
	io.output.bits := FC2.output_data.bits
}

class AutoSimpleTest(c: AutoSimple) extends Tester(c) {
  
}


