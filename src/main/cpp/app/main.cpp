#include <iostream>
using namespace std;

#include "TestRegOps.hpp"
#include "platform.h"

bool Run_TestRegOps(WrapperRegDriver * platform) {
  TestRegOps t(platform);

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

  t.set_out_pins(10);
}

int main()
{
  WrapperRegDriver * platform = initPlatform();

  Run_TestRegOps(platform);

  deinitPlatform(platform);

  return 0;
}
