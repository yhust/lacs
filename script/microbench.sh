#!/bin/bash

# set flintrockPemPath as an environmental variable
# $1 file number $2 cache size per worker (MB) $3 rate1 $4 rate2

rate1=$3
rate2=$4  

read -r master < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${master} "cd ~/lacs;sed -i '$ d' conf/alluxio-site.properties; echo 'alluxio.lacs.mode=$1' >> conf/alluxio-site.properties;bin/alluxio-start.sh all;python python/generate_microbench_rates.py $1 $rate1 $rate2; bin/alluxio runLAWrite $2"

	# Run mm-default
	#MaxMinDefault
	# Run rep-default
	#Default
exit 0
