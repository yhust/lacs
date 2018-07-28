#!/bin/bash



k=$1   # number of users
n=400 # number of files
arrival_rate=0.2 #  request rate of normal users(as Poisson)
zipf_factor=1.05 # distribution
factor=$2 # ratio of request rates (aggressive to normal)

# Generate preference
python python/generate_rates.py $k $n $arrival_rate $zipf_factor $factor


# Synchronize the pop file
/root/spark-ec2/copy-dir /root/alluxio-la/pop.txt
