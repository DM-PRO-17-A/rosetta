package rosetta

import Chisel._

class ConvolutionLayer(input_width: Int, windows: Int, filters: Int) extends RosettaAccelerator {
    val window_size = 5*5*3

    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts){
        val input = Vec.fill(window_size * windows){Bits(INPUT, input_width)}

        // Bits length is same as output from a single DP, ie. a filter applied to a window
        val output = Vec.fill(windows * filters){Bits(OUTPUT, math.ceil(math.log(window_size) / math.log(2)).toInt + input_width + 1)}
    }

    // Load weights from file
    // weights.length == 20

    val weights = Vec(
      // This looks bad as shit, but scala 2.11.x doesnt have nice support for this
      scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/weights/convolution1.txt")).getLines

      // How many bits for a brotha? Gotta do toFloat.toInt because of the number format. Could/should be changed w
      .map(line => line.split(" ").map(f => SInt(f.toFloat.toInt, 2)))

      .map(filter => Vec(filter)).toArray
    )

    // Apply filters to all windows given as input
    // Wait until the previous component states that the data is ready
    for (f <- 0 until filters) {
      for (w <- 0 until windows) {
          // Apply F filters to all windows given
          // and hook the result up to output
          printf(w.toString)
          printf(f.toString)
        }

        // 1. Increment some counter to keep track of how many filters we're done with
          // We're done with counter * F filters and we proceed with the next F filters next iteration
        // 2. Wait until the next component is ready.
          // This is because we have to wait for the next component of
          // the pipeline to be done with the current output before we change it
    }

    // Set ready after done with all windows given and all 20 filters
    // to indicate that we're ready for the next W windows
    // Proceed to wait for data valid from the previous component

}

class ConvolutionLayerTests(c: ConvolutionLayer) extends Tester(c) {
}
