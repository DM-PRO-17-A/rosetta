package rosetta

import Chisel._
import fpgatidbits.regfile._

class RegFileGenerator extends RosettaAccelerator {
    val numMemPorts = 0
    val idBits = log2Up(16)
    val dataBits = 32
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val regFileIF = new RegFileSlaveIF(idBits, dataBits)

    }

    //RegFile(numRegs: Int, idBits: Int, dataBits: Int) - databits = width
    val regFile = Module(new RegFile(1, idBits, dataBits)).io

    io.regFileIF <> regFile.extIF

}


class RegFileTests(c: RegFileGenerator) extends Tester(c){
    val regFile = c.io.regFileIF

    poke(regFile.cmd.bits.regID, 1)
    poke(regFile.cmd.bits.read, 0)
    poke(regFile.cmd.bits.write, 1)
    poke(regFile.cmd.bits.writeData, 666)
    poke(regFile.cmd.valid, 1)
    step(1)
    poke(regFile.cmd.valid, 0)
    step(5) // allow the command to propagate and take effect
    peek(regFile.cmd.bits)
}