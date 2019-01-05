
import numpy
import os
import sys
from os.path import dirname
import subprocess
import time




def ThrottleTest(usr_id, file_id, total_count, rate):


    for i in range(0, int(total_count)):
        # get a file id from the popularity
        interval = numpy.random.exponential(1.0/rate)
        print "sleep for %s seconds" % interval
        time.sleep(interval)
        os.system('bin/alluxio runLARead %s %s >> /tmp/log &' % (file_id, usr_id))

    os.system('echo "All read requests submitted!" ')


if __name__ == "__main__":
    ThrottleTest(int(sys.argv[1]), sys.argv[2], float(sys.argv[2]), float(sys.argv[3]))

