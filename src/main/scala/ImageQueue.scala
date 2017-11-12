package rosetta

import Chisel._
import fpgatidbits.ocm._


class ImageQueue(dataWidth: Int, queueDepth: Int, vec_fill_size: Int) extends RosettaAccelerator {
    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val input_data = Vec.fill(vec_fill_size){UInt(INPUT, width=dataWidth)}
        val input_pulse = Bool(INPUT)

        val output_data = Decoupled(Vec.fill(vec_fill_size){UInt(OUTPUT, dataWidth)})
        val full = Bool(OUTPUT)
        val empty = Bool(OUTPUT)

    }

    val pulse_reg = Reg(next=io.input_pulse)
    val queue = Module(new FPGAQueue(Vec.fill(vec_fill_size){UInt(width=dataWidth)}, queueDepth))

    queue.io.enq.valid := !io.input_pulse && pulse_reg
    queue.io.enq.bits := io.input_data
    
    io.full := (queue.io.count === UInt(queueDepth -1))
    io.empty := (queue.io.count === UInt(0))

    io.output_data <> queue.io.deq


    /*
    when(queue.io.enq.valid && queue.io.enq.ready){
        printf("ADD: (%d, %d, %d, %d, %d, %d, %d, %d), len: %d\n", queue.io.enq.bits(0), queue.io.enq.bits(1), queue.io.enq.bits(2), queue.io.enq.bits(3), queue.io.enq.bits(4),
            queue.io.enq.bits(5), queue.io.enq.bits(6), queue.io.enq.bits(7), queue.io.count)
    }
    // Printsetning som printer alle bits i en dequeue-operasjon når valid og ready er true.
    when(queue.io.deq.valid && queue.io.deq.ready){
        printf("POP: (%d, %d, %d, %d, %d, %d, %d, %d), len: %d\n", queue.io.deq.bits(0), queue.io.deq.bits(1), queue.io.deq.bits(2), queue.io.deq.bits(3), queue.io.deq.bits(4),
            queue.io.deq.bits(5), queue.io.deq.bits(6), queue.io.deq.bits(7), queue.io.count)
    }
    */

}
