#!/bin/bash

# set flintrockPemPath as an environmental variable



# runDelta
read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt



for ((i= 50; i < 350; i=$((i+50))))
do
	echo $i
	ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "cd ~/lacs;bin/alluxio runDelta $i 100"
done

# get logs
mkdir ~/Desktop/delta_log
scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${line}:~/lacs/*.txt ~/Desktop/delta_log/


exit 0
