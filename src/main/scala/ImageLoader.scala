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
    println("TEST \n")
    println(numMemLines)

    val log2test = log2Up(numMemLines)
    printf("log2test = %d", UInt(log2test))
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

class ImageQueue extends RosettaAccelerator {
    val numMemPorts = 0
    val vec_fill_size  = 4
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val queue_input = Flipped(Decoupled(Vec.fill(vec_fill_size){UInt(INPUT, width = 32)}))  //Valid and bits are inputs
        val queue_output = (Decoupled(Vec.fill(vec_fill_size){UInt(OUTPUT, width = 32)}))       //Valid and bits are outputs.count
        val queue_count = UInt(OUTPUT)
        val queue_write_enable = Bool(INPUT)

    }

    val testQueue = Module(new Queue(Vec.fill(vec_fill_size){UInt(width = 32)}, entries = 16))
    //Queues the bits from queue_input
    testQueue.io.enq <> io.queue_input
    //Connects output to the dequed information from testQueue.
    io.queue_output <> testQueue.io.deq
    //Counts the number of elements in the queue
    testQueue.io.count <> io.queue_count

    /*
    testList = Array[BigInt][1,2,3,4]

    def writeImage(image: List[Int]) Unit = {

    }
    */

    //testQueue.io.enq.valid <> io.valid_input
    //testQueue.io.deq.ready <> io.read_data
    /*
    for ( i <- test ) {
        poke(c.io.queue_input.bits, (UInt)i)
        poke(c.io.queue_input.valid, 1)
        step(1)
    }
    */



}


class ImageLoaderTests(c: ImageLoader) extends Tester(c) {
    //peek(c.log2test)
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

class ImageQueueTests(c: ImageQueue) extends  Tester(c) {

    poke(c.io.queue_input.bits, Array[BigInt](1,2,3,4))
    poke(c.io.queue_input.valid, 1)
    step(1)
    poke(c.io.queue_input.bits, Array[BigInt](5,6,7,8))
    poke(c.io.queue_input.valid, 1)
    step(1)
    peek(c.io.queue_count)
    poke(c.io.queue_input.valid, 0)
    step(1)
    poke(c.io.queue_output.ready, 1)
    peek(c.io.queue_output)
    step(1)
    peek(c.io.queue_output)
    step(1)
    peek(c.io.queue_count)
    peek(c.io.queue_output)



}
