package rosetta

import Chisel._

// These param names are really bad
class FullyConnected(kernels: Int, weights: Array[Array[Int]], input_size: Int, weight_size: Int, output_size: Int,data_width: Int) extends RosettaAccelerator {
    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts){
        val input_data = Vec.fill(input_size){UInt(INPUT, data_width)}

        val output_data = Decoupled(Vec.fill(output_size){UInt(OUTPUT, data_width)})
    }

    val w = Vec(weights.map(s => Vec(s.map(t => UInt(t, data_width)))))

    val acc = Vec.fill(output_size){Reg(init=UInt(width=data_width))}

    val weight_counter = Reg(init=UInt(width=data_width))
    val input_counter = Reg(init=UInt(width=data_width))

    for(k <- 0 until kernels){
        val dotprod = Module(new DotProduct(input_size, data_width)).io
        dotprod.vec_1 := io.input_data

        val w_slice = Vec.fill(input_size){UInt(width=data_width)}
        for(i <- 0 until input_size) {
            w_slice(i) := w(k)(weight_counter+UInt(i))
        }
        dotprod.vec_2 := w_slice

        acc(weight_counter + UInt(k)) := acc(input_counter) + dotprod.data_out

        // This is pretty ugly
        when(input_counter === UInt(output_size)) {
            input_counter := UInt(0)
        } .otherwise {
            input_counter := input_counter + input_size
        }
        when(weight_counter === UInt(weight_size)) {
            weight_counter := UInt(0)
        } .otherwise {
            weight_counter := weight_counter + kernels
        }
    }

    for(i <- 0 until output_size) {
        io.output_data.bits(i) := acc(i)
    }
}

class FullyConnectedTests(c: FullyConnected) extends Tester(c) {
    val test_array = Array[BigInt](1,1,1,1)

    poke(c.io.input_data, test_array)
    step(1)
    peek(c.io.output_data)
    peek(c.weight_counter)
}
