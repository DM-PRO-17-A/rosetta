package rosetta

import Chisel._

class Pooling(kernels: Int, input_size: Int, filter_size: Int, data_width: Int) extends RosettaAccelerator {
    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts) {
        // Currently takes a flat array as input
        val input_data = Vec.fill(input_size*filter_size){UInt(INPUT, data_width)}

        val output_data = Vec.fill(input_size){UInt(OUTPUT, data_width)}
    }

    val counter = UInt(0)
    when(counter === UInt((input_size*filter_size)/kernels)) {
        counter := UInt(0)
    } .otherwise {
        counter := counter + UInt(1)
    }

    // Kernels should be a for-loop here, initialising the correct amount of max-modules

    val result_arr = Vec.fill(input_size){UInt(width=data_width)}

    for(i <- 0 until input_size){
        val max = Module(new Max(filter_size, data_width)).io
        val max_inputs = Vec.fill(filter_size){UInt(width=data_width)}

        for(j <- 0 until filter_size) {
            max_inputs(j) := io.input_data(i*filter_size+j)
        }

        max.nums := max_inputs
        result_arr(i) := max.dataOut
    }

    io.output_data := result_arr

}

class PoolingTests(c: Pooling) extends Tester(c) {
    val input = Array[BigInt](0,0,1,0,0,0,0,0)

    poke(c.io.input_data, input)
    peek(c.io.output_data)
}
