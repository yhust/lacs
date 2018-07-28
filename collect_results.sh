#!/bin/bash


# $1 is the first parameter means the number of clients
# run read tests on the last $1 slaves
read -ra slave_arr -d '' <<<"$SLAVES"
rm results/*.txt
touch results/all_latency.txt
touch results/all_cacheHit.txt
#touch results/all_workerLoads.txt

#SCRIPT="cd alluxio-la; ./bin/alluxio readTest &>/dev/null &"
for ((i = 10; i < 25; i++))
do
    echo $i
    slave="${slave_arr[$i]}"
    echo $slave
    #scp root@${slave_arr[$i]}:/root/alluxio-la/test_files/readTimes.txt test_files/$i.txt
    scp root@${slave_arr[$i]}:/root/alluxio-la/logs/readLatency.txt /root/alluxio-la/results/${i}_latency.txt
    cat results/${i}_latency.txt >> results/all_latency.txt
    scp root@${slave_arr[$i]}:/root/alluxio-la/logs/cacheHit.txt /root/alluxio-la/results/${i}_cacheHit.txt
    cat results/${i}_cacheHit.txt >> results/all_cacheHit.txt
    scp root@${slave_arr[$i]}:/root/alluxio-la/logs/workerLoad.txt /root/alluxio-la/results/${i}_loads.txt
    cat results/${i}_loads.txt >> results/all_loads.txt
    #scp root@${slave_arr[$i]}:/root/alluxio-la/logs/workerLoads.txt /root/alluxio-la/results/${i}_workerLoads.txt
    #cat results/${i}_workerLoads.txt >> results/all_workerLoads.txt
done

# Collect all the results into a single file for the convience
#cd results
#rm all_results.txt
#cat *.txt > all_results.txt
#cd ..

