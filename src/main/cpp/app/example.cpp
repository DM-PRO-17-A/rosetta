#include <iostream>
#include <fstream>
#include <unistd.h>
#include <sstream>
#include <bitset>
#include <string>
using namespace std;

//#include "TestRegOps.hpp"
#include "AutoSimple.hpp"
#include "platform.h"
#include "fpga_interfacing.hpp"

int main()
{
  WrapperRegDriver * platform = initPlatform();

  ifstream input("./auto_simple_input.txt");
  int n_images = 4;
  vector<int> images(3072); 
  int index = 0;
  int i;

  string line;
  int n = 0;
  while (getline(input, line) && n == 0) {
    index = 0;
    istringstream is(line);
    while (is >> i) {
      images[index] = i;
      //cout << "images[" << index << "]: " << i << endl;
      index++;
    }
    n++;
  }

  set_qnn_input(platform, images);

  vector<float> result;
  result = get_qnn_output(platform);

  string gtsrb_classes[43] = {"20 Km/h", "30 Km/h", "50 Km/h", "60 Km/h", "70 Km/h", "80 Km/h",
                 "End 80 Km/h", "100 Km/h", "120 Km/h", "No overtaking",
                 "No overtaking for large trucks", "Priority crossroad", "Priority road",
                 "Give way", "Stop", "No vehicles",
                 "Prohibited for vehicles with a permitted gross weight over 3.5t including their trailers, and for tractors except passenger cars and buses",
                 "No entry for vehicular traffic", "Danger Ahead", "Bend to left",
                 "Bend to right", "Double bend (first to left)", "Uneven road",
                 "Road slippery when wet or dirty", "Road narrows (right)", "Road works",
                 "Traffic signals", "Pedestrians in road ahead", "Children crossing ahead",
                 "Bicycles prohibited", "Risk of snow or ice", "Wild animals",
                 "End of all speed and overtaking restrictions", "Turn right ahead",
                 "Turn left ahead", "Ahead only", "Ahead or right only",
                 "Ahead or left only", "Pass by on right", "Pass by on left", "Roundabout",
                 "End of no-overtaking zone",
                 "End of no-overtaking zone for vehicles with a permitted gross weight over 3.5t including their trailers, and for tractors except passenger cars and buses"};

  float max = 0;
  int max_i;
  for(i = 0; i < 43; i++){
    if(result[i] > max){
      max = result[i];
      max_i = i;
    }
    //cout << gtsrb_classes[i] << ": " <<  result[i] << endl;
  }

  cout << gtsrb_classes[max_i] << endl;


  //Run_TestRegOps(platform);
  //Run_AutoSimple(platform);

  deinitPlatform(platform);

  return 0;
}
