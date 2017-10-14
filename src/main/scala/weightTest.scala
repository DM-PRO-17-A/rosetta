package rosetta

import Chisel._

class weightTest(arr_len: Int, weights: Array[Int], data_width: Int) extends RosettaAccelerator {
    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val number = UInt(INPUT, data_width)

        val data_out = Vec.fill(arr_len){UInt(OUTPUT, data_width)}
    }

    val w = Vec(weights.map(s => UInt(s, data_width)))

    for(i <- 0 to arr_len-1) {
        io.data_out(i) := w(i) * io.number
    }

}

class weightTestTests(c: weightTest) extends Tester(c) {
    val test_num = 2
    poke(c.io.number, test_num)
    peek(c.io.data_out)
    //expect(c.io.data_out, test_num*10)
}
