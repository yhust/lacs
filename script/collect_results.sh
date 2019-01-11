#!/bin/bash


# $1 worker# $2 client#

IPs=()
index=0
while read line ; do
	IPs[$index]="$line"
	let "index++"
done <  $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

mkdir ~/Desktop/Google_log
for ((i = $1+1; i < $1+$2+1; i++))
do
    echo $i
    slave="${IPs[$i]}"
    echo $slave
	scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${slave}:~/lacs/logs/Google* ~/Desktop/Google_log/
done


