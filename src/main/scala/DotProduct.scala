package rosetta

import Chisel._

class DotProduct(arr_size: Int, input_width: Int, output_width: Int) extends RosettaAccelerator {
    def XNOR(a: UInt, b: UInt):UInt = {
        ~(a ^ b)
    }
    def expandInt(a: UInt): UInt = {
        UInt((a*UInt(2))-UInt(1), 2)
    }

    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts){
        val vec_1 = Vec.fill(arr_size){UInt(INPUT, input_width)}
        val vec_2 = Vec.fill(arr_size){UInt(INPUT, input_width)}

        val data_out = UInt(OUTPUT, output_width)
    }


    val outputArray = Vec.fill(arr_size){UInt(width=output_width)}
    if(input_width == 1) {
        for(i <- 0 to arr_size-1){
            outputArray(i) := expandInt(XNOR(io.vec_1(i), io.vec_2(i)))      // Bitwise XNOR
        }
    } else {
        for(i <- 0 to arr_size-1){
            outputArray(i) := io.vec_1(i) * io.vec_2(i)
        }
    }
    
    val sum = Module(new Sum(arr_size, output_width)).io

    sum.nums := outputArray

    io.data_out := sum.data_out
}

class DotProductTests(c: DotProduct) extends Tester(c) {
    val vec_1 = Array[BigInt](0,1,1,0)
    val vec_2 = Array[BigInt](1,1,0,0)

    poke(c.io.vec_1, vec_1)
    poke(c.io.vec_2, vec_2)
    peek(c.outputArray)
    peek(c.io.data_out)
}
