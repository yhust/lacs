#!/bin/bash

# set flintrockPemPath as an environmental variable
# $1 file number $2 access count for each test point $3 rate $4 factor

filenumber=$1
accesscount=$2
rate=$3
factor=$4

IPs=()
index=0
while read line ; do
    IPs[$index]="$line"
    let "index++"
done <  $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt 

echo 'slow clients'
echo $rate
for ((i = 31; i < 36; i++))
do
	echo $((i-30)) # user id start from 1
 	client="${IPs[$i]}"
 	echo $client
 	ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${client} "cd ~/lacs;bin/alluxio runLABenchmark 'benchmark' $filenumber $rate $accesscount $((i-30)) >/tmp/log 2>/tmp/err &"
done

echo 'medium clients'
rate=$(expr $rate*$factor | bc)
echo $rate
for ((i = 36; i < 41; i++))
do
	echo $((i-30))
 	client="${IPs[$i]}"
 	echo $client
 	ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${client} "cd ~/lacs;bin/alluxio runLABenchmark 'benchmark' $filenumber $rate $accesscount $((i-30)) >/tmp/log 2>/tmp/err &"
done

echo 'fast clients'
rate=$(expr $rate*$factor | bc)
echo $rate
for ((i = 41; i < 46; i++))
do
	echo $((i-30))
 	client="${IPs[$i]}"
 	echo $client
 	ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${client} "cd ~/lacs;bin/alluxio runLABenchmark 'benchmark' $filenumber $rate $accesscount $((i-30)) >/tmp/log 2>/tmp/err &"
done


exit 0
