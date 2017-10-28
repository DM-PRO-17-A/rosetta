package rosetta

import Chisel._

class ComparatorWrapper(dataWidth: Int, valuesPerIteration: Int, thresholds: Array[Int]) extends RosettaAccelerator {
  val numMemPorts = 0
  val io = new RosettaAcceleratorIF(numMemPorts) {
    // values to be compared
    val input = Vec.fill(valuesPerIteration){UInt(INPUT, dataWidth)}
    // values to be output
    val output = Vec.fill(valuesPerIteration){UInt(OUTPUT, dataWidth)}
  }

  val t = Vec(thresholds.map(s => UInt(s, dataWidth)))
  val counter = Reg(init=UInt(0))

  for(i <- 0 to valuesPerIteration) {
    io.output := UInt(0)
  }

  for(j <- 0 until valuesPerIteration){
      val in = Module(new Comparator(dataWidth)).io
      in.in0 := io.input(UInt(j))
      printf("%d\n",t(counter + UInt(j)))
      in.in1 := t(counter + UInt(j))
      io.output(UInt(j)) := in.output
    }

  when(counter === UInt(thresholds.length)) {
    counter := UInt(0)
  } .otherwise {
    counter := counter + UInt(valuesPerIteration)
  }


  
}

class ComparatorWrapperTest(c: ComparatorWrapper) extends Tester(c) {
  val test = Array[BigInt](1, 2)
  val test2 = Array[BigInt](3, 4)
  poke(c.io.input, test)
  peek(c.io.output)
  step(1)
  poke(c.io.input, test2)
  peek(c.io.output)
  //expect(c.io.output, Array(1, 0, 1, 0))
}


