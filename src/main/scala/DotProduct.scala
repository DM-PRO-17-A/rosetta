package rosetta

import Chisel._

class DotProduct(input_size: Int, input_width: Int) extends RosettaAccelerator {
    def XNOR(a: Bits, b: Bits): Bits = {
        ~(a ^ b)
    }

    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts){
        val vec_1 = Vec.fill(input_size){Bits(INPUT, input_width)}
        val vec_2 = Vec.fill(input_size){Bits(INPUT, 1)}

        // Calculate output_width based on input_size and input_width
        val data_out = Bits(OUTPUT, math.ceil(math.log(input_size) / math.log(2)).toInt + input_width + 1)
    }
    
    if(input_width == 1) {
        io.data_out := (io.vec_1 zip io.vec_2).map{case (i1: Bits, i2: Bits) => XNOR(i1,i2)}.reduceLeft(_ + _)
    } else {
        io.data_out := (io.vec_1 zip io.vec_2).map{case (i1: UInt, i2: Bits) => Mux(i2 === Bits(0), -i1.zext, i1.zext)}.reduceLeft(_ + _)
    }
}

class DotProductTests(c: DotProduct) extends Tester(c) {
    val vec_1 = Array[BigInt](2,1,1,1)
    val vec_1_1 = Array[BigInt](1,0,1,0)
    val vec_2 = Array[BigInt](1,1,0,1)

    poke(c.io.vec_1, vec_1)
    poke(c.io.vec_2, vec_2)
    expect(c.io.data_out, 3)
}
