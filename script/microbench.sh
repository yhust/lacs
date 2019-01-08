#!/bin/bash

# set flintrockPemPath as an environmental variable
# $1 file number $2 cache size per worker (MB) $3 access count for each test point $4 loop# $5 mode


read -r master < $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt
#ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "cd ~/lacs; rm logs/microbench-time.txt;rm logs/microbench-hr.txt;"
ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${master} "cd ~/lacs;sed -i '$ d' conf/alluxio-site.properties; echo 'alluxio.lacs.mode=$5' >> conf/alluxio-site.properties;bin/alluxio-start.sh all;"


IPs=()
index=0
while read line ; do
    IPs[$index]="$line"
    let "index++"
done <  $(cd `dirname $0`; cd ..; pwd)/flintrock/flintrock.txt 

# set the ips of the two clients
client1=${IPs[${#IPs[@]}-1]}   # the last one
client2=${IPs[${#IPs[@]}-2]} 

rate1=1
rate2=1


i=0
while [ $i -lt $4 ]
do
	# generate prefs
	ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${master} "cd ~/lacs; python python/generate_microbench_rates.py $1 $rate1 $rate2"
	# Run LACS	 
	ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${master} "bin/alluxio runLAWrite $2"
		# submit requests
	ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${client1} "cd ~/lacs;bin/alluxio runBenchmark 'microbench' $2 100 $1 $rate1 $3 >> /tmp &"
	ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${client1} "cd ~/lacs;bin/alluxio runBenchmark 'microbench' $2 100 $1 $rate2 $3 >> /tmp &"
	
	
	# Run mm-default
	#MaxMinDefault
	# Run rep-default
	#Default
	let "rate2=rate2+1"
done




#ssh -o StrictHostKeyChecking=no -i $flintrockPemPath ${line} "cd ~/lacs; bin/alluxio runModelTest $1 $2 $3"


# get logs
mkdir ~/Desktop/microbench_log
scp -o StrictHostKeyChecking=no -i $flintrockPemPath -r ${line}:~/lacs/logs/microbench* ~/Desktop/microbench_log/


exit 0
