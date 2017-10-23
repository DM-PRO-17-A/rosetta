package rosetta

import Chisel._

class ConvolutionWindow(input_width: Int) extends RosettaAccelerator {
    val input_size = 5*5*3

    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts){
        // A single window
        val input = Vec.fill(input_size){Bits(INPUT, input_width)}

        // The result of applying a filter is a single number of a specific length. Length is same as output from DP
        val output = Bits(OUTPUT, math.ceil(math.log(input_size) / math.log(2)).toInt + input_width + 1)
    }

    val dot_product = new DotProduct(input_size, input_width)
    dot_product.io.vec_1 := io.input

    // Temporary hard coded filter
    dot_product.io.vec_2 := Array[BigInt](1,1,0,1).map(e => Bits(e)) // correct weights, ie. the correct filter. Not implemented and should be given as input probably?

    io.output := dot_product.io.data_out
}

class ConvolutionWindowTests(c: DotProduct) extends Tester(c) {
}
