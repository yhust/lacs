#!/bin/bash

# set flintrockPemPath as an environmental variable
# $1 mode 


read -r master < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${master} "cd ~/lacs;sed -i '$ d' conf/alluxio-site.properties; echo 'alluxio.lacs.mode=$1' >> conf/alluxio-site.properties;bin/alluxio-start.sh all;"
	
	# Run mm-default
	#MaxMinDefault
	# Run rep-default
	#Default
exit 0
