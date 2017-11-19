#include <iostream>
#include <string>
#include <algorithm>
#include <vector>
// Rosetta stuff in order to use the FPGA and pins
#include "AutoSimple.hpp"
#include "platform.h"
#include "fpga_interfacing.hpp"

using namespace std;


const int VEC_SIZE = 43;
static const string gtsrb_classes[VEC_SIZE] = {"20 Km/h", "30 Km/h", "50 Km/h", "60 Km/h", "70 Km/h", "80 Km/h",
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


vector<int> speed = {0, 0};

vector<int> get_signal( std::string sign )
{
	vector<int> signal(4);
	if ( "50 Km/h" == sign )
	{
		signal = {0, 1, 0, 0};
		speed = {0, 1};
	}
	else if ( "70 Km/h" == sign )
	{
		signal = {1, 0, 0, 0};
		speed = {1, 0};
	}
	else if ( "100 Km/h" == sign )
	{
		signal = {1, 1, 0, 0};
		speed = {1, 1};
	}
	else if ( "Turn right ahead" == sign )
	{
		signal[0] = speed[0];
		signal[0] = speed[1];
		signal[2] = 0;
		signal[3] = 1;
	}
	else if ( "Turn left ahead" == sign )
	{
		signal[0] = speed[0];
		signal[0] = speed[1];
		signal[2] = 1;
		signal[3] = 0;
	}
	else if ( "No entry for vehicular traffic" == sign )
	{
		signal[0] = speed[0];
		signal[0] = speed[1];
		signal[2] = 1;
		signal[3] = 1;
	}
	else if ( "Stop" == sign )
	{
		signal = {0, 0, 0, 0};
		speed = {0, 0};
	}
	else
	{
		signal[0] = speed[0];
		signal[0] = speed[1];
		signal[2] = 0;
		signal[3] = 0;
	}
	return signal;
}


int main()
{
	// Platform to use pins from
	WrapperRegDriver * platform = initPlatform();


	// Wait until start button on PCB is pressed
	while ( 1 == get_pcb_btns()[0] );
	go( 1 );
	

	// Initialise array for output data
	vector<float> average( VEC_SIZE );
	vector<float> weights( VEC_SIZE );
	int i;
	for ( i = 0; i < VEC_SIZE; ++i )
	{
		average[i] = 0.0;
		if ( 2 == i || 4 == i || 7 == i || 14 == i || 17 == i || 33 == i || 34 == i )
			weights[i] = 1.0;
		else
			weights[i] = 0.5
	}

	while(1)
	{
		/* Read input from daughter card 
		 * If busy, don't process and send new data
		 */
		vector<int> input;
		input = get_input_pins( platform );
		if ( 1 == input[1] )
			continue;


		// POSSIBLY call image taking method and send to QNN

		
		// Get QNN output and find most likely sign
		vector<float> output;
		output = get_qnn_output( platform );
		
		int i;
		for ( i = 0; i < VEC_SIZE; ++i )
		{
			// TODO: Insert logic for calculating most likely sign
			average[i] += ( output[i] * weights[i] );
		}
		// Standardise result
		average = softmax( average );
		
		
		// Get most likely sign and send instructions to daughter card
		int max_index = *max_element( average.begin(), average.end() );
		std::string sign = gtsrb_classes[max_index];
		vector<int> signal;
		signal = get_signal( sign );
		// If the robot is at a crossroads, tell it what to do
		if ( 1 == input[0] )
			set_output_pins( platform, signal );
		else
		{
			signal = { speed[0], speed[0], 0, 0 };
			set_output_pins( platform, signal );
		}
			
		
		// Stop the program in its entirety
		if ( "Stop" == sign )
		{
			cout << "This is the end." << endl;
			go( 0 );
			break;
		}
	}
	

	deinitPlatform(platform);
	
	
	return 0;
}
