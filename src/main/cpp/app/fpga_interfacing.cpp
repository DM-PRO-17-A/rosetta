#include <iostream>
#include <fstream>
#include <string>
#include <typeinfo>
using namespace std;
#include <sstream>
#include "TestOutputQueue.hpp"
#include "platform.h"
#include <iomanip>
#include <cmath>
#include <cstdlib>
#include <unistd.h>


int* Run_OutputQueue(WrapperRegDriver * platform) {
    TestOutputQueue q(platform);
    int vec[43];
    int input[43];

    while(q.get_empty() != 0){};
    q.set_output_pulse(1);
    q.set_output_pulse(0);
    vec[0] = q.get_output_data_0();
    vec[1] = q.get_output_data_1();
    vec[2] = q.get_output_data_2();  
    vec[3] = q.get_output_data_3();
    vec[4] = q.get_output_data_4();
    vec[5] = q.get_output_data_5();
    vec[6] = q.get_output_data_6(); 
    vec[7] = q.get_output_data_7();
    vec[8] = q.get_output_data_8();
    vec[9] = q.get_output_data_9();
    vec[10] = q.get_output_data_10();
    vec[11] = q.get_output_data_11();
    vec[12] = q.get_output_data_12();
    vec[13] = q.get_output_data_13();
    vec[14] = q.get_output_data_14();
    vec[15] = q.get_output_data_15();
    vec[16] = q.get_output_data_16();
    vec[17] = q.get_output_data_17();
    vec[18] = q.get_output_data_18();
    vec[19] = q.get_output_data_19();
    vec[20] = q.get_output_data_20();
    vec[21] = q.get_output_data_21();
    vec[22] = q.get_output_data_22();
    vec[23] = q.get_output_data_23();
    vec[24] = q.get_output_data_24();
    vec[25] = q.get_output_data_25();
    vec[26] = q.get_output_data_26();
    vec[27] = q.get_output_data_27();
    vec[28] = q.get_output_data_28();
    vec[29] = q.get_output_data_29();
    vec[30] = q.get_output_data_30();
    vec[31] = q.get_output_data_31();
    vec[32] = q.get_output_data_32();
    vec[33] = q.get_output_data_33();
    vec[34] = q.get_output_data_34();
    vec[35] = q.get_output_data_35();
    vec[36] = q.get_output_data_36();
    vec[37] = q.get_output_data_37();
    vec[38] = q.get_output_data_38();
    vec[39] = q.get_output_data_39();
    vec[40] = q.get_output_data_40();
    vec[41] = q.get_output_data_41();
    vec[42] = q.get_output_data_42(); 
    int* pointer  = vec;
    return pointer;
}

//Må legge til bilde fra Kamera som et argument i main.


float* Scaleshift(int* pointer){
  //filter 1
  float A[43] = {0.10613798, 0.0967258, 0.0944901, 0.08205982, 0.08600254, 0.07336438,
            0.09290288, 0.08346851, 0.08365503, 0.10139856, 0.09375499, 0.10817018,
          0.12580705, 0.11591881, 0.10589285, 0.1045065,  0.09333096, 0.10654855,
          0.09912547, 0.09181008, 0.10024401, 0.10732551, 0.09640726, 0.102323,
          0.11001986, 0.10809018, 0.09999774, 0.10208429, 0.0995523,  0.11225812,
          0.10275448, 0.09403487, 0.10364552, 0.10415483, 0.10874164, 0.12092833,
          0.10799013, 0.09335944, 0.1140102,  0.09289351, 0.11192361, 0.09586933,
          0.08955998};
  //filter 2
  float B[43] = {-0.96071649, 0.68546766, 1.44876862, 1.84089804, 1.4770503, 2.17828822,
           -0.48416716, 0.3688474, 1.08563411, 0.55420429, 0.43626797, 0.16900358,
           -0.1347385,  0.0145933, -0.54196042, -0.37247148, -0.58978826, -0.4172011,
            0.26940933, -0.02862083, 0.28240931, -0.60368943, -0.63643998,  0.05946794,
           -0.21361144, 0.09381656, -0.09491161, -0.64013398,  0.05029509, -0.51663965,
           -0.32239845, 0.43873206, -0.75330114, -0.14596492, -0.45224375, -0.29807806,
           -0.57703501, -0.58822173, 0.20007333, -0.55777472, -0.56918406, -0.59853441,
           -0.55538076};
  float inn[43]; // tall etter at scaleshift har kjort
    
  for (int i =0; i < 43; i = i+1){ 
    inn[i]= pointer[i]*A[i] + B[i];  
  };
  float* output = inn;
  return output;
}


float* SoftMax(float* inn){

  float out [43]; // De endelige 43 tallene etter softmax
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
    float* output = out;
  
    return output;
}

int get_qnn_output(WrapperRegDriver * platform)
{
  int* vec_queue_pointer = Run_OutputQueue(WrapperRegDriver * platform);
  float* vec_scale_pointer = Scaleshift(vec_queue_pointer);
  return SoftMax(vec_scale_pointer);
}

