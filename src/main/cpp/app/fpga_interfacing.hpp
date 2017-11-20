#ifndef FpgaInterfacing_H
#define FpgaInterfacing_H

#include <vector>
#include "AutoSimple.hpp"

// Interfacing
void set_qnn_input(WrapperRegDriver* platform, vector<int> image);
void set_output_pins(WrapperRegDriver* platform, vector<int> pins);
void go(WrapperRegDriver* platform, int go_val);

vector<int> get_qnn_data(WrapperRegDriver* platform);
vector<int> get_input_pins(WrapperRegDriver* platform);
vector<int> get_pcb_btns(WrapperRegDriver* platform);
vector<float> get_qnn_output(WrapperRegDriver* platform);

// QNN layers
vector<float> scaleshift(vector<int> pointer);
vector<float> softmax(vector<float> inn);

#endif
