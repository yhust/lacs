#!/bin/bash

# set flintrockPemPath as an environmental variable
# $1 mode 


python3 $(cd `dirname $0`; cd ..; pwd)/flintrock/standalone.py run-command lacs "cd ~/lacs;sed -i '$ d' conf/alluxio-site.properties; echo 'alluxio.lacs.mode=$1' >> conf/alluxio-site.properties;"

read -r master < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${master} "cd ~/lacs;bin/alluxio-start.sh all;"
	
	# Run mm-default
	#MaxMinDefault
	# Run rep-default
	#Default
exit 0
