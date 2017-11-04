package rosetta

import Chisel._
import fpgatidbits.regfile._
import fpgatidbits.ocm._

class RegFileGenerator extends RosettaAccelerator {
    val dataWidth = 32
    val queueDepth = 32
    val vec_fill_size = 4

    val numMemPorts = 0
    val idBits = log2Up(16)
    val dataBits = 32
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val regFileIF = new RegFileSlaveIF(idBits, dataBits)

        val queue_input = Flipped(Decoupled(UInt(INPUT, width = dataWidth)))
        val queue_output = (Decoupled(Vec.fill(vec_fill_size){UInt(OUTPUT, width = dataWidth)}))       //Valid and bits are outputs.count
        val queue_count = UInt(OUTPUT)

    }

    val testQueue = Module(new FPGAQueue(Vec.fill(vec_fill_size){UInt(width = dataWidth)}, entries = queueDepth))

    //RegFile(numRegs: Int, idBits: Int, dataBits: Int) - databits = width
    val regFile = Module(new RegFile(1, idBits, dataBits)).io


    io.regFileIF <> regFile.extIF

    //testQueue.io.enq.bits <> io.regFileIF.cmd.bits.writeData

    io.queue_output <> testQueue.io.deq
    testQueue.io.count <> io.queue_count



}


class RegFileTests(c: RegFileGenerator) extends Tester(c){
    val regFile = c.io.regFileIF

    poke(regFile.cmd.bits.regID, 1)
    poke(regFile.cmd.bits.read, 0)
    poke(regFile.cmd.bits.write, 1)
    poke(regFile.cmd.bits.writeData, 4)
    poke(regFile.cmd.valid, 1)
    step(1)
    poke(regFile.cmd.valid, 0)
    step(5) // allow the command to propagate and take effect
    poke(regFile.cmd.bits.read, 1)
    poke(regFile.cmd.bits.write, 0)
    poke(regFile.cmd.bits.writeData, 0)
    poke(regFile.cmd.valid, 1)
    step(1)
    poke(regFile.cmd.valid, 0)
    step(1)
    peek(regFile.readData)
}