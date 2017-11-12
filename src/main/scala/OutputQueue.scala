package rosetta

import Chisel._
import fpgatidbits.ocm._


class OutputQueue(dataWidth: Int, queueDepth: Int, vec_fill_size: Int) extends RosettaAccelerator {
    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val input_data = Flipped(Decoupled(Vec.fill(vec_fill_size){UInt(INPUT, width=dataWidth)}))

        val output_data = Vec.fill(vec_fill_size){UInt(OUTPUT, dataWidth)}
        val output_pulse = Bool(INPUT)

        val empty = Bool(OUTPUT)

    }

    val pulse_reg = Reg(next=io.output_pulse)
    val queue = Module(new FPGAQueue(Vec.fill(vec_fill_size){UInt(width=dataWidth)}, queueDepth))
    val output_ready = (!io.output_pulse && pulse_reg)
    val output_reg = Reg(init=Vec.fill(vec_fill_size){UInt(width=dataWidth)})

    queue.io.enq <> io.input_data
    io.empty := (queue.io.count === UInt(0))

    queue.io.deq.ready := output_ready

    when(output_ready && !io.empty){
        io.output_data := queue.io.deq.bits
        output_reg := queue.io.deq.bits
    } .otherwise {
        io.output_data := output_reg
    }



    /*
    when(queue.io.enq.valid && queue.io.enq.ready){
        printf("ADD: (%d, %d, %d, %d, %d, %d, %d, %d), len: %d\n", queue.io.enq.bits(0), queue.io.enq.bits(1), queue.io.enq.bits(2), queue.io.enq.bits(3), queue.io.enq.bits(4),
            queue.io.enq.bits(5), queue.io.enq.bits(6), queue.io.enq.bits(7), queue.io.count)
    }
     Printsetning som printer alle bits i en dequeue-operasjon når valid og ready er true.
    when(queue.io.deq.valid && queue.io.deq.ready){
        printf("POP: (%d, %d, %d, %d, %d, %d, %d, %d), len: %d\n", queue.io.deq.bits(0), queue.io.deq.bits(1), queue.io.deq.bits(2), queue.io.deq.bits(3), queue.io.deq.bits(4),
            queue.io.deq.bits(5), queue.io.deq.bits(6), queue.io.deq.bits(7), queue.io.count)
    }
    */
}
