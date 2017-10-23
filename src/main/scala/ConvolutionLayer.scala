package rosetta

import Chisel._

class ConvolutionLayer(input_width: Int, windows: Int, filters: Int) extends RosettaAccelerator {
    val window_size = 5*5*3

    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts){
        val input = Vec.fill(window_size * windows){Bits(INPUT, input_width)}

        // Bits length is same as output from a single DP, ie. a filter applied to a window
        val output = Vec.fill(windows_size * windows * filters){Bits(OUTPUT, math.ceil(math.log(window_size) / math.log(2)).toInt + input_width + 1)}
    }


    // This attempts to apply M filters to N windows in one go
    // and the previous module in the pipeline is responsible
    // for feeding this module 1 or more windows at a time.
    // Windows will probably never be greater than four (4).
    //
    // This module is responsible for applying all 20 filters
    // to each window and the amount of filters applied per iteration
    // is given as an argument (filters)
    //
    // This is not entirely reflected in the code yet.

    for (w <- 0 until windows) {
        for (f <- 0 until filters) {

            // Need to find a better var name than cw
            val cw = new ConvolutionWindow(input_width)

            // Correct input.
            cw.io.input := io.input.slice(window_size * w, window_size * (w + 1))
            // Filters are currently hardcoded in the ConvolutionalWindow module
            // The correct filter should probably be chosen in this module and passed in (?)

            // Hook up output to the correct output indexes. Not sure if this is correct yet, its 23:59 in China
            // This doesnt compile, yet, but I guess that the intention is obvious
            io.output.slice(window_size * (w + f * w), window_size * (w + f * w + 1)) := cw.io.output
        }
    }

}

class ConvolutionLayerTests(c: DotProduct) extends Tester(c) {
}
