#ifndef FpgaInterfacing_H
#define FpgaInterfacing_H

#include <vector>
#include "AutoSimple.hpp"

void set_qnn_input(WrapperRegDriver* platform, vector<int> image);
vector<int> get_qnn_data(WrapperRegDriver* platform);
void set_output_pins(WrapperRegDriver* platform, int* pins);
vector<int> get_input_pins(WrapperRegDriver* platform);
vector<float> scaleshift(int pointer);
vector<float> softmax(float inn);
vector<float> get_qnn_output(WrapperRegDriver* platform);

#endif
