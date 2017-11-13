package rosetta

import Chisel._

class ComparatorWrapper(dataWidth: Int, valuesPerIteration: Int) extends RosettaAccelerator {
  val numMemPorts = 0
  val io = new RosettaAcceleratorIF(numMemPorts) {
    // values to be compared
    val input = Flipped(Decoupled(Vec.fill(valuesPerIteration){SInt(INPUT, 21)}))
    // values to be output
    val output = Decoupled(Vec.fill(valuesPerIteration){UInt(OUTPUT, 1)})
  }

  io.output.valid := io.input.valid
  io.input.ready := io.output.ready

  val thresholds = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/thresholds.txt")).getLines.toArray
  val t = Vec(thresholds(0).split(", ").map(f => SInt(f.toInt, width = 12)))

  // sets all values in output to be 0, NECESSARY to avoid error
  for(i <- 0 to valuesPerIteration) {
    io.output.bits := UInt(0)
  }

  // creates a new comparator for each element in array 
  for(j <- 0 until valuesPerIteration){
      // io.output.bits(UInt(j)) := Mux(io.input.bits(UInt(j)) >= t(UInt(j)), UInt(1), UInt(0))
      io.output.bits(j) := Mux(io.input.bits(j) >= t(j), UInt(1), UInt(0))
    }

}


class ComparatorWrapperTest(c: ComparatorWrapper) extends Tester(c) {
  val test = Array[BigInt](157, 105, -225, -199, -117, 201, 102, 255, 59, -221, 232, -227, -118, -139, 190, 37, 34, 197, 56, -201, -105, -77, -73, 186, 80, -82, 189, 204, 63, 9, 239, -205, -78, -57, -152, -160, -141, -57, 54, -167, -159, -211, 1, 108, 82, 199, -82, 132, 97, 112, -72, -133, -160, 125, -146, 99, 71, 168, 194, -227, -151, 243, -229, 197, -83, -66, -112, 239, -30, 238, 127, -95, -231, 104, 214, -1, -67, -4, 123, 45, 252, -250, 96, 214, -127, -36, 44, 235, -97, -204, -164, -144, 97, -110, 54, -135, 50, -202, 100, -159, 37, 58, -55, -142, -163, 140, 158, 90, -241, -2, -131, -236, 124, -233, -181, 203, 36, -202, 116, 191, -209, 64, -215, -71, 96, 24, -247, 113, -120, -198, -3, 234, -14, 61, 148, 230, 95, 138, 226, 170, 59, -233, -219, -101, -191, -164, 84, 112, -82, -177, -70, 49, 152, 100, 111, -157, -115, 14, -196, -33, 176, 211, 50, 107, -130, 27, 245, -132, -83, 245, -219, -43, -238, -46, 88, -230, -196, 214, 76, 166, 79, 208, 227, 133, 74, -112, -164, -2, -229, -77, 145, -143, 8, -60, 234, -58, 111, 65, 226, -147, 114, 19, 88, 42, -76, 123, 124, 132, -180, -115, -108, -218, 155, -33, 220, 202, 209, 55, 164, 125, 244, -90, -152, -77, 143, -50, -145, -170, -251, -200, 111, -205, -196, 250, -27, -14, -46, -223, -107, -174, -245, -154, 52, 124, 127, 3, 204, 30, 30, -201, -62, 129, -249, 61, -237, -94)
  poke(c.io.input.valid, 1)
  poke(c.io.input.bits(0), test(0))
  peek(c.io.output)
}


