package rosetta

import Chisel._

// These param names are really bad
class FullyConnected(kernels: Int, weights: Array[Array[Int]], input_size: Int, weight_size: Int, output_size: Int, input_width: Int) extends RosettaAccelerator {
    val output_width = math.ceil(math.log(input_size * 2 ^ input_width) / math.log(2)).toInt + 1
    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts){
        val input_data = Vec.fill(input_size){UInt(INPUT, input_width)}

        val output_data = Decoupled(Vec.fill(output_size){SInt(OUTPUT, output_width)})
    }
    io.output_data.valid := Bool(false)

    val w = Vec(weights.map(s => Vec(s.map(t => UInt(t, 1)))))

    val acc = Vec.fill(output_size){Reg(init=SInt(width=output_width))}

    val weight_counter = Reg(init=UInt(width=output_width))
    val output_counter = Reg(init=UInt(width=output_width))

    for(k <- 0 until kernels){
        val dotprod = Module(new DotProduct(input_size, input_width)).io
        dotprod.vec_1 := io.input_data

        val w_slice = Vec.fill(input_size){UInt(width=1)}
        for(i <- 0 until input_size) {
            w_slice(i) := w(k)(weight_counter+UInt(i))
        }
        dotprod.vec_2 := w_slice

        printf("k: %d, dp: %d\n", UInt(k), dotprod.data_out)
        acc(output_counter + UInt(k)) := acc(output_counter + UInt(k)) + dotprod.data_out

        output_counter := output_counter + UInt(kernels)
        when(output_counter === UInt(output_size) - UInt(kernels)) {
            output_counter := UInt(0)

            weight_counter := weight_counter + UInt(input_size)
            when(weight_counter === UInt(weight_size) - UInt(input_size)) {
                weight_counter := UInt(0)
                io.output_data.valid := Bool(true)
            }
        }

    }

    for(i <- 0 until output_size) {
        io.output_data.bits(i) := acc(i)
    }
}

class FullyConnectedTests(c: FullyConnected) extends Tester(c) {
    val test_array = Array[BigInt](20, 10, 2, 45)
    val step_size = 2

    // This test is really ugly
    poke(c.io.input_data, Array[BigInt](20,10))
    peek(c.acc)
    step(1)
    poke(c.io.input_data, Array[BigInt](2, 45))
    peek(c.acc)
    step(1)
    peek(c.io.output_data)
    expect(c.io.output_data.bits(0), -33)
    expect(c.io.output_data.bits(1), 77)
    expect(c.io.output_data.bits(2), -77)
}
