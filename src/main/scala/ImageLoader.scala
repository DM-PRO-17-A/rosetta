package rosetta

import Chisel._
import fpgatidbits.ocm._

class ImageLoader(numMemLines: Int, dataWidth: Int)  extends RosettaAccelerator {
    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val write_enable = Bool(INPUT)
        val write_addr = UInt(INPUT, width = log2Up(numMemLines))
        val write_data = UInt(INPUT, width = dataWidth)
        val read_addr = UInt(INPUT, width = log2Up(numMemLines))
        val read_data = UInt(OUTPUT, width = dataWidth)
    }

    val bram = Module(new DualPortBRAM(addrBits = log2Up(numMemLines), dataBits = dataWidth)).io
    
    val write_port = bram.ports(0)
    write_port.req.addr := io.write_addr
    write_port.req.writeData := io.write_data
    write_port.req.writeEn := io.write_enable

    val read_port = bram.ports(1)
    read_port.req.addr := io.read_addr
    read_port.req.writeEn := UInt(0)
    io.read_data := read_port.rsp.readData

    io.signature := makeDefaultSignature()
}

class ImageLoaderTests(c: ImageLoader) extends Tester(c) {
    poke(c.io.write_enable, 1)
    poke(c.io.write_addr, 16)
    poke(c.io.write_data, 1234)
    poke(c.io.read_addr, 16)
    step(1)
    peek(c.io.read_data)
    poke(c.io.read_addr, 1)
    step(1)
    peek(c.io.read_data)
    peek(c.io.write_addr)

}
