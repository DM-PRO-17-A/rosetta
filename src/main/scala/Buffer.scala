package rosetta

import Chisel._

class Buffer(buffer_size: Int, data_width: Int) extends RosettaAccelerator{
    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts) {
        // Currently only accepts one input number, should be expanded to accept more than one per cycle
        val input_data = UInt(INPUT, data_width)
        
        val output_data = Decoupled(Vec.fill(buffer_size) { UInt(OUTPUT, data_width) })
    }

    val counter = Reg(init=UInt(0, data_width))     // Using data_width here doesn't really make sense, should be changed 
    val buffer = Vec.fill(buffer_size) { Reg(init=UInt(width=data_width)) }
    io.output_data.valid := Bool(false)

    when ( counter === UInt(buffer_size) ) {
        counter := UInt(0)
        io.output_data.valid := Bool(true)
    } .otherwise {
        counter := counter + UInt(1)
    }

    buffer(counter) := io.input_data

    io.output_data.bits := buffer
}

class BufferTests(c: Buffer) extends Tester(c) {
    for(i <- 1 to 8) {
        poke(c.io.input_data, i)
        peek(c.io.output_data)
        peek(c.counter)
        step(1)
    }

    peek(c.io.output_data)
    peek(c.counter)
    step(1)
    
    for(i <- 9 to 8*2+1){
        poke(c.io.input_data, i)
        peek(c.io.output_data)
        peek(c.counter)
        step(1)
    }
}
