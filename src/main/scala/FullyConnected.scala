package rosetta

import Chisel._

// These param names are really bad
class FullyConnected(kernels_path: String, kernels_length: Int, weights_length: Int, kernels_per_it: Int, input_per_it: Int, input_width: Int) extends RosettaAccelerator {

    // Map the kernels / weights to UInt
    val kernels = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream(kernels_path)).getLines.toArray.slice(0, kernels_length)
        .map(kernel =>
          kernel.split(" ").toArray.slice(0, weights_length).grouped(input_per_it).map(group => UInt(BigInt(group.mkString, 2), width=input_per_it)).toArray
          //kernel.split(" ").map(i => UInt(i.toInt, width=1)).toArray.slice(0, weights_length)
        )

    val output_width = math.ceil(math.log(weights_length * math.pow(2, input_width)) / math.log(2)).toInt + 1

    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val input_data = Flipped(Decoupled(Vec.fill(input_per_it){UInt(INPUT, input_width)}))

        val output_data = Decoupled(Vec.fill(kernels_length){SInt(OUTPUT, output_width)})
    }

    val done :: waiting :: calc :: Nil = Enum(UInt(), 3) // Sexy
    val state = Reg(init = waiting)

    val current_weight = Reg(init=UInt(0, width=log2Up(weights_length) + 1))
    val current_kernel = Reg(init=UInt(0, width=log2Up(kernels_length) + 1))


    if (kernels_length == kernels_per_it && weights_length == input_per_it) {
        val dot_prods = Array.fill(kernels_per_it){Module(new DotProduct(input_per_it, input_width)).io}

        for (k <- 0 until kernels_per_it) {
            dot_prods(k).vec_1 := io.input_data.bits
            dot_prods(k).vec_2 := kernels(k)(0)
            io.output_data.bits(k) := dot_prods(k).data_out
        }

        io.output_data.valid := io.input_data.valid
        io.input_data.ready := io.output_data.ready

    } else {
        val acc = Array.fill(kernels_length){Reg(init=SInt(0, width=output_width))}

        val dot_prods = Array.fill(kernels_per_it){Module(new DotProduct(input_per_it, input_width)).io}
        for (k <- 0 until kernels_per_it) {
            dot_prods(k).vec_1 := io.input_data.bits
            dot_prods(k).vec_2 := UInt(0, width=input_per_it)
        }

        for (i <- 0 until kernels_length) {
              io.output_data.bits(i) := acc(i)
        }

        // Default values
        io.output_data.valid := Bool(false)
        io.input_data.ready := Bool(true)

        val weight_step = current_weight / UInt(input_per_it) * UInt(kernels_length) + current_kernel
        switch (weight_step) {
          for (w <- 0 until weights_length by input_per_it) {
            for (k <- 0 until kernels_length by kernels_per_it) {
              is (UInt(w / input_per_it * kernels_length + k, width=log2Up(weights_length / input_per_it * kernels_length + kernels_length))) {
                for (k_i <- 0 until kernels_per_it) {
                  dot_prods(k_i).vec_2 := kernels(k + k_i)(w / input_per_it)
                }
              }
            }
          }
        }


        switch(state) {
            is (done) {
              io.output_data.valid := Bool(true)
              when (io.output_data.ready) {
                // Reset everything and change state!
                for (k <- 0 until kernels_length) {
                  acc(k) := UInt(0)
                }
                current_weight := UInt(0)
                current_kernel := UInt(0)
                state := waiting
              }
            }
            is (waiting) {
              io.input_data.ready := Bool(true)
              when (io.input_data.valid) {
                state := calc
                current_kernel := UInt(0)
              }
            }
            is (calc) {
                when (current_kernel === UInt(kernels_length)) {
                  when (current_weight === UInt(weights_length - input_per_it)) {
                    state := done
                    io.input_data.ready := Bool(false)
                  } .otherwise {
                    io.input_data.ready := Bool(true)
                    state := waiting
                  }

                  current_weight := current_weight + UInt(input_per_it)

                } .otherwise {
                  when (io.input_data.valid) {
                    switch (weight_step % UInt(kernels_length)) {
                      for (k <- 0 until kernels_length by kernels_per_it) {
                        is (UInt(k, width=log2Up(kernels_length))) {
                          for (k_i <- 0 until kernels_per_it) {
                            acc(k + k_i) := acc(k + k_i) + dot_prods(k_i).data_out
                          }
                        }
                      }
                    }

                    current_kernel := current_kernel + UInt(kernels_per_it)
                    io.input_data.ready := Bool(false)
                    io.output_data.valid := Bool(false)
                }
              }
            }
        }

    }


}

class FullyConnectedTests(c: FullyConnected) extends Tester(c) {
    val input_data = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1input60.txt")).getLines.toArray.head.split(" ").map(s => BigInt(s.toInt))
    val kernels = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1.txt")).getLines.toArray.map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))

    val input_size = 64
    val kernels_per_iteration = 32
    val kernels_length = 256 
    val weights_length = 3072

    for (image <- 0 until 1) {
        poke(c.io.output_data.ready, 1) // The next component is ready
        step(1)
        for (k <- 0 until kernels_length) {
          expect(c.io.output_data.bits(k), 0)
        }
        for (i <- 0 until weights_length by input_size) {
            //expect(c.io.output_data.valid, 0)
            //expect(c.io.input_data.ready, 1)
            //step(5) // Some delay while changing input

            // Poke input and set valid to 1!
            poke(c.io.input_data.bits, input_data.slice(i, i + input_size))
            poke(c.io.input_data.valid, 1)
            step(1) // First step, move from waiting state to calc state

            while (peek(c.io.input_data.ready) == 0) {
              step(1) // In calc state, ready == 0 until we're done with current input
              peek(c.state)
              peek(c.current_kernel)
              peek(c.current_weight)
            }

            poke(c.io.input_data.valid, 0) // As soon as ready == 1, the previous component has to set valid to 0 or provide the next input immediately

            // Nothing happens even after 10 steps, we're in the frame_wait state, and we can verify that!
            for (k <- 0 until kernels_length by kernels_per_iteration) {
                for (k_i <- 0 until kernels_per_iteration) {
                    expect(c.io.output_data.bits(k + k_i), (input_data.slice(0, i+input_size) zip kernels(k + k_i).slice(0, i+input_size)).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _))
                }
            }
            step(1)
            if (peek(c.io.output_data.valid) == 1) {
              poke(c.io.output_data.ready, 0) // The next component has received the input
            }
        }
        // Output data should now be valid!
        peek(c.done)
        peek(c.state)
        peek(c.current_kernel)
        peek(c.current_weight)
    }
}
