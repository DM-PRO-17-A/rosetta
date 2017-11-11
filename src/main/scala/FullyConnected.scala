package rosetta

import Chisel._

// These param names are really bad
class FullyConnected(kernels_path: String, kernels_per_it: Int, input_per_it: Int, input_width: Int) extends RosettaAccelerator {
    val kernels_length = 2
    val weights_length = 16
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

    val im_wait :: frame_wait :: calc :: Nil = Enum(UInt(), 3)
    val state = Reg(init = im_wait)

    switch(state) {
        is(im_wait) {
            io.input_data.ready := Bool(true)
            when(io.input_data.valid) {
                // Received first input
                state := calc
            }
        } 
        is(frame_wait) {
            io.input_data.ready := Bool(true)
            when(io.input_data.valid) {
                // Received next input
                state := calc
            }
        }
        is(calc) {
            when(current_weight === UInt(weights_length - input_per_it)){
                when(current_kernel === UInt(kernels_length - kernels_per_it)){
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
    }

    when (current_kernel === UInt(kernels_length - kernels_per_it)) {
        when (current_weight === UInt(weights_length - input_per_it)) {
            // We are done
            io.output_data.valid := Bool(true)
        } .otherwise {
            current_kernel := UInt(0)
            current_weight := current_weight + UInt(input_per_it)
        }
    } .otherwise {
        current_kernel := current_kernel + UInt(kernels_per_it)
        io.output_data.valid := Bool(false)
    }

}

class FullyConnectedTests(c: FullyConnected) extends Tester(c) {
    val input_data = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1input60.txt")).getLines.toArray.head.split(" ").map(s => BigInt(s.toInt))
    val kernels = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1.txt")).getLines.toArray.map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))

    val input_size = 16
    val kernels_per_iteration = 1

    for (image <- 0 until 1) {
        val acc = Array.fill(kernels_per_iteration){0}
        for (i <- 0 until 16 by input_size) {
            println(i)
            poke(c.io.input_data.valid, 1)
            poke(c.io.input_data.bits, input_data.slice(i, i + input_size))
            //println(i, i + input_size)

            //println(input_data.slice(i, i+input_size).mkString(", "))
            for (k <- 0 until 2 by kernels_per_iteration) {
                step(1)
                //println(kernels(k).slice(i, i+input_size).mkString(", "))
                //println((input_data.slice(i, i+input_size) zip kernels(k).slice(i, i+input_size)).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_+_))
                for (k_i <- 0 until kernels_per_iteration) {
                    expect(c.io.output_data.bits(k + k_i), (input_data.slice(0, i+input_size) zip kernels(k + k_i).slice(0, i+input_size)).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _))
                }
            }
        }
    }
    peek(c.io.output_data)
}
