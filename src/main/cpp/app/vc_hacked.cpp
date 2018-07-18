// What's a makefile
// g++ vc.cpp `pkg-config --libs --cflags opencv` -o vc.out
// oh, and remember to use sudo when running it you dummy

// In compile_sw.sh
// g++ -std=c++11 *.cpp -lsds_lib `pkg-config --libs --cflags opencv` -o app

#include <iostream>
#include <vector>
#include <stdio.h>
#include "opencv2/opencv.hpp"
#include <string>
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"
#include "math.h"
#include "platform.h"
#include "fpga_interfacing.hpp"

using namespace std;
using namespace cv;

int Distance(Vec3b color1, Vec3b color2);
Mat Region(Mat image, Vec3b red, Vec3b blue, int radii[2]);
Mat CropRegion(Mat image, Mat T, int limit);
Mat Preprocessing(Mat image, Vec3b red, Vec3b blue, int radii[2], int limit);





const int VEC_SIZE = 43;
const string gtsrb_classes[VEC_SIZE] = {"20 Km/h", "30 Km/h", "50 Km/h", "60 Km/h", "70 Km/h", "80 Km/h",
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







Vec3b red = Vec3b(123,34,30);
Vec3b blue = Vec3b(21,42,95);
//int radii[] = {25,25};
int radii[] = {30,30};
int limit = 10000;

Mat Region(Mat image, Vec3b red, Vec3b blue, int radii[2])
{
  //image.cols = image.size().height = 240 = y
  //image.rows = image.size().width = 432 = x
  Mat T(image.rows, image.cols, CV_8UC1);
  for(int y=0;y<image.cols;y++) { //y
    for(int x=0;x<image.rows;x++) { //x
      Vec3b rgb=image.at<Vec3b>(x,y);
      int reddist = Distance(rgb, red);
      int bluedist = Distance(rgb, blue);
      if(reddist < radii[0] || bluedist < radii[1]) {
        T.at<unsigned char>(x,y) = 255;
      } else {
        T.at<unsigned char>(x,y) = 0;
      }
    }
  } return T;
}

Mat CropRegion(Mat image, Mat T, int limit)
{
  dilate(T, T, getStructuringElement(MORPH_ELLIPSE, Size(3, 3)));
  dilate(T, T, getStructuringElement(MORPH_ELLIPSE, Size(3, 3)));
  dilate(T, T, getStructuringElement(MORPH_ELLIPSE, Size(3, 3)));
  Mat mat = T.clone();
  floodFill(mat, Point(0,0), Scalar(255));
  bitwise_not(mat, mat);
  T = (T | mat);
  Moments oMoments = moments(T);
  double dM01 = oMoments.m01;
  double dM10 = oMoments.m10;
  double dArea = oMoments.m00;
  if (dArea > limit) {
    int posX = dM10 / dArea;
    int posY = dM01 / dArea;
    int radius = sqrt(dArea/3)/9;
    int x = posX-radius;
    int y = posY-radius;
    int s = 2*radius;
    
    if (x+s >= image.cols-1 || x < 0) {
      Mat crop(2, 2, CV_8UC3, Scalar(0,0,0));
      return crop;
    }
    else if (y+s >= image.rows-1 || y < 0) {
      Mat crop(2, 2, CV_8UC3, Scalar(0,0,0));
      return crop;
    }
    else {
      Mat crop = image(Rect(x, y, s, s));
      return crop;
    }
  } 
  else {
    Mat crop(2, 2, CV_8UC3, Scalar(0,0,0));
    return crop;
  }
}

int Distance(Vec3b color1, Vec3b color2)
{
  int r1 = color1.val[0];
  int g1 = color1.val[1];
  int b1 = color1.val[2];
  int b2 = color2.val[0];
  int g2 = color2.val[1];
  int r2 = color2.val[2];
  int res = sqrt(pow(r1-r2,2)+pow(g1-g2,2)+pow(b1-b2,2));
  return res;
}

Mat Preprocessing(Mat image, Vec3b red, Vec3b blue, int radii[2], int limit)
{
  Mat reg = Region(image, red, blue, radii);
  Mat crop = CropRegion(image, reg, limit);
  //resize(crop, res, cvSize(49, 49), 0, 0, CV_INTER_AREA );
  return crop;
}

int main() 
{
  // Create a VideoCapture object and open the webcam
  // Index signifies which webcam, use 0 on pynq
  VideoCapture cap(0);


  // FPGA queue stuff.
  WrapperRegDriver * platform = initPlatform();


  // Check if camera opened successfully
  if (!cap.isOpened()) {
    cout << "Error opening video stream or file" << endl;
    return -1;
  }

  int w = 432;
  int h = 240;
  int fps = 10;

  //int ch = 180;
  //int cw = 216;

  cap.set(CV_CAP_PROP_FRAME_WIDTH, w);
  cap.set(CV_CAP_PROP_FRAME_HEIGHT, h);
  cap.set(CV_CAP_PROP_FPS, fps);
  //cout << cap.get(CV_CAP_PROP_FRAME_WIDTH) << endl;
  //int count_frame = 1;
  //int winSize = 32;

  int count = 0;
  while (1) {
    Mat frame;
    count++;
    const string name = "images/image_"+to_string(count)+".jpg";

    // Capture frame-by-frame
    cap >> frame;

    // If the frame is empty, break immediately
    if (frame.empty())
     // break;
      continue;

    // Crop image to size
    //frame = frame(Rect(cw, 0, cw, ch));

    Mat cropped;
    cropped = Preprocessing(frame, red, blue, radii, limit);
    if (cropped.empty())
      continue;
    if (cropped.rows < 5) {
      cropped.release();
      Mat crop(32, 32, CV_8UC3, Scalar(0,0,0));
      // imshow("Cropped", crop);
    }
    else {
      Mat crop;
      resize(cropped, crop, cvSize(32, 32), 0, 0, CV_INTER_AREA );

      // Convert to RGB
      // cvtColor(frame, frame, cv::COLOR_BGR2RGB);

      //imwrite(name, cropped);
      //imshow("Cropped", crop);
      vector<int> V;
      if (crop.isContinuous()) {
        V.assign(crop.datastart, crop.dataend);
      } 
      else {
        for (int i = 0; i < crop.rows; ++i) {
          V.insert(V.end(), crop.ptr<uchar>(i), crop.ptr<uchar>(i)+crop.cols);
        }
      }

      set_qnn_input(platform, V);

      vector<float> V_output = get_qnn_output(platform);

      float max_v = 0;
      int max_index = 0;
      
      for (int i = 0; i < 43; i++) {
        if (V_output[i] > max_v) {
          max_v = V_output[i];
          max_index = i;
        }
      }

      std::string sign = gtsrb_classes[max_index];
      cout << sign << " " << max_v << endl;
    }

    // Display the resulting frame
    // imshow("Frame", frame);

    // Press  ESC on keyboard to exit
    char c = (char)waitKey(1);
    if (c == 27)
      break;

    cout << "Frame " << endl;
  }

  deinitPlatform(platform);

  // When everything done, release the video capture object
  cap.release();

  // Closes all the frames
  destroyAllWindows();

  return 0;
}
