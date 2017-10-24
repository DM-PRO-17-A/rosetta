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

    val weights = Vec(
      scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/weights/convolution1.txt")).getLines // This looks bad as shit, but scala 2.11.x doesnt have nice support for this
      .map(line => line.split(" ").map(f => SInt(f.toFloat.toInt, 2))) // How many bits for a brotha? Gotta do toFloat.toInt because of the number format. Could/should be changed
      .map(filter => Vec(filter)).toArray
    )

    for (w <- 0 until windows) {
        for (f <- 0 until filters) {
          printf(w)
          printf(f)
        }
    }

}

class ConvolutionLayerTests(c: ConvolutionLayer) extends Tester(c) {
}
