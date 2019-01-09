'''
Max-min cache allocation + default load balancing (max-free)

'''

from max_min_allocator import max_min_allocator
import numpy as np
import sys
import os

def mm_default(mu_vec, c_vec, rates, delta, is_cluster):

    path = os.getcwd()
    if(is_cluster==1):
        path = os.getcwd() + '/lacs'

    k = len(rates[:, 1])  # user number
    n = len(rates[1, :])  # file number
    m = len(c_vec)  # machine number

    loc_vec = np.zeros(n,dtype=np.int32)
    cache_vec = max_min_allocator(rates.copy(), sum(c_vec))
    rate_by_file = np.sum(rates, axis=0)
    sorted_rate_by_file = np.copy(rate_by_file)
    sorted_rate_by_file[::-1].sort()
    total_memory_rate = np.dot(cache_vec, rate_by_file)
    final_cache_vec= np.zeros(n) # might be slightly different from cache_vec due to load balancing. (some file does not get cached.)

    R_Lambda_M = np.zeros((k,m))  # per_user memory rate after rounding
    R_Lambda = np.zeros((k,m))  # per_user rate after rounding

    # now do max-free allocation

    # allocate cache at first
    cache_usage = np.zeros(m)
    sorted_rate_indices = np.argsort(-rate_by_file).tolist()
    remaining_file_indices = np.copy(sorted_rate_indices)

    placed_file_indices = list()
    for index_f in remaining_file_indices:
        rate = rate_by_file[index_f]
        #  find the machine with the largest available rate.

        available_rates = mu_vec - np.sum(R_Lambda, axis = 0)
        best_fit_index_m = np.argmax(available_rates)
        if(available_rates[best_fit_index_m]<0):
            print 'No machine can hold a file with access rate %s' % rate
            return False
        placed_file_indices.append(index_f)
        loc_vec[index_f] = best_fit_index_m
        R_Lambda[:, best_fit_index_m] += rates[:, index_f]
        if c_vec[best_fit_index_m] - cache_usage[best_fit_index_m] >= cache_vec[index_f]:
            R_Lambda_M[:, best_fit_index_m] += rates[:, index_f] * cache_vec[index_f]
            cache_usage[best_fit_index_m] += cache_vec[index_f]
            final_cache_vec[index_f] = cache_vec[index_f]

    # now calculate the average latency after rounding
    R_Lambda_D = R_Lambda - R_Lambda_M
    user_latencies_by_machine, user_latencies = user_latency(R_Lambda, R_Lambda_D, mu_vec, delta)

    latency_mm_default = (sum(np.multiply(np.array(mu_vec), np.reciprocal(mu_vec - np.sum(R_Lambda,axis = 0)))) - m + sum(sum(
        R_Lambda_D)) * delta) / sum(rate_by_file)



    # log the loc_vec, cache_vec and block_list in alloc.txt
    f = open(path+'/alloc.txt','w')
    f.write(','.join("{:d}".format(loc) for loc in loc_vec))
    f.write("\n")
    f.write(','.join("{0:.2f}".format(ratio)for ratio in final_cache_vec))
    f.write("\n")
    f.write("\n")
    f.close()

    # for debug
    f = open(path+'/alloc_mm_default.txt','w')
    f.write(','.join("{:d}".format(loc) for loc in loc_vec))
    f.write("\n")
    f.write(','.join("{0:.2f}".format(ratio) for ratio in cache_vec))
    f.write("\n")
    f.write("\n") # no blocking user
    f.close()

    # log latency in theory
    f = open(path+'/latency_mm_default.txt','w')
    f.write(','.join(str(latency) for latency in user_latencies))
    f.write("\n")
    f.write("%s\n" % latency_mm_default)
    f.close()

    print 'mm', user_latencies
    return latency_mm_default, user_latencies
    # print R_Lambda, cache_usage, latency_rounded,cached_file_indices

# get the latency of each user
def user_latency(R_Lambda, R_Lambda_D, mu_vec,delta):
    k = len(R_Lambda[:,0])
    m = len(R_Lambda[0,:])
    user_latencies= np.zeros(k)
    user_latencies_by_machine = np.zeros((k,m))
    rate_by_user = np.sum(R_Lambda, axis = 1)
    for index_u in range(k):
        for index_m in range(m):
            user_latencies_by_machine[index_u, index_m] = R_Lambda[index_u, index_m] / (mu_vec[index_m] - sum(R_Lambda[:,index_m])) + R_Lambda_D[index_u,index_m] * delta
            user_latencies[index_u] += R_Lambda[index_u,index_m] / (mu_vec[index_m] - sum(R_Lambda[:,index_m])) + R_Lambda_D[index_u,index_m] * delta
        user_latencies[index_u] /=  rate_by_user[index_u]

    # check accuracy:
    #print np.dot(user_latencies, rate_by_user)/sum(rate_by_user)
    return user_latencies_by_machine, user_latencies


if __name__ == '__main__':
    if(len(sys.argv)< 6):
        print "Usage: bandwidth(per-worker), workercount, filesize, cachesize(per-worker),delta"
    bandwidth = float(sys.argv[1])
    machine_number = int(sys.argv[2])
    filesize = float(sys.argv[3])
    cachesize = float(sys.argv[4])
    delta = float(sys.argv[5])
    iscluster= int(sys.argv[6])

    path = os.getcwd()
    if(iscluster==1):
        path = os.getcwd() + '/lacs'

    # read the rates from pop.txt
    with open(path+"/pop.txt", "r") as f:
        lines = f.readlines()
        user_number = len(lines)
        file_number = len(lines[0].split(','))
        rates = np.zeros((user_number,file_number))
        for index_u in range(user_number):
            line = lines[index_u]
            rates[index_u,:]= np.asfarray(np.array(line.split(',')), np.float)

    f.close()
    mu_vector = np.ones(machine_number)*bandwidth /filesize
    c_vector = np.ones(machine_number)*cachesize / filesize
    mm_default(mu_vector, c_vector, rates, delta)


