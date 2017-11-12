package rosetta

import Chisel._

// These param names are really bad
class FullyConnected(kernels_path: String, kernels_length: Int, weights_length: Int, kernels_per_it: Int, input_per_it: Int, input_width: Int) extends RosettaAccelerator {

    // Map the kernels / weights to UInt
    val kernels = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream(kernels_path)).getLines.toArray.slice(0, kernels_length)
        .map(kernel =>
          kernel.split(" ").map(i => UInt(i.toInt, width=1)).toArray.slice(0, weights_length)
        )

    val output_width = math.ceil(math.log(weights_length * math.pow(2, input_width)) / math.log(2)).toInt + 1

    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val input_data = Flipped(Decoupled(Vec.fill(input_per_it){UInt(INPUT, input_width)}))

        val output_data = Decoupled(Vec.fill(kernels_length){SInt(OUTPUT, output_width)})
    }
    io.input_data.ready := Bool(false)
    io.output_data.valid := Bool(false)


    val acc = Array.fill(kernels_length){Reg(init=SInt(0, width=output_width))}


    for (i <- 0 until kernels_length) {
        io.output_data.bits(i) := acc(i)
    }


    val next_weight = Bool(false)
    val next_kernel = Bool(true)

    val current_weight = Reg(init=UInt(0, width=log2Up(weights_length)))
    val current_kernel = Reg(init=UInt(0, width=log2Up(kernels_length)))

    val im_wait :: frame_wait :: calc :: Nil = Enum(UInt(), 3) // Sexy
    val state = Reg(init = frame_wait)

    switch(state) {
        is(im_wait) {
            when(io.input_data.valid && io.output_data.ready) {
                // Received first input and the next element in the pipeline is ready
                for (k <- 0 until kernels_length) {
                  // Reset accumulators before begining to process next image
                  acc(k) := UInt(0)
                }
                io.input_data.ready := Bool(false) // Ie. we're not ready for next input!
                state := calc
            } .otherwise {
                io.output_data.valid := Bool(true)
                io.input_data.ready := Bool(true) // We are waiting for the next image and havent received anything yet
            }
        }
        is(frame_wait) {
            when(io.input_data.valid) {
                // Received next input
                io.input_data.ready := Bool(false)
                state := calc
            } .otherwise {
                io.input_data.ready := Bool(true)
            }
        }
        is(calc) {
            when(current_kernel === UInt(kernels_length - kernels_per_it)){
                io.input_data.ready := Bool(true) // At this step we are ready for next input
                when(current_weight === UInt(weights_length - input_per_it)){
                    // Done with image
                    state := im_wait
                } .otherwise {
                    // Done with weights for current kernels
                    state := frame_wait
                }
            }
        }
    }

    val dot_prods = Array.fill(kernels_per_it){Module(new DotProduct(input_per_it, input_width)).io}

    for (k <- 0 until kernels_per_it) {
        dot_prods(k).vec_1 := io.input_data.bits
        dot_prods(k).vec_2 := io.input_data.bits
    }

    val weight_step = current_weight / UInt(input_per_it) * UInt(kernels_length) + current_kernel

    switch (weight_step) {
      for (w <- 0 until weights_length by input_per_it) {
        for (k <- 0 until kernels_length by kernels_per_it) {
          is (UInt(w / input_per_it * kernels_length + k, width=log2Up(weights_length / input_per_it * kernels_length + kernels_length))) {
            for (k_i <- 0 until kernels_per_it) {
              for (i <- 0 until input_per_it) {
                dot_prods(k_i).vec_2(i) := kernels(k + k_i)(w + i)
              }
            }
          }
        }
      }
    }

    when(state === calc) {
      switch (weight_step % UInt(kernels_length)) {
        for (k <- 0 until kernels_length by kernels_per_it) {
          is (UInt(k, width=log2Up(kernels_length))) {
            for (k_i <- 0 until kernels_per_it) {
              acc(k + k_i) := acc(k + k_i) + dot_prods(k_i).data_out
            }
          }
        }
      }

      when (current_kernel === UInt(kernels_length - kernels_per_it)) {
          current_kernel := UInt(0)

          when (current_weight === UInt(weights_length - input_per_it)) {
              // We are done
              io.output_data.valid := Bool(true)
              current_weight := UInt(0)

          } .otherwise {
              current_weight := current_weight + UInt(input_per_it)
          }

      } .otherwise {
          current_kernel := current_kernel + UInt(kernels_per_it)
          io.output_data.valid := Bool(false)
      }
    }


}

class FullyConnectedTests(c: FullyConnected) extends Tester(c) {
    val input_data = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1input60.txt")).getLines.toArray.head.split(" ").map(s => BigInt(s.toInt))
    val kernels = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1.txt")).getLines.toArray.map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))

    val input_size = 16
    val kernels_per_iteration = 2
    val kernels_length = 12
    val weights_length = 256

    for (image <- 0 until 2) {
        poke(c.io.output_data.ready, 1) // The next component is ready
        for (i <- 0 until weights_length by input_size) {
            expect(c.io.output_data.valid, 0)
            expect(c.io.input_data.ready, 1)
            step(5) // Some delay while changing input

            // Poke input and set valid to 1!
            poke(c.io.input_data.bits, input_data.slice(i, i + input_size))
            poke(c.io.input_data.valid, 1)
            step(1) // First step, move from waiting state to calc state

            while (expect(c.io.input_data.ready, 0)) {
              step(1) // In calc state, ready == 0 until we're done with current input
              // Keep in mind that the last iteration of this while loop will result in a test "failing". It is expected though it fucks up the "SUCCESS" metric
            }

            poke(c.io.input_data.valid, 0) // As soon as ready == 1, the previous component has to set valid to 0 or provide the next input immediately

            // Nothing happens even after 10 steps, we're in the frame_wait state, and we can verify that!
            step(10)
            for (k <- 0 until kernels_length by kernels_per_iteration) {
                for (k_i <- 0 until kernels_per_iteration) {
                    expect(c.io.output_data.bits(k + k_i), (input_data.slice(0, i+input_size) zip kernels(k + k_i).slice(0, i+input_size)).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _))
                }
            }
        }
        // Output data should now be valid!
        expect(c.io.output_data.valid, 1)

        // Step once so the next component can react on the fact that output_data.valid == 1
        step(1)
        poke(c.io.output_data.ready, 0) // Indicating that the next component is not ready.

        // Do some random steps to check that the output doesnt get changed before output_data.ready is 1 again
        step(10)
        for (k <- 0 until kernels_length) {
            expect(c.io.output_data.bits(k), (input_data.slice(0, weights_length) zip kernels(k).slice(0, weights_length)).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _))
        }
    }
}
