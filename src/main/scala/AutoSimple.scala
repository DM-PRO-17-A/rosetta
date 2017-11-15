package rosetta

import Chisel._

class AutoSimple(kernels_path1: String, kernels_path2: String) extends RosettaAccelerator {
  val numMemPorts = 0

  val input_width = 8

  val kernels_per_it = 8
  val kernels_length = 16

  val input_per_it = 32
  val weights_length = 128

  val output_width = math.ceil(math.log(weights_length * math.pow(2, input_width)) / math.log(2)).toInt + 1

  val io = new RosettaAcceleratorIF(numMemPorts) {
    // values to be compared
    val input_data = Vec.fill(32){SInt(INPUT, 8)}
    val input_pulse = Bool(INPUT)
    val output_pulse = Bool(INPUT)

    // values to be output
    val output_data = Vec.fill(16){Bits(OUTPUT, output_width)}
    val empty = Bool(OUTPUT)
    val full = Bool(OUTPUT)
  }

  val IQ = Module(new ImageQueue(input_width, 128, input_per_it)) // 128 * input_size numbers
  IQ.io.input_data <> io.input_data
  IQ.io.input_pulse <> io.input_pulse

  val FC1 = Module(new FullyConnected(kernels_path1, kernels_length, weights_length, kernels_per_it, input_per_it, input_width))
  FC1.io.input <> IQ.io.output

  //val BT = Module(new ComparatorWrapper(21, 16)).io
  //BT.input <> FC1.output

  //val FC2 = Module(new FullyConnected(kernels_path2, 43, 16, 43, 16, 1)).io
  //FC2.input <> BT.output

  val OQ = Module(new OutputQueue(FC1.output_width, 24, 16))
  OQ.io.input <> FC1.io.output
  OQ.io.output_data <> io.output_data
  OQ.io.output_pulse <> io.output_pulse

  io.empty <> OQ.io.empty
  io.full <> IQ.io.full
}


class AutoSimpleTest(c: AutoSimple) extends Tester(c) {
    def LoadResource(path: String) : Array[String] = {
        scala.io.Source.fromInputStream(this.getClass.getResourceAsStream(path)).getLines.toArray
    }

    val input           = LoadResource("/test_data/fc1input60.txt").head.split(" ").map(s => BigInt(s.toInt))
    val fc_1_weights    = LoadResource("/test_data/fc1.txt").map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))
    val fc_2_weights    = LoadResource("/test_data/fc2.txt").map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))
    val thresholds      = LoadResource("/test_data/thresholds.txt").map(t => t.split(", ").map(_.toInt)).toArray.head
    val expected_output = LoadResource("/test_data/fc2output60.txt").head.split(" ").map(s => BigInt(s.toInt))

    val images                = 24
    val kernels_per_iteration = 8
    val kernels_length        = 16
    val input_size            = 32
    val weights_length        = 128

    def FC1_Result(n: Int) : Array[BigInt] = {
      val offset = n * weights_length
      // n = 0, 0-128
      // n = 1, 128-256
      // n = 2, 256-184
      return fc_1_weights.map(kernel => (input.slice(offset, offset + weights_length) zip kernel.slice(0, weights_length))
        .map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _)
      )
    }

    val fc_1 = FC1_Result(0)

    val bt_output = (fc_1 zip thresholds.slice(0, kernels_length))
      .map{case (i1: BigInt, i2: Int) => if (i2 > i1) 0 else 1}
    val bt_output_real = (fc_1 zip thresholds.slice(0, kernels_length))
      .map{case (i1: BigInt, i2: Int) => if (i2 > i1) -1 else 1}

    val fc_2 = fc_2_weights.map(kernel => (bt_output_real zip kernel.slice(0, 16))
      .map{case (i1: Int, i2: Int) => i1*i2}
      .reduceLeft(_ + _))
      .map(i => BigInt(i)).toArray

    // Insert image into IQ
    for (n <- 0 until images) {
      val offset = n * weights_length
      for (i <- 0 until weights_length by input_size) {
        poke(c.io.input_pulse, 1)
        step(1)
        poke(c.io.input_data, input.slice(offset + i, offset + i + input_size))
        println(offset + i, offset + i + input_size)
        poke(c.io.input_pulse, 0)
        step(1)
      }
    }

    var n = 0
    // While there is still shit in the input queue
    while (peek(c.IQ.io.empty) == 0) {
      step(1)

      // If theres something in the output queue, we can check it for lulz!
      while(peek(c.OQ.queue.io.count) > 0) {
        poke(c.io.output_pulse, 1)
        step(1)
        poke(c.io.output_pulse, 0)
        val FC_N_Output = FC1_Result(n)
        for (k <- 0 until kernels_length) {
          expect(c.io.output_data(k), FC_N_Output(k))
        }
        n += 1
        step(1)
      }

      // While waiting for the current calculation to finish
      while(peek(c.FC1.io.output.valid) == 0) {
        step(1)
      }

      step(1)
    }
    while(peek(c.OQ.queue.io.count) > 0) {
      poke(c.io.output_pulse, 1)
      step(1)
      poke(c.io.output_pulse, 0)
      val FC_N_Output = FC1_Result(n)
      for (k <- 0 until kernels_length) {
        expect(c.io.output_data(k), FC_N_Output(k))
      }
      n += 1
      step(1)
    }
}


