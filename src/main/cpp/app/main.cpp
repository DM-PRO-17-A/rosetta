#include <iostream>
#include <fstream>
#include <unistd.h>
#include <sstream>
#include <bitset>
using namespace std;

#include "AutoSimple.hpp"
#include "platform.h"

bool Run_AutoSimple(WrapperRegDriver* platform) {
    AutoSimple as(platform);
    ifstream input("./auto_simple_input.txt");
    int n_images = 4;
    int images[n_images][3072]; 
    int index = 0;
    int i;

    string line;
    int n = 0;
    while (getline(input, line)) {
      index = 0;
      istringstream is(line);
      while (is >> i) {
        images[n][index] = i;
        index++;
      }
      n++;
    }

    for (int n = 0; n < n_images; n++) {
      for (int i = 0; i < 3072; i += 32) {
        as.set_input_data_0(images[n][i]);
        as.set_input_data_1(images[n][i+1]);
        as.set_input_data_2(images[n][i+2]);
        as.set_input_data_3(images[n][i+3]);
        as.set_input_data_4(images[n][i+4]);
        as.set_input_data_5(images[n][i+5]);
        as.set_input_data_6(images[n][i+6]);
        as.set_input_data_7(images[n][i+7]);
        as.set_input_data_8(images[n][i+8]);
        as.set_input_data_9(images[n][i+9]);
        as.set_input_data_10(images[n][i+10]);
        as.set_input_data_11(images[n][i+11]);
        as.set_input_data_12(images[n][i+12]);
        as.set_input_data_13(images[n][i+13]);
        as.set_input_data_14(images[n][i+14]);
        as.set_input_data_15(images[n][i+15]);
        as.set_input_data_16(images[n][i+16]);
        as.set_input_data_17(images[n][i+17]);
        as.set_input_data_18(images[n][i+18]);
        as.set_input_data_19(images[n][i+19]);
        as.set_input_data_20(images[n][i+20]);
        as.set_input_data_21(images[n][i+21]);
        as.set_input_data_22(images[n][i+22]);
        as.set_input_data_23(images[n][i+23]);
        as.set_input_data_24(images[n][i+24]);
        as.set_input_data_25(images[n][i+25]);
        as.set_input_data_26(images[n][i+26]);
        as.set_input_data_27(images[n][i+27]);
        as.set_input_data_28(images[n][i+28]);
        as.set_input_data_29(images[n][i+29]);
        as.set_input_data_30(images[n][i+30]);
        as.set_input_data_31(images[n][i+31]);
        as.set_input_pulse(1);
        as.set_input_pulse(0);
      }
    }

    ifstream result_file("./auto_simple_results.txt");
    signed long int results[n_images][43]; 

    n = 0;
    while (getline(result_file, line)) {
      index = 0;
      istringstream is(line);
      while (is >> i) {
        results[n][index] = i;
        index++;
      }
      n++;
    }

    int a = 1;
    int b = 1;

    for (int n = 0; n < n_images; n++) {
      as.set_output_pulse(1);
      as.set_output_pulse(0);
      cout << bitset<10>(as.get_output_data_0()).to_string() << " == " << bitset<10>(results[n][0]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_1()).to_string() << " == " << bitset<10>(results[n][1]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_2()).to_string() << " == " << bitset<10>(results[n][2]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_3()).to_string() << " == " << bitset<10>(results[n][3]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_4()).to_string() << " == " << bitset<10>(results[n][4]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_5()).to_string() << " == " << bitset<10>(results[n][5]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_6()).to_string() << " == " << bitset<10>(results[n][6]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_7()).to_string() << " == " << bitset<10>(results[n][7]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_8()).to_string() << " == " << bitset<10>(results[n][8]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_9()).to_string() << " == " << bitset<10>(results[n][9]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_10()).to_string() << " == " << bitset<10>(results[n][10]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_11()).to_string() << " == " << bitset<10>(results[n][11]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_12()).to_string() << " == " << bitset<10>(results[n][12]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_13()).to_string() << " == " << bitset<10>(results[n][13]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_14()).to_string() << " == " << bitset<10>(results[n][14]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_15()).to_string() << " == " << bitset<10>(results[n][15]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_16()).to_string() << " == " << bitset<10>(results[n][16]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_17()).to_string() << " == " << bitset<10>(results[n][17]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_18()).to_string() << " == " << bitset<10>(results[n][18]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_19()).to_string() << " == " << bitset<10>(results[n][19]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_20()).to_string() << " == " << bitset<10>(results[n][20]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_21()).to_string() << " == " << bitset<10>(results[n][21]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_22()).to_string() << " == " << bitset<10>(results[n][22]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_23()).to_string() << " == " << bitset<10>(results[n][23]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_24()).to_string() << " == " << bitset<10>(results[n][24]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_25()).to_string() << " == " << bitset<10>(results[n][25]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_26()).to_string() << " == " << bitset<10>(results[n][26]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_27()).to_string() << " == " << bitset<10>(results[n][27]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_28()).to_string() << " == " << bitset<10>(results[n][28]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_29()).to_string() << " == " << bitset<10>(results[n][29]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_30()).to_string() << " == " << bitset<10>(results[n][30]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_31()).to_string() << " == " << bitset<10>(results[n][31]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_32()).to_string() << " == " << bitset<10>(results[n][32]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_33()).to_string() << " == " << bitset<10>(results[n][33]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_34()).to_string() << " == " << bitset<10>(results[n][34]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_35()).to_string() << " == " << bitset<10>(results[n][35]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_36()).to_string() << " == " << bitset<10>(results[n][36]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_37()).to_string() << " == " << bitset<10>(results[n][37]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_38()).to_string() << " == " << bitset<10>(results[n][38]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_39()).to_string() << " == " << bitset<10>(results[n][39]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_40()).to_string() << " == " << bitset<10>(results[n][40]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_41()).to_string() << " == " << bitset<10>(results[n][41]).to_string() << endl;
      cout << bitset<10>(as.get_output_data_42()).to_string() << " == " << bitset<10>(results[n][42]).to_string() << endl << endl;
    }

}

int main()
{
  WrapperRegDriver * platform = initPlatform();

  Run_AutoSimple(platform);

  deinitPlatform(platform);

  return 0;
}
