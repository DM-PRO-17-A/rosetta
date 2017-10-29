#include <iostream>
using namespace std;

#include "ImageLoader.hpp"
#include "platform.h"

/*bool Run_TestRegOps(WrapperRegDriver * platform) {
  TestRegOps t(platform);

  cout << "Signature: " << hex << t.get_signature() << dec << endl;
  cout << "Enter two operands to sum: ";
  unsigned int a, b;
  cin >> a >> b;

  t.set_op_0(a);
  t.set_op_1(b);

  cout << "Result: " << t.get_sum() << " expected: " << a+b << endl;

  return (a+b) == t.get_sum();
}
*/
bool Run_ImageLoader(WrapperRegDriver * platform) {
    ImageLoader img(platform);

    img.set_write_enable(1);
    img.set_write_addr(0);
    img.set_write_data(1234);
    img.set_write_enable(0);
    
    img.set_read_addr(0);
    cout << img.get_read_data();
}

bool Run_ImageQueue(WrapperRegDriver * platform) {
    ImageQueue queue(platform);

    val test = Array(0, 0, 0, 255, 0, 0, 0, 255, 0, 0, 0, 255)

    for ( i <- test ) {
        poke(c.io.queue_input.bits, (UInt)i)
        poke(c.io.queue_input.valid, 1)
        step(1)
    }



}

int main()
{
  WrapperRegDriver * platform = initPlatform();

  //Run_TestRegOps(platform);

  Run_ImageLoader(platform);

  deinitPlatform(platform);

  return 0;
}
