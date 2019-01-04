#!/bin/bash


user_count=$1
rate=$2 # access rate
total_count=$3   # total count of access

read -ra slave_arr -d '' <<<"$SLAVES"

#SCRIPT="cd alluxio-la;PATH=$PATH python python/LABenchmark.py $2 $3 > /tmp/pythonlog &"
#echo $SCRIPT

slave="${slave_arr[10]}"
for ((i = 0; i < $user_count; i++))
do
 echo $i
 slave="${slave_arr[$((i + 10))]}"
 echo $slave
 SCRIPT="cd alluxio-la;PATH=$PATH python python/LABenchmark.py 0 $total_count $rate > /tmp/pythonlog &"
 echo $SCRIPT
 ssh -l "root" "${slave}" "${SCRIPT}" # error of alluxio: unknown command
done
