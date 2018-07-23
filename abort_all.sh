#!/usr/bin/env bash

read -ra slave_arr -d '' <<<"$SLAVES"
#SCRIPT="pkill SPReadTest;pkill SPReadExecutor"
#SCRIPT="ps ax | grep SPReadExecutor |awk -F ' ' '{print $1}' | xargs kill -9"
SCRIPT="cd alluxio-la; bash killLAReader.sh"
#SCRIPT="mkdir /tmp/cold; mkdir /tmp/alluxioUDF"
#SCRIPT="rm -r /root/spark; rm -r /root/alluxio-load-balancing; rm -r ec-cache; rm -r scala"
for i in {0..9}
do
    slave="${slave_arr[$i]}"
    echo $slave
    ssh -l "root" "${slave_arr[$i]}" "${SCRIPT}"
done
