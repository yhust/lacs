#!/bin/bash

# set flintrockPemPath as an environmental variable
# $1 file size (in MB) $2 file number $3 access count for each test point

# write test files in the first tier
read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "cd ~/lacs; bin/alluxio runModelTest $1 $2 $3"


# get logs
mkdir ~/Desktop/model_log
scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${line}:~/lacs/logs/model_test.txt ~/Desktop/model_log/


exit 0
