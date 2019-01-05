import numpy as np
import time
import os
import sys
'''
user_label = 1: slow user
user_label = 2: fast user
'''
def LABenchmarkGoogleTrace(usr_id, total_count, user_label):
    # load the popularity vector
    popularity = list()
    fw = open("pop.txt","r")

    pop_str = fw.readlines()[usr_id]
    fw.close()

    if(user_label == 2):
        total_count *= 25
    for pop in pop_str.split(','):
        popularity.append(float(pop))
    popularity /= np.sum(popularity)
    file_num = len(popularity)

    intervals = list()
    #f = 1
    if user_label == 1:
        f = open("slow_interval.txt","r")
    else:
        f = open("fast_interval.txt","r")
    for line in f.readlines():
        intervals.append((float)(line))



    for i in range(0, int(total_count)):
        # get a file id from the popularity
        interval = np.random.choice(intervals, p=np.ones(len(intervals))* 1.0/len(intervals))
        print "sleep for %s seconds" % interval
        time.sleep(interval)
        if(user_label == 2): # to avoid congestion on the client side. We will launch two clients for each fast user. Each client has half submission rate.
            time.sleep(interval)
        file_id = np.random.choice(np.arange(0, file_num), p=popularity)
        print file_id
        os.system('bin/alluxio runLARead %s %s >> /tmp/log &' % (file_id, usr_id))

    os.system('echo "All read requests submitted!" ')




if __name__ == "__main__": # divide the intervals in the trace into two parts
    LABenchmarkGoogleTrace(int(sys.argv[1]), float(sys.argv[2]),int(sys.argv[3]))
    # f=open("interval.txt", "r")
    # f_slow = open("slow_interval.txt","w")
    # f_fast = open("fast_interval.txt","w")
    #
    # intervals = list()
    #
    # for line in f.readlines():
    #     intervals.append(float(line))
    # f.close()
    #
    # intervals.sort()
    #
    # fast_intervals = np.array(intervals[0:len(intervals)/2:1])
    # slow_intervals = np.array(intervals[len(intervals)/2:len(intervals):1])
    # print np.mean(fast_intervals)
    # print np.mean(slow_intervals)
    #
    # for i in range(len(intervals)/2):
    #     f_fast.write("%s\n" % intervals[i])
    # for i in np.arange(len(intervals)/2,len(intervals)):
    #     f_slow.write("%s\n" %intervals[i])
    # f_fast.close()
    # f_slow.close()


