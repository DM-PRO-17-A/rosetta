#!/bin/sh

g++ -std=c++11 *.cpp -lsds_lib -o app `pkg-config --libs --cflags opencv`
