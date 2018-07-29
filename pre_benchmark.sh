#!/bin/bash



k=$1   # number of users
n=400 # number of files
arrival_rate=$2 #  request rate of normal users(as Poisson)
zipf_factor=1.05 # distribution
factor=$3 # ratio of request rates (aggressive to normal)

# Generate preference
python python/generate_rates.py $k $n $arrival_rate $zipf_factor $factor
#PATH=$PATH python python/mm_default.py 76 10 10 1000 2.12
#/usr/bin/python2.7 python/lacs.py 120 10 100 2000 0.215



# Write test files
#bin/alluxio runLAWrite

# Synchronize the pop file
/root/spark-ec2/copy-dir /root/alluxio-la/pop.txt

echo Done
