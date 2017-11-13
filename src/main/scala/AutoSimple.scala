package rosetta

import Chisel._

class AutoSimple(kernels_path1: String, kernels_path2: String) extends RosettaAccelerator {
  val numMemPorts = 0
  val io = new RosettaAcceleratorIF(numMemPorts) {
    // values to be compared
    val input = Flipped(Decoupled(Vec.fill(32){SInt(INPUT, 8)}))
    // values to be output
    val output = Decoupled(Vec.fill(16){UInt(OUTPUT, 21)})
  }

  // sets all values in output to be 0, NECESSARY to avoid error
  for(i <- 0 until 43) {
    io.output.bits := UInt(0)
  }

	val FC1 = Module(new FullyConnected(kernels_path1, 16, 128, 8, 32, 8)).io
	FC1.input_data <> io.input


	val BT = Module(new ComparatorWrapper(21, 16)).io
	BT.input <> FC1.output_data


	val FC2 = Module(new FullyConnected(kernels_path2, 43, 16, 43, 16, 21)).io
	FC2.input_data <> BT.output
	FC2.output_data <> io.output

}

class AutoSimpleTest(c: AutoSimple) extends Tester(c) {
  // val input = Array[BigInt](1, 2)
  // poke(c.io.input, input)
  // step(16)
  // poke(c.io.input.valid, 1)
  // peek(c.io.output)
  val input = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1input60.txt")).getLines.toArray.head.split(" ").map(s => BigInt(s.toInt))
  val output_result = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc2output60.txt")).getLines.toArray.head.split(" ").map(s => BigInt(s.toInt))

	// val kernels = scala.io.Source.fromInputStream(thi3s.getClass.getResourceAsStream("/test_data/fc1.txt")).getLines.toArray.map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))

	val input_size = 32
	val kernels_per_iteration = 8
	val kernels_length = 16
	val weights_length = 128

	for (image <- 0 until 1) {
	    poke(c.io.output.ready, 1) // The next component is ready
	    for (i <- 0 until weights_length by input_size) {
	        // expect(c.io.output.valid, 0)
	        // expect(c.io.input.ready, 1)
	        step(5) // Some delay while changing input

	        // Poke input and set valid to 1!
	        poke(c.io.input.bits, input.slice(i, i + input_size))
	        poke(c.io.input.valid, 1)
	        step(1) // First step, move from waiting state to calc state

	        while (peek(c.io.input.ready) == 0) {
	          step(1) // In calc state, ready == 0 until we're done with current input
	          // Keep in mind that the last iteration of this while loop will result in a test "failing". It is expected though it fucks up the "SUCCESS" metric
	        }

	        poke(c.io.input.valid, 0) // As soon as ready == 1, the previous component has to set valid to 0 or provide the next input immediately

	        // Nothing happens even after 10 steps, we're in the frame_wait state, and we can verify that!
	        step(10)
	        // for (k <- 0 until kernels_length by kernels_per_iteration) {
	        //     for (k_i <- 0 until kernels_per_iteration) {
	        //         // expect(c.io.output.bits(k + k_i), (input.slice(0, i+input_size) zip output_result(k + k_i).slice(0, i+input_size)).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _))
	        //         // peek(c.io.output)
	        //     }
	        // }
	    }
	    // Output data should now be valid!
	    // expect(c.io.output.valid, 1)

	    // Step once so the next component can react on the fact that output.valid == 1
	    step(1)
	    poke(c.io.output.ready, 0) // Indicating that the next component is not ready.

	    // Do some random steps to check that the output doesnt get changed before output.ready is 1 again
	    step(10)
	    for (k <- 0 until kernels_length) {
	        // expect(c.io.output.bits(k), (input.slice(0, weights_length) zip output_result(k).slice(0, weights_length)).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _))
	        // peek(c.io.output)
	    }
	  }
	  val weights = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1.txt")).getLines.toArray.map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))

	  val fc_1 = weights.map(kernel => (input.slice(0, weights_length) zip kernel.slice(0, weights_length)).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _))
		val thresholds = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/thresholds.txt")).getLines.toArray.map(t => t.split(", ").map(_.toInt)).toArray.head
		val bt_output = (fc_1 zip thresholds).map{case (i1: BigInt, i2: Int) => if (i2 > i1) -1 else 1}
    val kernels2 = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc2.txt")).getLines.toArray.map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))

		val fc_2 = (bt_output zip kernels2.slice(0, 16)).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _)
	  expect(c.io.output, fc2)
	  peek(c.BT.output)
	  peek(c.io.output)
	  peek(c.FC2.output_data)
}


