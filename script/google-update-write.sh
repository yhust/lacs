#!/bin/bash

# set flintrockPemPath as an environmental variable
# $1 cache size per worker in MB $2 file number  $3 usernumber

filenumber=$2
usernumber=$3


read -r master < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${master} "cd ~/lacs;python python/generate_google_rates.py $usernumber $filenumber;bin/alluxio runLAWrite $1"

# copy pop file to all nodes
scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${master}:~/lacs/pop.txt ~/Desktop/
python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py copy-file lacs ~/Desktop/pop.txt /home/ec2-user/lacs/

	# Run mm-default
	#MaxMinDefault
	# Run rep-default
	#Default
exit 0
