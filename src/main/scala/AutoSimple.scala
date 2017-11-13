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


  val FC2 = Module(new FullyConnected(kernels_path2, 43, 16, 43, 16, 1)).io
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
          step(5) // Some delay while changing input

          // Poke input and set valid to 1!
          poke(c.io.input.bits, input.slice(i, i + input_size))
          poke(c.io.input.valid, 1)
          step(1) // First step, move from waiting state to calc state

          while (peek(c.io.input.ready) == 0) {
            step(1) // In calc state, ready == 0 until we're done with current input
          }

          poke(c.io.input.valid, 0) // As soon as ready == 1, the previous component has to set valid to 0 or provide the next input immediately

          step(10)
      }
      step(1)

      step(10)
    }
    val fc_1_weights = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1.txt")).getLines.toArray.map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))
    val fc_2_weights = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc2.txt")).getLines.toArray.map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))
    val thresholds = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/thresholds.txt")).getLines.toArray.map(t => t.split(", ").map(_.toInt)).toArray.head

    val fc_1 = fc_1_weights.map(kernel => (input.slice(0, weights_length) zip kernel.slice(0, weights_length)).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _))
    for (k <- 0 until kernels_length) {
      expect(c.FC1.output_data.bits(k), fc_1(k))
    }

    val bt_output = (fc_1 zip thresholds.slice(0, kernels_length)).map{case (i1: BigInt, i2: Int) => if (i2 > i1) 0 else 1}
    val bt_output_real = (fc_1 zip thresholds.slice(0, kernels_length)).map{case (i1: BigInt, i2: Int) => if (i2 > i1) -1 else 1}
    for (k <- 0 until kernels_length) {
      expect(c.BT.output.bits(k), bt_output(k))
    }


    val fc_2 = fc_2_weights.map(kernel => (bt_output_real zip kernel.slice(0, 16)).map{case (i1: Int, i2: Int) => i1*i2}.reduceLeft(_ + _)).map(i => BigInt(i)).toArray
    for (i <- 0 until 16) {
      expect(c.FC2.output_data.bits(i), fc_2(i))
    }
}


