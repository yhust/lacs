#!/bin/bash

# set flintrockPemPath as an environmental variable
# $1 file size (in MB) $2 file number $3 access count for each test point

# prepare the alloc.txt ==> no use but lacs needs this to initialize

read -r line < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "cd ~/lacs; rm python/alloc.txt; touch python/alloc.txt;"

allocStr=""
i=0
while [ $i -le $2 ]
do
	echo $2
	echo $allocStr
	allocStr=${allocStr}"1," 
	#ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "echo -n '1,' >> python/alloc.txt"
	let "i++"
done
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "echo -n ${allocStr%?} >> ~/lacs/python/alloc.txt"




#ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "cd ~/lacs; bin/alluxio runModelTest $1 $2 $3"


# get logs
mkdir ~/Desktop/model_log
scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${line}:~/lacs/logs/model_test.txt ~/Desktop/model_log/


exit 0
