package rosetta

import Chisel._

class GPIOPins() extends RosettaAccelerator {
  val nin_pins = 2
  val nout_pins = 4
  val numMemPorts = 0
  val io = new RosettaAcceleratorIF(numMemPorts) {
    val out_pins = Vec.fill(4){UInt(INPUT, width=1)}
    val in_pins = Vec.fill(2){UInt(OUTPUT, width=1)}
    val pcb_btn = UInt(OUTPUT, width=1)
  }

  io.ck_out := UInt(7, width=nout_pins)
  for(i <- 0 until nout_pins){
    io.ck_out(i) := io.out_pins(i)
  }

  for(i <- 0 until nin_pins){
    io.in_pins(i) := io.ck_in(i)
  }

  io.pcb_btn := io.pbtn
}

