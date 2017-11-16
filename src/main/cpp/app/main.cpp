#include <iostream>
using namespace std;

#include "GPIOPins.hpp"
#include "platform.h"

bool Run_GPIOPins(WrapperRegDriver * platform) {
  GPIOPins t(platform);

  /*
  t.set_out_pins_0(1);
  t.set_out_pins_1(1);
  t.set_out_pins_2(1);
  t.set_out_pins_3(1);
  t.set_out_pins_4(1);
  t.set_out_pins_5(1);
  t.set_out_pins_6(1);
  t.set_out_pins_7(1);
  */

  t.set_out_pins_0(1);
  t.set_out_pins_1(1);
  t.set_out_pins_2(1);
  t.set_out_pins_3(1);
  while(true){
    cout << "IN0: " << t.get_in_pins_0() << " IN1: " << t.get_in_pins_1() << " BTN: " << t.get_pcb_btn() << endl;
  }
}

int main()
{
  WrapperRegDriver * platform = initPlatform();

  Run_GPIOPins(platform);

  deinitPlatform(platform);

  return 0;
}
