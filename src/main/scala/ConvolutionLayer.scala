package rosetta

import Chisel._


object Counter {
  def wrapAround(n: UInt, max: UInt) = 
    Mux(n > max, UInt(0), n)

  def counter(max: UInt, en: Bool, amt: UInt): UInt = {
    val x = Reg(init=UInt(0, max.getWidth))
    when (en) {
      x := wrapAround(x + amt, max)
    }
    x
  }
}

class ConvolutionLayer(input_width: Int, windows: Int, filters: Int) extends RosettaAccelerator {
    val window_size = 5*5*3

    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts){
        val input_windows = Vec.fill(windows){Vec.fill(window_size){Bits(INPUT, input_width)}}

        // Bits length is same as output from a single DP, ie. a filter applied to a window
        val output = Vec.fill(windows * filters){Bits(OUTPUT, math.ceil(math.log(window_size * 2 ^ input_width) / math.log(2)).toInt + 1)}
    }

    // Load weights from file
    val weights = Vec(
      scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/weights/convolution1.txt")).getLines
      .map(line => line.split(" ").map(i => if (i.toInt == 1) Bits(1) else Bits(0)))
      .map(filter => Vec(filter.toArray)).toArray
    )


    // Apply filters to all windows given as input
    // Wait until the previous component states that the data is ready
    val dot_products = Array.fill(windows * filters){Module(new DotProduct(window_size, input_width))}
    0 until windows foreach { w => {
      0 until filters foreach { f => {
          dot_products(w * filters + f).io.vec_1 := io.input_windows(w)
          dot_products(w * filters + f).io.vec_2 := io.input_windows(w) // to prevent NO DEFAULT SPECIFIED FOR WIRE errors
          io.output(w * filters + f) := dot_products(w * filters + f).io.data_out
          }
        }
      }
    }


    // Steps is equal to 20 divided by filters per iteration
    val steps = 20 / filters
    val step_counter = Counter.counter(UInt(steps), Bool(true), UInt(1))

    // The switch takes cares of hooking up the right filters
    // for each iteration or "step"
    switch (step_counter) {
      for (step <- 0 until steps) {
        is (UInt(step)) {
          0 until windows foreach { w => {
            0 until filters foreach { f => {
                dot_products(w * filters + f).io.vec_2 := weights(step * filters + f)
                }
              }
            }
          }
        }
      }
    }
}

class ConvolutionLayerTests(c: ConvolutionLayer) extends Tester(c) {
    val windows = 4
    val filters = 2
    val lines = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/convolution1.txt")).getLines.toArray

    // Inputs
    0 until windows foreach {
      w => {
        poke(c.io.input_windows(w), lines(w * 21).split(" ").map(f => BigInt(f.toInt)))
      }
    }

    for (s <- 0 until 20 / filters) {
      for (w <- 0 until windows) {
        val window_offset = w * 21
        val step_offset = filters * s
        val outputs = lines.slice(window_offset + step_offset + 1, window_offset + step_offset + 1 + filters).map(f => BigInt(f.toInt))
        for (f <- 0 until filters) {
          // w * filters, the first filters (m) outputs belongs to the 1st window
          expect(c.io.output(w * filters + f), outputs(f))
        }
      }
      step(1)
    }
}
