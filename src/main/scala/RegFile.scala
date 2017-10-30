package rosetta

import Chisel._
import fpgatidbits.regfile._

class RegFile extends RosettaAccelerator {
    val numMemPorts = 0
    val idBits = log2Up(16)
    val dataBits = 32
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val regFileIF = new RegFileSlaveIF(idBits, dataBits)

    }

    val regFile = Module(new RegFile(numRegs, regAddrBits, wCSR)).io
    
    io.regFileIF <> regFile.extIF

}


class RegFileTests(c: RegFile) extends Tester(c){
    val regFile = c.io.regFileIF

    poke(regFile.cmd.bits.regID, 123)
    poke(regFile.cmd.bits.read, 0)
    poke(regFile.cmd.bits.write, 1)
    poke(regFile.cmd.bits.writeData, 666)
    poke(regFile.cmd.valid, 1)
    step(1)
    poke(regFile.cmd.valid, 0)
    step(5) // allow the command to propagate and take effect
}