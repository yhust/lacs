#!/bin/bash

# set flintrockPemPath as an environmental variable
# $1 file number $2 access count for each test point $3 rate1 $4 rate2

IPs=()
index=0
while read line ; do
    IPs[$index]="$line"
    let "index++"
done <  $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt 

# set the ips of the two clients
client1=${IPs[${#IPs[@]}-1]}   # the last one
client2=${IPs[${#IPs[@]}-2]} 

rate1=$3
rate2=$4

echo 'start'

echo $client1
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${client1} "cd ~/lacs;bin/alluxio runLABenchmark 'microbench' $1 $rate1 $2 1 >> /tmp/log &"
echo $client2
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${client2} "cd ~/lacs;bin/alluxio runLABenchmark 'microbench' $1 $rate2 $2 2 >> /tmp/log &"
	


exit 0
