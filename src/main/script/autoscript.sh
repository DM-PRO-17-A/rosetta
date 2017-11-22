#!/bin/sh

cd /home/xilinx
sudo ./set_clock.sh 25

cd /home/xilinx/rosetta/rosetta
./load_bitfile.sh
echo "Starting Autoauto.."
sudo ./app
