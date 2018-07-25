#!/bin/bash


total_count=$1   # total count of access
rate=$2 # access rate
factor=$3 # ratio of normal user and aggressive user

read -ra slave_arr -d '' <<<"$SLAVES"

#SCRIPT="cd alluxio-la;PATH=$PATH python python/LABenchmark.py $2 $3 > /tmp/pythonlog &"
#echo $SCRIPT

#one slow user
slave="${slave_arr[10]}"
SCRIPT="cd alluxio-la;PATH=$PATH python python/LABenchmark.py 0 $total_count $rate > /tmp/pythonlog &"
ssh -l "root" "${slave}" "${SCRIPT}"


SCRIPT="cd alluxio-la;PATH=$PATH python python/LABenchmark.py 1 $total_count $rate > /tmp/pythonlog &"
for ((i = 11; i < $factor + 11; i++))
do
  echo $i
  slave="${slave_arr[$i]}"  
  ssh -l "root" "${slave}" "${SCRIPT}" # error of alluxio: unknown command
done


#one fast user
#slave="${slave_arr[11]}"
#echo slave
#SCRIPT="cd alluxio-la;PATH=$PATH python python/LABenchmark.py 1 $(expr $total_count*$factor | bc) $(expr $rate*$factor | bc) > /tmp/pythonlog &" #$(($rate * $factor))
#echo $SCRIPT
#ssh -l "root" "${slave}" "${SCRIPT}"



#for ((i = 30; i < $1 + 30; i++))
#do
 #   echo $i
 #   slave="${slave_arr[$i]}"
 #   echo $slave
 #   ssh -l "root" "${slave_arr[$i]}" "${SCRIPT}" # error of alluxio: unknown command
   
#done
echo Done
