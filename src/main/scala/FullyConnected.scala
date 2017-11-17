package rosetta

import Chisel._

// These param names are really bad
class FullyConnected(kernels_path: String, kernels_length: Int, weights_length: Int, kernels_per_it: Int, input_per_it: Int, input_width: Int) extends RosettaAccelerator {

    // Map the kernels / weights to UInt
    val kernels = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream(kernels_path)).getLines.toArray.slice(0, kernels_length)
        .map(kernel =>
          Vec(kernel.split(" ").toArray.slice(0, weights_length).grouped(input_per_it).map(group => UInt(BigInt(group.mkString, 2), width=input_per_it)).toArray)
        )

    val output_width = math.ceil(math.log(weights_length * math.pow(2, input_width)) / math.log(2)).toInt + 1

    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val input = Flipped(Decoupled(Vec.fill(input_per_it){UInt(INPUT, input_width)}))

        val output = Decoupled(Vec.fill(kernels_length){SInt(OUTPUT, output_width)})
    }

    val done :: waiting :: calc :: Nil = Enum(UInt(), 3) // Sexy
    val state = Reg(init = waiting)

    val weight_steps = weights_length / input_per_it
    val weight_step = Reg(init=UInt(0, width=log2Up(weight_steps)))

    val kernel_steps = kernels_length / kernels_per_it
    val kernel_step = Reg(init=UInt(0, width=log2Up(kernel_steps + 1)))

    val acc = Array.fill(kernels_length){Reg(init=SInt(0, width=output_width))}

    val dot_prods = Array.fill(kernels_per_it){Module(new DotProduct(input_per_it, input_width)).io}
    for (k <- 0 until kernels_per_it) {
        dot_prods(k).vec_1 := io.input.bits
        dot_prods(k).vec_2 := UInt(0, width=input_per_it)
    }

    for (k <- 0 until kernels_length) {
          io.output.bits(k) := acc(k)
    }

    // Default values
    io.output.valid := Bool(false)
    io.input.ready := Bool(true)

    switch(state) {
      is (done) {
        io.output.valid := Bool(true)
        io.input.ready := Bool(false)
        when (io.output.ready) {
          // Reset everything and change state!
          for (k <- 0 until kernels_length) {
            acc(k) := UInt(0)
          }
          weight_step := UInt(0)
          kernel_step := UInt(0)
          state := waiting
        }
      }
      is (waiting) {
        io.input.ready := Bool(true)
        when (io.input.valid) {
          io.input.ready := Bool(false)
          state := calc
          kernel_step := UInt(0)
        }
      }
      is (calc) {
        when (kernel_step === UInt(kernel_steps)) {
          when (weight_step === UInt(weight_steps - 1)) {
              state := done
            } .otherwise {
              state := waiting
            }

            weight_step := weight_step + UInt(1)

            } .otherwise {
              switch (kernel_step) {
                for (k <- 0 until kernel_steps) {
                  is (UInt(k)) {
                    for (k_i <- 0 until kernels_per_it) {
                      dot_prods(k_i).vec_2 := kernels(k * kernels_per_it + k_i)(weight_step)
                      acc(k * kernels_per_it + k_i) := acc(k * kernels_per_it + k_i) + dot_prods(k_i).output_data
                    }
                  }
                }
              }

              kernel_step := kernel_step + UInt(1)
              io.input.ready := Bool(false)
              io.output.valid := Bool(false)
            }
      }
    }


}

class FullyConnectedTests(c: FullyConnected) extends Tester(c) {
    val input = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1input60.txt")).getLines.toArray.head.split(" ").map(s => BigInt(s.toInt))
    val kernels = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1.txt")).getLines.toArray.map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))

    val input_size = 64
    val weights_length = 1024
    val kernels_per_iteration = 32
    val kernels_length = 64

    for (image <- 0 until 2) {
        poke(c.io.output.ready, 1) // The next component is ready
        step(1)
        for (k <- 0 until kernels_length) {
          expect(c.io.output.bits(k), 0)
        }
        for (i <- 0 until weights_length by input_size) {
            poke(c.io.input.bits, input.slice(i, i + input_size))
            poke(c.io.input.valid, 1)
            step(1)

            while (peek(c.io.input.ready) == 0) {
              step(1)
              peek(c.state)
              peek(c.kernel_step)
              peek(c.weight_step)
            }

            poke(c.io.input.valid, 0)

            for (k <- 0 until kernels_length by kernels_per_iteration) {
                for (k_i <- 0 until kernels_per_iteration) {
                    expect(c.io.output.bits(k + k_i), (input.slice(0, i+input_size) zip kernels(k + k_i).slice(0, i+input_size)).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _))
                }
            }
            step(1)
            if (peek(c.io.output.valid) == 1) {
              poke(c.io.output.ready, 0) // The next component has received the input
            }
        }
        // Output data should now be valid!
        peek(c.done)
        peek(c.state)
        peek(c.kernel_step)
        peek(c.weight_step)
    }
}
