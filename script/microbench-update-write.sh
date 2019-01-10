#!/bin/bash

# set flintrockPemPath as an environmental variable
# $1 cache size per worker in MB $2 file number  $3 rate1 $4 rate2

rate1=$3
rate2=$4

read -r master < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${master} "cd ~/lacs;python python/generate_microbench_rates.py $2 $rate1 $rate2;bin/alluxio runLAWrite $1"

# copy pop file to all nodes
scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${master}:~/lacs/pop.txt ~/Desktop/
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py copy-file lacs ~/Desktop/pop.txt ~/lacs/

	# Run mm-default
	#MaxMinDefault
	# Run rep-default
	#Default
exit 0
