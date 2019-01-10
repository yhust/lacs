#!/bin/bash

# set flintrockPemPath as an environmental variable
# $1 file number $2 cache size per worker (MB) $3 access count for each test point $4 rate1 $5 rate2

IPs=()
index=0
while read line ; do
    IPs[$index]="$line"
    let "index++"
done <  $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt 

# set the ips of the two clients
client1=${IPs[${#IPs[@]}-1]}   # the last one
client2=${IPs[${#IPs[@]}-2]} 

rate1=$4
rate2=$5

echo 'start'
read -r master < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt
#ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${master} "cd ~/lacs; python python/generate_microbench_rates.py $1 $rate1 $rate2; bin/alluxio runLAWrite $2"

#exit 0

ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${client1} "cd ~/lacs;bin/alluxio runBenchmark 'microbench' $1 $rate1 $3 1 >> /tmp/log &"
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${client2} "cd ~/lacs;bin/alluxio runBenchmark 'microbench' $1 $rate2 $3 2 >> /tmp/log &"
	
	


# get logs
#mkdir ~/Desktop/microbench_log
#scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${client1}:~/lacs/logs/microbench-time.txt ~/Desktop/microbench_log/1-time.txt
#scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${client1}:~/lacs/logs/microbench-hr.txt ~/Desktop/microbench_log/1-hr.txt
#scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${client2}:~/lacs/logs/microbench-time.txt ~/Desktop/microbench_log/2-time.txt
#scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${client2}:~/lacs/logs/microbench-hr.txt ~/Desktop/microbench_log/2-hr.txt

exit 0
