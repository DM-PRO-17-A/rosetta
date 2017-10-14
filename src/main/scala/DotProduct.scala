package rosetta

import Chisel._

class DotProduct(input_size: Int, input_width: Int) extends RosettaAccelerator {
    def XNOR(a: UInt, b: UInt): UInt = {
        ~(a ^ b)
    }

    // This function should convert 0 / 1 -> -1 / 1
    def expandInt(i: UInt): SInt = {
      i.toSInt * SInt(2) - SInt(1)
    }

    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts){
        val vec_1 = Vec.fill(input_size){UInt(INPUT, input_width)}
        val vec_2 = Vec.fill(input_size){UInt(INPUT, input_width)}

        // Calculate output_width based on input_size and input_width
        val data_out = UInt(OUTPUT, math.ceil(math.log(input_size) / math.log(2)).toInt + input_width + 1)
    }

    io.data_out := (io.vec_1 zip io.vec_2).map{case (i1: UInt, i2: UInt) => i1*i2}.reduceLeft(_ + _)
}

class DotProductTests(c: DotProduct) extends Tester(c) {
    val vec_1 = Array[BigInt](2,1,1,1)
    val vec_2 = Array[BigInt](2,2,0,1)

    poke(c.io.vec_1, vec_1)
    poke(c.io.vec_2, vec_2)
    expect(c.io.data_out, (vec_1 zip vec_2).map{case (i1, i2) => i1*i2}.reduceLeft(_ + _))
    expect(c.io.data_out, 7)
}
