#!/usr/bin/env bash

read -ra slave_arr -d '' <<<"$SLAVES"
#SCRIPT="cd alluxio-la; bash killLAReader.sh"
#SCRIPT="mkdir /tmp/cold; mkdir /tmp/alluxioUDF"
SCRIPT="wget https://bootstrap.pypa.io/get-pip.py;
 python2.7 get-pip.py;pip install numpy"
#SCRIPT="rm -r /root/spark; rm -r /root/alluxio-load-balancing; rm -r ec-cache; rm -r scala"
for i in {0..49}
do
    slave="${slave_arr[$i]}"
    echo $slave
    ssh -l "root" "${slave_arr[$i]}" "${SCRIPT}"
done
