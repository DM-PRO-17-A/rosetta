package rosetta

import Chisel._

// These param names are really bad
class FullyConnected(kernels_path: String, kernels_per_it: Int, input_per_it: Int, input_width: Int) extends RosettaAccelerator {
    // Map the kernels / weights to UInt
    val kernels = Vec(
      scala.io.Source.fromInputStream(this.getClass.getResourceAsStream(kernels_path)).getLines.toArray.slice(0, 4)
        .map(kernel => Vec(
          kernel.split(" ").map(i => UInt(i.toInt, width=1)).toArray
        )
      )
    )

    val output_width = math.ceil(math.log(kernels(0).length * math.pow(2, input_width)) / math.log(2)).toInt + 1

    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val input_data = Vec.fill(input_per_it){UInt(INPUT, input_width)}

        val output_data = Decoupled(Vec.fill(kernels.length){SInt(OUTPUT, output_width)})
    }
    io.output_data.valid := Bool(false)


    val acc = Vec.fill(kernels.length){Reg(init=SInt(0, width=output_width))}

    for (i <- 0 until kernels.length) {
        io.output_data.bits(i) := acc(i)
    }


    val next_weight = Bool(false)
    val next_kernel = Bool(true)

    val current_weight = Reg(init=UInt(0, log2Up(kernels(0).length)))
    val current_kernel = Reg(init=UInt(0, log2Up(kernels.length)))

    for (k <- 0 until kernels_per_it) {
        val dot_prod = Module(new DotProduct(input_per_it, input_width)).io
        dot_prod.vec_1 := io.input_data

        for (i <- 0 until input_per_it) {
            dot_prod.vec_2(i) := kernels(current_kernel + UInt(k))(current_weight + UInt(i))
        }

        acc(current_kernel + UInt(k)) := acc(current_kernel + UInt(k)) + dot_prod.data_out
    }

    when (current_kernel === UInt(kernels.length - kernels_per_it)) {
        current_kernel := UInt(0)
        when (current_weight === UInt(kernels(0).length - input_per_it)) {
            // We are done
            current_weight := UInt(0)
        } .otherwise {
            current_weight := current_weight + UInt(input_per_it)
        }
    } .otherwise {
        current_kernel := current_kernel + UInt(kernels_per_it)
    }

}

class FullyConnectedTests(c: FullyConnected) extends Tester(c) {
    val input_data = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1input60.txt")).getLines.toArray.head.split(" ").map(s => BigInt(s.toInt))
    val kernels = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1.txt")).getLines.toArray.map(kernel => kernel.split(" ").map(s => s.toInt * 2 -1))
    println(kernels(0).length)

    val input_size = 10
    val kernels_per_iteration = 2

    for (image <- 0 until 1) {
      val acc = Array.fill(kernels_per_iteration){0}
      for (i <- 0 until kernels(0).length by input_size) {
          println(i)
          poke(c.io.input_data, input_data.slice(i, i + input_size))
          //println(i, i + input_size)

          //println(input_data.slice(i, i+input_size).mkString(", "))
          for (k <- 0 until 4 by kernels_per_iteration) {
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
