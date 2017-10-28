package rosetta

import Chisel._

class ConvolutionLayer(input_width: Int, windows: Int, filters: Int) extends RosettaAccelerator {
    val window_size = 5*5*3

    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts){
        val input_windows = Vec.fill(windows){Vec.fill(window_size){Bits(INPUT, input_width)}}

        // Bits length is same as output from a single DP, ie. a filter applied to a window
        val output = Vec.fill(windows * filters){Bits(OUTPUT, math.ceil(math.log(window_size * 2 ^ input_width) / math.log(2)).toInt + 1)}

    }

    // Load weights from file
    // weights.length == 20
    val weights = Vec(
      // This looks bad as shit, but scala 2.11.x doesnt have nice support for this
      scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/weights/convolution1.txt")).getLines

      // How many bits for a brotha? Gotta do toFloat.toInt because of the number format. Could/should be changed w
      .map(line => line.split(" ").map(i => if (i.toInt == 1) Bits(1) else Bits(0)))

      .map(filter => Vec(filter.toArray)).toArray
    )


    // Apply filters to all windows given as input
    // Wait until the previous component states that the data is ready
    val dot_products = Array.fill(windows * filters){Module(new DotProduct(window_size, input_width))}
    0 until windows foreach { w => {
      0 until filters foreach { f => {
          println(w, f)
          dot_products(w * windows + f).io.vec_1 := io.input_windows(w)
          io.output(w * windows + f) := dot_products(w * windows + f).io.data_out
        }
      }
    }
    }


    // Steps is equal to 20 divided by filters per iteration
    val steps = 20 / filters

    // The switch takes cares of hooking up the right filters
    // for each iteration or "step"
    // Step is currently hard coded to be "1" and has to be replaced with a counter of some sort
    val step = Bits(0)
    switch (step) {
      for (step <- 0 until steps) {
        is (Bits(step)) {
          0 until windows foreach { w => {
            0 until filters foreach { f => {
                // w * windows + f - to index the dot_products
                // step * filters - to offset the current step and add f to get the right filter
                // Example:
                // step = 1, ie. the second iteration
                // filters = 5, ie. 5 filters per iteration
                //
                // This will then hook up filter 5 -> 9 to window W
                dot_products(w * windows + f).io.vec_2 := weights(step * filters + f)
              }
            }
          }
        }
      }
    }
    }


    // 1. Increment some counter to keep track of how many filters we're done with
      // We're done with counter * F filters and we proceed with the next F filters next iteration
    // 2. Wait until the next component is ready.
      // This is because we have to wait for the next component of
      // the pipeline to be done with the current output before we change it

    // Set ready after done with all windows given and all 20 filters
    // to indicate that we're ready for the next W windows
    // Proceed to wait for data valid from the previous component

}

class ConvolutionLayerTests(c: ConvolutionLayer) extends Tester(c) {
    val windows = 1
    val filters = 10
    val lines = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/convolution1.txt")).getLines.toArray
    val input = lines(0).split(" ").map(f => BigInt(f.toInt))
    val outputs = lines.slice(1, filters + 1).map(f => BigInt(f.toInt))

    poke(c.io.input_windows(0), input)
    for (f <- 0 until filters) {
      expect(c.io.output(f), outputs(f))
    }
}
