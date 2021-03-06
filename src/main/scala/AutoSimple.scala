package rosetta

import Chisel._

class AutoSimple(kernels_path1: String, kernels_path2: String) extends RosettaAccelerator {
  val numMemPorts = 0

  val input_width = 8

  val kernels_per_it = 32
  val kernels_length = 256

  val input_per_it = 32
  val weights_length = 3072

  val io = new RosettaAcceleratorIF(numMemPorts) {
    // values to be compared
    val input_data = Vec.fill(input_per_it){Bits(INPUT, 8)}
    val input_pulse = Bool(INPUT)
    val output_pulse = Bool(INPUT)

    // values to be output
    val output_data = Vec.fill(43){SInt(OUTPUT)}
    val empty = Bool(OUTPUT)
    val full = Bool(OUTPUT)

    val out_pins = Vec.fill(4){UInt(INPUT, width=1)}
    val pcb_go = UInt(INPUT, width=1)
    val in_pins = Vec.fill(2){UInt(OUTPUT, width=1)}
    val pcb_btn = Vec.fill(3){UInt(OUTPUT, width=1)}
  }

  io.ck_out := Cat(io.out_pins(0), Cat(io.out_pins(1), Cat(io.out_pins(2), Cat(io.out_pins(3)))))
  io.ck_go := io.pcb_go

  io.in_pins(0) := io.ck_in(1)
  io.in_pins(1) := io.ck_in(0)

  io.pcb_btn(0) := io.pbtn(2)
  io.pcb_btn(1) := io.pbtn(1)
  io.pcb_btn(2) := io.pbtn(0)

  val IQ = Module(new ImageQueue(input_width, 128, input_per_it))
  IQ.io.input_data <> io.input_data
  IQ.io.input_pulse <> io.input_pulse

  val FC1 = Module(new FullyConnected(kernels_path1, kernels_length, weights_length, kernels_per_it, input_per_it, input_width))
  FC1.io.input <> IQ.io.output

  val BT = Module(new ComparatorWrapper(FC1.output_width, kernels_length))
  BT.io.input <> FC1.io.output

  val FC2 = Module(new FullyConnected(kernels_path2, 43, kernels_length, 1, kernels_length, 1))
  FC2.io.input <> BT.io.output

  val OQ = Module(new OutputQueue(FC2.output_width, 24, 43))
  OQ.io.input <> FC2.io.output
  OQ.io.output_data <> io.output_data
  OQ.io.output_pulse <> io.output_pulse

  io.empty <> OQ.io.empty
  io.full <> IQ.io.full
}


class AutoSimpleTest(c: AutoSimple) extends Tester(c) {
    def LoadResource(path: String) : Array[String] = {
        scala.io.Source.fromInputStream(this.getClass.getResourceAsStream(path)).getLines.toArray
    }

    //val input           = LoadResource("/test_data/mult_images.txt").map(line => line.split(" ").map(s => BigInt(s.toInt)).toArray).toArray
    val input           = LoadResource("/test_data/input.txt").map(line => line.split(" ").map(s => BigInt(s.toInt)).toArray).toArray
    val fc_1_weights    = LoadResource("/test_data/fc1.txt").map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))
    val fc_2_weights    = LoadResource("/test_data/fc2.txt").map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))
    val thresholds      = LoadResource("/test_data/bt.txt").map(t => t.split(" ").map(_.toInt)).toArray.head
    val expected_output = LoadResource("/test_data/result.txt").map(line => line.split(" ").map(s => BigInt(s.toInt)).toArray).toArray

    val images                = input.length
    println(images)
    val kernels_per_iteration = 32
    val kernels_length        = 256
    val input_size            = 32
    val weights_length        = 3072

    def result_n(n: Int) : Array[BigInt] = {
      val fc_1 = fc_1_weights.map(kernel => (input(n).slice(0, weights_length) zip kernel.slice(0, weights_length))
        .map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _)
      )
      val bt_output_real = (fc_1 zip thresholds.slice(0, kernels_length))
        .map{case (i1: BigInt, i2: Int) => if (i2 > i1) -1 else 1}

      val fc_2 = fc_2_weights.map(kernel => (bt_output_real zip kernel)
        .map{case (i1: Int, i2: Int) => i1*i2}
        .reduceLeft(_ + _))
        .map(i => BigInt(i)).toArray
      return fc_2

    }

    // Insert image into IQ
    for (n <- 0 until 1) {
      val offset = 0
      for (i <- 0 until weights_length by input_size) {
        while (peek(c.io.full) == 1) {
          step(1)
        }
        poke(c.io.input_pulse, 1)
        step(1)
        poke(c.io.input_data, input(n).slice(offset + i, offset + i + input_size))
        poke(c.io.input_pulse, 0)
        step(1)
      }
    }

    peek(c.FC1.state)
    peek(c.FC1.kernel_step)
    step(15000)
    peek(c.FC1.state)
    peek(c.FC1.kernel_step)

    for (n <- 0 until 1) {
      poke(c.io.output_pulse, 1)
      step(1)
      poke(c.io.output_pulse, 0)
      step(1)
      //val FC_N_Output = FC1_Result(n)
      val res = result_n(n)
      for (k <- 0 until 43) {
        expect(c.io.output_data(k), expected_output(n)(k))
        expect(c.io.output_data(k), res(k))
      }
    }
    expect(c.IQ.queue.io.count, 0)
    expect(c.OQ.queue.io.count, 0)
}


