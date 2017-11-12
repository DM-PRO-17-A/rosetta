package rosetta

import Chisel._

class AutoSimple(kernels_path1: String, kernels_path2: String) extends RosettaAccelerator {
  val numMemPorts = 0
  val io = new RosettaAcceleratorIF(numMemPorts) {
    // values to be compared
    val input = Flipped(Decoupled(Vec.fill(32){SInt(INPUT, 8)}))
    // values to be output
    val output = Decoupled(Vec.fill(43){UInt(OUTPUT, 21)})
  }

  // sets all values in output to be 0, NECESSARY to avoid error
  for(i <- 0 until 43) {
    io.output.bits := UInt(0)
  }

	val FC1 = Module(new FullyConnected(kernels_path1: String, 256, 3072, 16, 32, 8)).io
	FC1.input_data.bits := io.input.bits

	val BT = Module(new ComparatorWrapper(21, 256)).io
	BT.input <> FC1.output_data

	val FC2 = Module(new FullyConnected(kernels_path2: String, 43, 256, 43, 256, 1)).io
	FC2.input_data <> BT.output
	io.output.bits := FC2.output_data.bits
}

// 43 250
// 256, 43
class AutoSimpleTest(c: AutoSimple) extends Tester(c) {
  // val input_data = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1input60.txt")).getLines.toArray.head.split(" ").map(s => BigInt(s.toInt))
  // poke(c.io.input, input_data)
  // poke(c.io.input.valid, 1)
  // peek(c.io.output)
}


