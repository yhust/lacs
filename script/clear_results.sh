#!/bin/bash


# $1 worker# $2 client#

IPs=()
index=0
while read line ; do
	IPs[$index]="$line"
	let "index++"
done <  $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt

for ((i = $1+1; i < $1+$2+1; i++)) 
do
    echo $i
    slave="${IPs[$i]}"
    echo $slave
	ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${slave} "rm ~/lacs/logs/Google*"
done


