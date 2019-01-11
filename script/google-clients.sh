#!/bin/bash

# set flintrockPemPath as an environmental variable
# $1 file number $2 access count for each test point
filenumber=$1
accesscount=$2


IPs=()
index=0
while read line ; do
    IPs[$index]="$line"
    let "index++"
done <  $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt 

echo 'slow clients'
for ((i = 31; i < 38; i++))
do
	echo $i
 	client="${IPs[$i]}"
 	echo $client
 	#ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${client} "cd ~/lacs;bin/alluxio runLABenchmark 'Google' $filenumber $rate $accesscount $i 0 >/tmp/log 2>/tmp/err &"
done


echo 'fast clients'
for ((i = 38; i < 51; i++))
do
	echo $i
 	client="${IPs[$i]}"
 	echo $client
 	#ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${client} "cd ~/lacs;bin/alluxio runLABenchmark 'Google' $filenumber $rate $accesscount $i 1 >/tmp/log 2>/tmp/err &"
done

exit 0
