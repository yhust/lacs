#!/bin/bash



k=$1   # number of users
n=$2 # number of files
arrival_rate=$3 #  request rate of normal users(as Poisson)
zipf_factor=$4 # distribution
factor=$5 # ratio of request rates (aggressive to normal)

# Generate preference
python python/generate_rates.py $k $n $arrival_rate $zipf_factor $factor


# Write test files
bin/alluxio runLAWrite

# Synchronize the pop file
/root/spark-ec2/copy-dir /root/alluxio-la/python/pop.txt

echo Done
