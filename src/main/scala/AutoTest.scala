// package rosetta

// import Chisel._

// class AutoTest(dataWidth: Int, valuesPerIteration: Int) extends RosettaAccelerator {
//   val numMemPorts = 0
//   val io = new RosettaAcceleratorIF(numMemPorts) {
//     // values to be compared
//     val input = Flipped(Decoupled(Vec.fill(valuesPerIteration){SInt(INPUT, dataWidth)}))
//     // values to be output
//     val output = Decoupled(Vec.fill(valuesPerIteration){UInt(OUTPUT, dataWidth)})
//   }

//   // val numbers = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/numbers.txt")).getLines.toArray
//   // val n = Vec(numbers(0).split(", ").map(f => SInt(f.toInt, width = 12)))
//   // val n = Vec(numbers.map(s => UInt(s, dataWidth)))

//   // sets all values in output to be 0, NECESSARY to avoid error
//   for(i <- 0 until 64) {
//     io.output.bits := UInt(0)
//   }

// 	val BT = Module(new ComparatorWrapper(dataWidth, valuesPerIteration)).io
// 	BT.input.bits := io.input.bits

//   val BT2 = Module(new ComparatorWrapper(dataWidth, valuesPerIteration)).io
//   BT2.input.bits := BT.output.bits
//   io.output.bits := BT2.output.bits

// }

// class AutoTestTest(c: AutoTest) extends Tester(c) {
//   val test = Array[BigInt](102, 4, -179, -169)
//   poke(c.io.input.bits, test)
//   poke(c.io.input.valid, 1)
//   peek(c.io.output)
// }


