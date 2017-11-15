package rosetta

import Chisel._

class DotProduct(input_size: Int, input_width: Int) extends RosettaAccelerator {
    def XNOR(a: Bits, b: Bits): Bits = {
        ~(a ^ b)
    }

    def expandInt(a: UInt): SInt = {
        SInt(2, width=3) * a.zext - UInt(1)
    }

    val output_width = math.ceil(math.log(input_size * math.pow(2, input_width)) / math.log(2)).toInt + 1

    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts){
        val vec_1 = Vec.fill(input_size){Bits(INPUT, input_width)}
        val vec_2 = Bits(INPUT, input_size)

        // Calculate output_width based on input_size and input_width
        val output_data = SInt(OUTPUT, output_width)
    }

    if(input_width == 1) {
      io.output_data := (io.vec_1.zipWithIndex.map{ case(i1, index) => (i1, io.vec_2(input_size - 1 - index))}).map{case (i1: Bits, i2: Bits) => Mux(i1 === i2, SInt(1, width=2), SInt(-1, width=2))}.fold(SInt(0, width=output_width))(_ + _)
    } else {
      io.output_data := (io.vec_1.zipWithIndex.map{ case(i1, index) => (i1, io.vec_2(input_size - 1 - index))}).map{case (i1: UInt, i2: Bits) => Mux(i2 === Bits(0), -i1.zext, i1.zext)}.fold(SInt(0, width=output_width))(_ + _)
    }
}

class DotProductTests(c: DotProduct) extends Tester(c) {
    val lines = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream("/test_data/dot_product_small.txt")).getLines.toArray

    0 until lines.length / 3 foreach {
        it => {
            val current = it * 3

            val vec_1 = lines(current).split(" ").map(f => BigInt(f.toInt))
            val vec_2 = BigInt(Integer.parseInt(lines(current + 1).split(" ").map(f => if (f.toInt == 1) 1 else 0).mkString, 2))
            val output = BigInt(lines(current + 2).toInt)

            poke(c.io.vec_1, vec_1)
            poke(c.io.vec_2, vec_2)
            expect(c.io.output_data, output)
        }
    }
}
