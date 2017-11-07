#include <iostream>
using namespace std;

<<<<<<< HEAD
#include "ImageLoader.hpp"
#include "platform.h"

/*bool Run_TestRegOps(WrapperRegDriver * platform) {
=======
//#include "TestRegOps.hpp"
#include "FullyConnected.hpp"
#include "platform.h"

/*
bool Run_TestRegOps(WrapperRegDriver * platform) {
>>>>>>> FCLayer
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
<<<<<<< HEAD
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



=======

bool Run_FullyConnected(WrapperRegDriver * platform) {
    FullyConnected fc(platform);

    //cout << "Signature: " << hex << fc.get_signature() << dec << endl;
    cout << "Enter four numbers: ";
    int a, b, c, d;
    cin >> a >> b >> c >> d;

    int result0, result1, result2;
    result0 = a - b + c - d;
    result1 = a + b + c + d;
    result2 = - a - b - c - d;

    fc.set_input_data_0(a);
    fc.set_input_data_1(b);
    fc.set_input_data_2(c);
    fc.set_input_data_3(d);

    cout << "Result 1: " << int(fc.get_output_data_bits_0()) << ". Expected: " << result0 << endl;
    cout << "Result 2: " << int(fc.get_output_data_bits_1()) << ". Expected: " << result1 << endl;
    cout << "Result 3: " << int(fc.get_output_data_bits_2()) << ". Expected: " << result2 << endl;
>>>>>>> FCLayer
}

int main()
{
  WrapperRegDriver * platform = initPlatform();

  //Run_TestRegOps(platform);
<<<<<<< HEAD

  Run_ImageLoader(platform);
=======
  Run_FullyConnected(platform);
>>>>>>> FCLayer

  deinitPlatform(platform);

  return 0;
}
