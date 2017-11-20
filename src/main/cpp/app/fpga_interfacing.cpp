#include <iostream>
#include <fstream>
#include <string>
#include <typeinfo>
#include <sstream>
#include <iomanip>
#include <cmath>
#include <cstdlib>
#include <unistd.h>
#include <vector>

#include "fpga_interfacing.hpp"
#include "AutoSimple.hpp"
#include "platform.h"

using namespace std;


void set_qnn_input(WrapperRegDriver* platform, vector<int> image){
  AutoSimple a(platform);
  for (int i = 0; i < 3072; i += 32) {
    a.set_input_data_0(image[i]);
    a.set_input_data_1(image[i+1]);
    a.set_input_data_2(image[i+2]);
    a.set_input_data_3(image[i+3]);
    a.set_input_data_4(image[i+4]);
    a.set_input_data_5(image[i+5]);
    a.set_input_data_6(image[i+6]);
    a.set_input_data_7(image[i+7]);
    a.set_input_data_8(image[i+8]);
    a.set_input_data_9(image[i+9]);
    a.set_input_data_10(image[i+10]);
    a.set_input_data_11(image[i+11]);
    a.set_input_data_12(image[i+12]);
    a.set_input_data_13(image[i+13]);
    a.set_input_data_14(image[i+14]);
    a.set_input_data_15(image[i+15]);
    a.set_input_data_16(image[i+16]);
    a.set_input_data_17(image[i+17]);
    a.set_input_data_18(image[i+18]);
    a.set_input_data_19(image[i+19]);
    a.set_input_data_20(image[i+20]);
    a.set_input_data_21(image[i+21]);
    a.set_input_data_22(image[i+22]);
    a.set_input_data_23(image[i+23]);
    a.set_input_data_24(image[i+24]);
    a.set_input_data_25(image[i+25]);
    a.set_input_data_26(image[i+26]);
    a.set_input_data_27(image[i+27]);
    a.set_input_data_28(image[i+28]);
    a.set_input_data_29(image[i+29]);
    a.set_input_data_30(image[i+30]);
    a.set_input_data_31(image[i+31]);
    a.set_input_pulse(1);
    a.set_input_pulse(0);
  }
}

vector<int> get_qnn_data(WrapperRegDriver * platform) {
  AutoSimple a(platform);
  vector<int> vec(43);

  while(a.get_empty() != 0){};

  a.set_output_pulse(1);
  a.set_output_pulse(0);

  vec[0] = a.get_output_data_0();
  vec[1] = a.get_output_data_1();
  vec[2] = a.get_output_data_2();  
  vec[3] = a.get_output_data_3();
  vec[4] = a.get_output_data_4();
  vec[5] = a.get_output_data_5();
  vec[6] = a.get_output_data_6(); 
  vec[7] = a.get_output_data_7();
  vec[8] = a.get_output_data_8();
  vec[9] = a.get_output_data_9();
  vec[10] = a.get_output_data_10();
  vec[11] = a.get_output_data_11();
  vec[12] = a.get_output_data_12();
  vec[13] = a.get_output_data_13();
  vec[14] = a.get_output_data_14();
  vec[15] = a.get_output_data_15();
  vec[16] = a.get_output_data_16();
  vec[17] = a.get_output_data_17();
  vec[18] = a.get_output_data_18();
  vec[19] = a.get_output_data_19();
  vec[20] = a.get_output_data_20();
  vec[21] = a.get_output_data_21();
  vec[22] = a.get_output_data_22();
  vec[23] = a.get_output_data_23();
  vec[24] = a.get_output_data_24();
  vec[25] = a.get_output_data_25();
  vec[26] = a.get_output_data_26();
  vec[27] = a.get_output_data_27();
  vec[28] = a.get_output_data_28();
  vec[29] = a.get_output_data_29();
  vec[30] = a.get_output_data_30();
  vec[31] = a.get_output_data_31();
  vec[32] = a.get_output_data_32();
  vec[33] = a.get_output_data_33();
  vec[34] = a.get_output_data_34();
  vec[35] = a.get_output_data_35();
  vec[36] = a.get_output_data_36();
  vec[37] = a.get_output_data_37();
  vec[38] = a.get_output_data_38();
  vec[39] = a.get_output_data_39();
  vec[40] = a.get_output_data_40();
  vec[41] = a.get_output_data_41();
  vec[42] = a.get_output_data_42(); 

  return vec;
}

