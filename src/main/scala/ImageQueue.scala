package rosetta

import Chisel._
import fpgatidbits.ocm._


class ImageQueue(dataWidth: Int, queueDepth: Int, vec_fill_size: Int) extends RosettaAccelerator {
    val numMemPorts = 0
    val io = new RosettaAcceleratorIF(numMemPorts) {
        val input_data = UInt(INPUT, width=dataWidth)
        val input_pulse = Bool(INPUT)

        val output = Decoupled(UInt(OUTPUT, dataWidth))
        val full = Bool(OUTPUT)
        val empty = Bool(OUTPUT)

    }

    val pulse_reg = Reg(next=io.input_pulse)
    val queue = Module(new FPGAQueue(UInt(width=dataWidth), queueDepth))

    queue.io.enq.valid := !io.input_pulse && pulse_reg
    queue.io.enq.bits := io.input_data

    io.full := (queue.io.count === UInt(queueDepth -1))
    io.empty := (queue.io.count === UInt(0))

    io.output <> queue.io.deq
}

class ImageQueueTests(c: ImageQueue) extends Tester(c) {
  val input_data = Array(1,2,3,4,5,6,7,8).map(BigInt(_))
  // Poke input_data & pulse to 1 and step(1)
  poke(c.io.input_data, input_data(0))
  poke(c.io.input_pulse, 1)

  expect(c.io.empty, 1)
  expect(c.io.full, 0)

  step(1)

  // Set pulse to 0 and epxect various stuff! Step
  poke(c.io.input_pulse, 0)

  expect(c.queue.io.count, 0)
  expect(c.io.empty, 1)
  expect(c.io.full, 0)

  step(1)

  // Pulse went from 1-0 == add stuff to the queue!
  // Also, we're ready for output
  expect(c.queue.io.count, 1)
  expect(c.io.empty, 0)
  expect(c.io.full, 0)

  poke(c.io.output.ready, 1)
  expect(c.io.output.valid, 0)
  step(1)

  // This cycle the data will be added to the registers
  // (because of output.ready) and output will be ready next cycle
  poke(c.io.output.ready, 0)
  expect(c.io.output.valid, 0)
  step(1)

  // Output is ready and valid!
  expect(c.io.output.valid, 1)
  peek(c.queue.io.count)
}
