package rosetta

import Chisel._

// These param names are really bad
class FullyConnected(kernels: Int, weights: Array[Array[Int]], input_size: Int, weight_size: Int, output_size: Int, input_width: Int) extends RosettaAccelerator {
    val output_width = math.ceil(math.log(input_size * 2 ^ input_width * weight_size) / math.log(2)).toInt + 1
    //val output_width = 21
    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts){
        val input_data = Vec.fill(input_size){UInt(INPUT, input_width)}

        val output_data = Decoupled(Vec.fill(output_size){SInt(OUTPUT, output_width)})
    }
    io.output_data.valid := Bool(false)

    val w = Vec(weights.map(s => Vec(s.map(t => UInt(t, 1)))))

    val acc = Vec.fill(output_size){Reg(init=SInt(0,width=output_width))}

    val weight_counter = Reg(init=UInt(width=log2Up(weight_size)))
    val kernel_counter = Reg(init=UInt(width=output_width))

    for(k <- 0 until kernels){
        val dotprod = Module(new DotProduct(input_size, input_width)).io
        dotprod.vec_1 := io.input_data

        val w_slice = Vec.fill(input_size){UInt(width=1)}
        for(i <- 0 until input_size) {
            w_slice(i) := w(kernel_counter + UInt(k))(weight_counter+UInt(i))
        }
        printf("weights: %b\n", w_slice(0))
        dotprod.vec_2 := w_slice

        // If through all weights, reset accumulators
        when(weight_counter === UInt(0)){
            acc(kernel_counter + UInt(k)) := dotprod.data_out
        } .otherwise {
            acc(kernel_counter + UInt(k)) := acc(kernel_counter + UInt(k)) + dotprod.data_out
        }
    }

    // If done with all kernels for current input
    when(kernel_counter === UInt(output_size - kernels)) {
        kernel_counter := UInt(0)

        // Update weight counter
        when(weight_counter === UInt(weight_size - input_size)) {
            // If we are done
            weight_counter := UInt(0)
            io.output_data.valid := Bool(true)
        } .otherwise {
            // Increment weight counter by input_size
            weight_counter := weight_counter + UInt(input_size)
        }
    } .otherwise {
        // Increment kernel_counter by kernels
        kernel_counter := kernel_counter + UInt(kernels)
    }

    for(i <- 0 until output_size) {
        io.output_data.bits(i) := acc(i)
    }
}

class FullyConnectedTests(c: FullyConnected) extends Tester(c) {
    val test_data = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1input60.txt")).getLines.toArray
    val img = test_data(0).split(" ").map(s => BigInt(s.toInt))

    val lines = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/fc1.txt")).getLines.toArray
    val weights = lines(0).split(" ").map(s => s.toInt * 2 -1)
    val weights_1 = lines(1).split(" ").map(s => s.toInt * 2 -1)

    for(i <- 0 until 10){
        println(img(i), weights(i))
        poke(c.io.input_data, Array(img(i)))
        step(1)
        expect(c.io.output_data.bits(0), (img.slice(0, i+1) zip weights.slice(0, i+1)).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _))
        expect(c.io.output_data.bits(1), (img.slice(0, i+1) zip weights_1.slice(0, i+1)).map{case (i1: BigInt, i2: Int) => i1*i2}.reduceLeft(_ + _))
    }
    peek(c.io.output_data)
}
