#!/bin/bash


total_count=$1   # total count of access
rate=$2 # access rate
factor=$3 # ratio of normal user and aggressive user

read -ra slave_arr -d '' <<<"$SLAVES"

#SCRIPT="cd alluxio-la;PATH=$PATH python python/LABenchmark.py $2 $3 > /tmp/pythonlog &"
#echo $SCRIPT

for ((i = 0; i < 5; i++))
do
 echo $i
 slave="${slave_arr[$((i + 10))]}"
 echo $slave
 SCRIPT="cd alluxio-la;PATH=$PATH python python/LABenchmark.py $i $total_count $rate > /tmp/pythonlog &"
 echo $SCRIPT
 ssh -l "root" "${slave}" "${SCRIPT}" # error of alluxio: unknown command
done

for ((i = 5; i < 10; i++))
do
 echo $i
 slave="${slave_arr[$((i + 10))]}"
 echo $slave
 SCRIPT="cd alluxio-la;PATH=$PATH python python/LABenchmark.py $i $(expr $total_count*$factor | bc) $(expr $rate*$factor | bc) > /tmp/pythonlog &"
 echo $SCRIPT
 ssh -l "root" "${slave}" "${SCRIPT}" # error of alluxio: unknown command
done


#To avoid congestion on the client side, submit the requests of fast users from two.
for ((i = 10; i < 15; i++))
do
 echo $i
 slave="${slave_arr[$((i + 10))]}" 
 SCRIPT="cd alluxio-la;PATH=$PATH python python/LABenchmark.py $i $(expr $total_count*$factor*$factori/2 | bc) $(expr $rate*$factor*$factor/2 | bc) > /tmp/pythonlog &"
 echo $slave
 echo $SCRIPT
 ssh -l "root" "${slave}" "${SCRIPT}" # error of alluxio: unknown command
done

#To avoid congestion on the client side, submit the requests of fast users from two.
for ((i = 15; i < 20; i++))
do
 echo $i
 slave="${slave_arr[$((i + 10))]}"
 SCRIPT="cd alluxio-la;PATH=$PATH python python/LABenchmark.py $((i-5)) $(expr $total_count*$factor*$factori/2 | bc) $(expr $rate*$factor*$factor/2 | bc) > /tmp/pythonlog &"
 echo $slave
 echo $SCRIPT
 ssh -l "root" "${slave}" "${SCRIPT}" # error of alluxio: unknown command
done

