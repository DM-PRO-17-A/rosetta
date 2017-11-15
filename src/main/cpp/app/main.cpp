#include <iostream>
#include <fstream>
#include <unistd.h>
using namespace std;

#include "AutoSimple.hpp"
#include "platform.h"

bool Run_AutoSimple(WrapperRegDriver* platform) {
    AutoSimple as(platform);
    ifstream input("./fc1.txt");
    int image[3072];
    int index = 0;
    int i;
    while (input >> i) {
      image[index] = i; // YOLO string -> int
      index++;
    }
    int input_size = 32;

    for (int i = 0; i < 3072; i += 32) {
      as.set_input_data_0(image[i]);
      as.set_input_data_1(image[i+1]);
      as.set_input_data_2(image[i+2]);
      as.set_input_data_3(image[i+3]);
      as.set_input_data_4(image[i+4]);
      as.set_input_data_5(image[i+5]);
      as.set_input_data_6(image[i+6]);
      as.set_input_data_7(image[i+7]);
      as.set_input_data_8(image[i+8]);
      as.set_input_data_9(image[i+9]);
      as.set_input_data_10(image[i+10]);
      as.set_input_data_11(image[i+11]);
      as.set_input_data_12(image[i+12]);
      as.set_input_data_13(image[i+13]);
      as.set_input_data_14(image[i+14]);
      as.set_input_data_15(image[i+15]);
      as.set_input_data_16(image[i+16]);
      as.set_input_data_17(image[i+17]);
      as.set_input_data_18(image[i+18]);
      as.set_input_data_19(image[i+19]);
      as.set_input_data_20(image[i+20]);
      as.set_input_data_21(image[i+21]);
      as.set_input_data_22(image[i+22]);
      as.set_input_data_23(image[i+23]);
      as.set_input_data_24(image[i+24]);
      as.set_input_data_25(image[i+25]);
      as.set_input_data_26(image[i+26]);
      as.set_input_data_27(image[i+27]);
      as.set_input_data_28(image[i+28]);
      as.set_input_data_29(image[i+29]);
      as.set_input_data_30(image[i+30]);
      as.set_input_data_31(image[i+31]);
      as.set_input_pulse(1);
      as.set_input_pulse(0);
    }

    for (int n = 0; n < 3072 / 128; n++) {
      int offset = 16 * n;
      as.set_output_pulse(1);
      as.set_output_pulse(0);
      cout << (int) as.get_output_data_0() << endl;
      cout << (int) as.get_output_data_1() << endl;
      cout << (int) as.get_output_data_2() << endl;
      cout << (int) as.get_output_data_3() << endl;
      cout << (int) as.get_output_data_4() << endl;
      cout << (int) as.get_output_data_5() << endl;
      cout << (int) as.get_output_data_6() << endl;
      cout << (int) as.get_output_data_7() << endl;
      cout << (int) as.get_output_data_8() << endl;
      cout << (int) as.get_output_data_9() << endl;
      cout << (int) as.get_output_data_10() << endl;
      cout << (int) as.get_output_data_11() << endl;
      cout << (int) as.get_output_data_12() << endl;
      cout << (int) as.get_output_data_13() << endl;
      cout << (int) as.get_output_data_14() << endl;
      cout << (int) as.get_output_data_15() << endl << endl;t
    }

}

int main()
{
  WrapperRegDriver * platform = initPlatform();

  Run_AutoSimple(platform);

  deinitPlatform(platform);

  return 0;
}