void set_output_pins(WrapperRegDriver* platform, vector<int> pins) {
  AutoSimple a(platform);
  a.set_out_pins_0(pins[3]);
  a.set_out_pins_1(pins[2]);
  a.set_out_pins_2(pins[1]);
  a.set_out_pins_3(pins[0]);
}

void go(WrapperRegDriver* platform, int go_val) {
  AutoSimple a(platform);
  a.set_pcb_go(go_val);
}

vector<int> get_input_pins(WrapperRegDriver* platform) {
  AutoSimple a(platform);
  vector<int> pins(2);

  pins[0] = a.get_in_pins_0();
  pins[1] = a.get_in_pins_1();

  return pins;
}

vector<int> get_pcb_btns(WrapperRegDriver* platform) {
  AutoSimple a(platform);
  vector<int> btns(4);

  btns[0] = a.get_pcb_btn_0();
  btns[1] = a.get_pcb_btn_1();
  btns[2] = a.get_pcb_btn_2();

  return btns;
}

vector<float> scaleshift(vector<int> pointer) {

  float A[43] = {0.10613798, 0.0967258, 0.0944901, 0.08205982, 0.08600254, 0.07336438,
    0.09290288, 0.08346851, 0.08365503, 0.10139856, 0.09375499, 0.10817018,
    0.12580705, 0.11591881, 0.10589285, 0.1045065,  0.09333096, 0.10654855,
    0.09912547, 0.09181008, 0.10024401, 0.10732551, 0.09640726, 0.102323,
    0.11001986, 0.10809018, 0.09999774, 0.10208429, 0.0995523,  0.11225812,
    0.10275448, 0.09403487, 0.10364552, 0.10415483, 0.10874164, 0.12092833,
    0.10799013, 0.09335944, 0.1140102,  0.09289351, 0.11192361, 0.09586933,
    0.08955998};

  float B[43] = {-0.96071649, 0.68546766, 1.44876862, 1.84089804, 1.4770503, 2.17828822,
    -0.48416716, 0.3688474, 1.08563411, 0.55420429, 0.43626797, 0.16900358,
    -0.1347385,  0.0145933, -0.54196042, -0.37247148, -0.58978826, -0.4172011,
    0.26940933, -0.02862083, 0.28240931, -0.60368943, -0.63643998,  0.05946794,
    -0.21361144, 0.09381656, -0.09491161, -0.64013398,  0.05029509, -0.51663965,
    -0.32239845, 0.43873206, -0.75330114, -0.14596492, -0.45224375, -0.29807806,
    -0.57703501, -0.58822173, 0.20007333, -0.55777472, -0.56918406, -0.59853441,
    -0.55538076};

  vector<float> inn(43); 
    
  for (int i =0; i < 43; i = i+1){ 
    inn[i]= pointer[i]*A[i] + B[i];  
  };

  return inn;
}


vector<float> softmax(vector<float> inn) {
  vector<float> out(43); 
  float sum = 0;
  float max = inn[0];

  for (int i =0; i < 43; i = i+1){
    if(inn[i] > max){
      max = inn[i];
    };
  };

  std::cout << std::fixed;
  std::cout << std::setprecision(14);

  for (int i =0; i < 43; i = i+1){  
      out[i] = std::exp(inn[i] - max); 
      sum = sum + out[i];
  };

  for (int i =0; i < 43; i = i+1){
    //cout << max << " -- " << sum << " -- " << out[i];
    out[i] = out[i] / sum;
  };

  return out;
}

vector<float> get_qnn_output(WrapperRegDriver* platform) {
  vector<int> vec_queue_pointer = get_qnn_data(platform);
  vector<float> vec_scale_pointer = scaleshift(vec_queue_pointer);
  return softmax(vec_scale_pointer);
}

