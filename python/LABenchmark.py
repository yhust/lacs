
import numpy
import os
import sys
from os.path import dirname
import subprocess
import time

'''
At every client node

Submit file requests according to the popularity profile.
'''

#totalCount = 3 # the number of total file requests

def LABenchmark(usr_id, total_count, rate):

    # load the popularity vector
    popularity = list()
    fw = open("pop.txt","r")

    pop_str = fw.readlines()[usr_id]
    fw.close()


    for pop in pop_str.split(','):
        popularity.append(float(pop))
    popularity /= numpy.sum(popularity)
    file_num = len(popularity)
    print popularity
    for i in range(0, int(total_count)):
        # get a file id from the popularity
        interval = numpy.random.exponential(1.0/rate)
        print "sleep for %s seconds" % interval
        time.sleep(interval)
        file_id = numpy.random.choice(numpy.arange(0, file_num), p=popularity)
        os.system('bin/alluxio runLARead %s %s >> /tmp/log &' % (file_id, usr_id))

    os.system('echo "All read requests submitted!" ')


if __name__ == "__main__":
    LABenchmark(int(sys.argv[1]), float(sys.argv[2]),float(sys.argv[3]))

