package rosetta

import Chisel._
import fpgatidbits.ocm._


class OutputQueue(dataWidth: Int, queueDepth: Int, vec_fill_size: Int) extends RosettaAccelerator {
    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val input = Flipped(Decoupled(Vec.fill(vec_fill_size){Bits(INPUT, width=dataWidth)}))

        val output_data = Vec.fill(vec_fill_size){Bits(OUTPUT, dataWidth)}
        val output_pulse = Bool(INPUT)

        val empty = Bool(OUTPUT)
        val count = UInt(OUTPUT, width=8)

    }

    val pulse_reg = Reg(init=Bool(false), next=io.output_pulse)
    val queue = Module(new FPGAQueue(Vec.fill(vec_fill_size){UInt(width=dataWidth)}, queueDepth))
    val output_ready = (!io.output_pulse && pulse_reg)
    val output_reg = Reg(init=Vec.fill(vec_fill_size){UInt(width=dataWidth)})

    queue.io.enq <> io.input
    io.empty := (queue.io.count === UInt(0))
    io.count := queue.io.count

    queue.io.deq.ready := output_ready

    when(output_ready && !io.empty){
        io.output_data := queue.io.deq.bits
        output_reg := queue.io.deq.bits
    } .otherwise {
        io.output_data := output_reg
    }
}
