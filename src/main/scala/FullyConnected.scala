package rosetta

import Chisel._

// These param names are really bad
class FullyConnected(kernels_path: String, kernels_per_it: Int, input_per_it: Int, input_width: Int) extends RosettaAccelerator {
    // Map the kernels / weights to UInt
    val kernels = Vec(
      scala.io.Source.fromInputStream(this.getClass.getResourceAsStream(kernels_path)).getLines.toArray.slice(0, 1)
        .map(kernel => Vec(
          kernel.split(" ").map(i => UInt(i.toInt, width=1)).toArray
        )
      )
    )

    // Output width is the amount of weights in each kernels times the maximum value of each input
    val output_width = math.ceil(math.log(kernels(0).length * math.pow(2, input_width)) / math.log(2)).toInt + 1

    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val input_data = Vec.fill(input_per_it){UInt(INPUT, input_width)}

        val output_data = Decoupled(Vec.fill(kernels.length){SInt(OUTPUT, output_width)})
    }
    io.output_data.valid := Bool(false)


    // Set up a bunch of accumulators, one for each kernel
    val acc = Vec.fill(kernels.length){Reg(init=SInt(0, width=output_width))}

    // Hard to name these variables, as we may be doing several weights at a time or
    // several kernels at a time but ¯\_(:D)_/¯
    val current_weight = Reg(init=UInt(width=log2Up(kernels(0).length)))
    val current_kernel = Reg(init=UInt(width=log2Up(kernels.length)))

    // Set up how many dotproducts to do at once
    for (k <- 0 until kernels_per_it) {
        val dot_prod = Module(new DotProduct(input_per_it, input_width)).io

        // All dot products takes in the same input
        dot_prod.vec_1 := io.input_data

        // Hook up the correct weights for each iteration.
        // Need to check if this is efficient in Vivado.
        val w_slice = Vec.fill(input_per_it){UInt(width=1)}
        for (i <- 0 until input_per_it) {
            w_slice(i) := kernels(current_kernel + UInt(k))(current_weight + UInt(i))
        }
        dot_prod.vec_2 := w_slice

        // If we're done with all weights in each kernel, reset accumulators
        // Ie. we're done with the current picture
        when (current_kernel === UInt(0)) {
            acc(current_kernel + UInt(k)) := dot_prod.data_out
        } .otherwise {
            acc(current_kernel + UInt(k)) := acc(current_kernel + UInt(k)) + dot_prod.data_out
        }
    }

    // If done with all kernels for current input
    when (current_kernel === UInt(kernels.length - kernels_per_it)) {
        current_kernel := UInt(0)

        // Update weight counter
        when (current_weight === UInt(kernels(0).length - input_per_it)) {
            // If we are done
            current_weight := UInt(0)
            io.output_data.valid := Bool(true)
        } .otherwise {
            // Increment weight counter by input_per_it
            current_weight := current_weight + UInt(input_per_it)
        }
    } .otherwise {
        // Increment kernel_counter by kernels
        current_kernel := current_kernel + UInt(kernels_per_it)
    }

    for (i <- 0 until kernels.length) {
        io.output_data.bits(i) := acc(i)
    }
}

class FullyConnectedTests(c: FullyConnected) extends Tester(c) {
    val input_data = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1input60.txt")).getLines.toArray.head.split(" ").map(s => BigInt(s.toInt))
    val kernels = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1.txt")).getLines.toArray.map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))

    val input_size = 1
    val kernels_per_iteration = 1

    for (i <- 0 until 10) {
        poke(c.io.input_data, input_data.slice(i, i + input_size))
        step(1)
        for (k <- 0 until kernels_per_iteration) {
          println(input_data.slice(i, i + input_size).mkString(" "), kernels(k).slice(0, i+1).mkString(" "))
          expect(c.io.output_data.bits(k), (input_data.slice(0, i+1) zip kernels(k).slice(0, i+1)).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _))
        }
    }
    peek(c.io.output_data)
}
