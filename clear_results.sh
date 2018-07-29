#!/bin/bash



# clear the readTimes.txt on the last 20 slaves
read -ra slave_arr -d '' <<<"$SLAVES"
SCRIPT="rm alluxio-la/logs/*.txt"
for ((i = 10; i < 10+$1; i++))
do
    echo $i
    slave="${slave_arr[$i]}"
    echo $slave
    ssh -l "root" "${slave_arr[$i]}" "${SCRIPT}"
done

# Also clear results collected locally
#cp results/all_results.txt all_results_backup.txt # in case forgetting to backup important results
#rm results/all_results.txt
rm results/*.txt
