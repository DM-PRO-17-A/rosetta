package rosetta

import Chisel._


class Convolution(data_width: Int, input_size: Int, filter_size: Int, channels: Int, filters: Int) extends RosettaAccelerator {
    // Load weights from file
    val weights = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/weights/convolution1.txt")).getLines
      .map(line => line.split(" ").map(i => if (i.toInt == 1) 1 else 0))
      .map(filter => UInt(BigInt(filter.mkString, 2), width=5*5*3)).toArray

    val numMemPorts = 0
    val input_width = data_width * input_size * channels // 8 bits * 6 * 3 - one row of 6*6*3
    val output_width = log2Up(filter_size * filter_size * channels * math.pow(2, data_width).toInt) // 5*5*3 * max value of each number
    val pixel_width = data_width * channels
    val window_width = filter_size * filter_size * channels * data_width // 5 * 5 * 3 * 8

    val io = new RosettaAcceleratorIF(numMemPorts){
        val input = Flipped(Decoupled(Vec.fill(input_size){Bits(INPUT, width=input_width)}))
        val output = Decoupled(Vec.fill(4 * filters){Bits(OUTPUT, width=output_width)})
    }
    io.input.ready := Bool(true)
    io.output.valid := Bool(false)

    val x_start = input_width - 1
    val x_end = input_width - pixel_width * filter_size
    val dot_products = Array.fill(4 * filters){Module(new DotProduct(5*5*3, data_width))}
    for (w <- 0 until 4) {
      for (f <- 0 until filters) {
        val x = w % 2
        val y = w / 2
        val offset = x * pixel_width
        val window = (0 until filter_size).map(i => io.input.bits(i + y)(x_start - offset, x_end - offset)).foldLeft(Bits(width=window_width)){(a, b) => Cat(a,b)}
        dot_products(w * filters + f).io.vec_1 := (0 until 5*5*3).map(i => window(window_width - 1 - i * 8, window_width - 8 - i*8))

        dot_products(w * filters + f).io.vec_2 := Bits(0)
        io.output.bits(w * filters + f) := dot_products(w * filters + f).io.output_data
      }
    }

    // Steps is equal to 20 divided by filters per iteration
    val steps = 20 / filters
    val step = Reg(init=UInt(0, width=log2Up(steps)))
    switch (step) {
      for (step_i <- 0 until steps) {
        is (UInt(step_i)) {
          for (w <- 0 until 4) {
            for (f <- 0 until filters) {
              dot_products(w * filters + f).io.vec_2 := weights(step_i * filters + f)
            }
          }
        }
      }
    }

    val ready :: calc :: Nil = Enum(UInt(), 2)
    val state = Reg(init = ready)

    switch(state) {
      is (ready) {
        io.input.ready := Bool(true)
        io.output.valid := Bool(false)
        when (io.input.valid) {
          io.input.ready := Bool(false)
          state := calc
          step := UInt(0)
        }
      }
      is (calc) {
        io.input.ready := Bool(false)
        io.output.valid := Bool(true)
        when (io.output.ready) {
          step := step + UInt(1)
          when (step === UInt(steps - 1)) {
            io.input.ready := Bool(true)
            state := ready
          }
        }
      }
    }


}

class ConvolutionTests(c: Convolution) extends Tester(c) {
    def toBinary(i: Int, digits: Int = 8) =
        String.format("%" + digits + "s", i.toBinaryString).replace(' ', '0')
    val filters = 1
    val windows = 4
    val filter_dim = 5
    val input = (0 to 32*32*3).map(_ => toBinary((math.random * (30-20) + 20).toInt)).toArray.grouped(32*3).map(g => g.mkString).toArray
    val input_window = (0 until 6).map(i => BigInt(input(i).slice(0, 6*3*8).mkString, 2)).toArray

    val weights = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/weights/convolution1.txt")).getLines
      .map(line => line.split(" ").map(_.toInt)).toArray

    for (y_i <- 0 until 14) {
      val input_window = (0 until 6).map(i => input(i+y_i*2).slice(0, 6*3*8)).toArray
      poke(c.io.input.valid, 1)
      poke(c.io.input.bits, (0 until 6).map(i => BigInt(input(i+y_i*2).slice(0, 6*3*8).mkString, 2)).toArray)
      step(1)
      poke(c.io.input.valid, 0)
      for (f <- 0 until 20) {
        peek(c.step)
        for (w <- 0 until windows) {
          val x = w % 2
          val y = w / 2
          val offset = x * 3*8
          val window = (0 until 5).map(i => input_window(i + y).slice(0 + offset, 5*3*8 + offset).grouped(8).map(v => BigInt(v.mkString, 2))).reduce(_++_).toArray
          val filter = weights(f)
          val result = (window zip filter).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _)
          println(result)
          expect(c.io.output.bits(w), result)
        }
        step(1)
        poke(c.io.output.ready, 1)
        step(1)
        poke(c.io.output.ready, 0)
      }
      peek(c.state)
    }
}
