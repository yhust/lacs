'''
Decide the allocation under isolation.


There could be multiple copies of the same file. Therefore, we need to output the allocation map of each user.

'''
import numpy as np
from linear_relaxation import linear_relaxation
import sys
import os

def get_iso_latency(mu_vec, c_vec, rates, delta): # unrounded performance

    k = len(rates[:,0])
    n = len(rates[0,:])
    m = len(mu_vec)
    rate_by_user = np.sum(rates, axis = 1)
    iso_latency = np.zeros(k)
    user_machine_latencies = np.ones((k,m))*(-1)
    user_machine_rates = np.zeros((k, m))
    user_macnine_disk_rates = np.zeros((k, m))
    Lambda = np.zeros((k,m))
    Lambda_D = np.zeros((k,m))



    for index_u in range(k):
        Lambda[index_u,:], Lambda_D[index_u,:], iso_latency[index_u] = linear_relaxation(mu_vec / k, c_vec / k, rates[index_u, :].copy(), delta)
        user_machine_rates[index_u,:] = Lambda[index_u,:]
        user_macnine_disk_rates[index_u,:] =Lambda_D[index_u,:]
        if(iso_latency[index_u] != float('Inf')):
            for index_m in range(m):
                user_machine_latencies[index_u,index_m] = Lambda[index_u, index_m] / (mu_vec[index_m]/k -  Lambda[index_u,index_m]) + delta * Lambda_D[index_u, index_m]
        #print 'si: user ', index_u, 'latencies', user_machine_latencies[index_u,:], iso_latency[index_u]
        #print 'total rate', Lambda, 'disk rate', Lambda_D
    avg_si = np.dot(rate_by_user,iso_latency ) / sum(rate_by_user)
    #print 'si, memory rate', np.sum(user_machine_rates - user_macnine_disk_rates, axis = 1)

    return avg_si, iso_latency, Lambda, Lambda_D



def get_iso_allocation(mu_vec, c_vec, rates,delta, is_cluster): # and also rounded performance
    # what if a user can't get stable performance, i.e., inf latency in theory, leave to max-free algorithm
    k = len(rates[:,0])
    n = len(rates[0,:])
    m = len(mu_vec)
    avg_si, iso_latency, Lambda, Lambda_D = get_iso_latency(mu_vec, c_vec, rates,delta)

    mu_vec_iso = mu_vec/k
    c_vec_iso = c_vec/k


    path = os.getcwd()
    if(is_cluster==1):
        path = os.getcwd() + '/lacs'
    f= open(path+'/alloc.txt', 'w')
    user_iso = np.zeros(k)
    rate_by_user = np.sum(rates, axis = 1)

    for index_u in range(k):
        R_Lambda_M = np.zeros(m)  #  memory rate after rounding
        R_Lambda = np.zeros(m)  # total rate after rounding
        rate_by_file = rates[index_u,:].copy()
        cache_usage = np.zeros(m)
        sorted_rate_indices=np.argsort(-rate_by_file).tolist()
        remaining_file_indices = np.copy(sorted_rate_indices)

        placed_file_indices = list()
        loc_vec = np.zeros(n,dtype=np.int32)
        cache_vec = np.zeros(n)


        if(iso_latency[index_u] != float('Inf')): # try to fit Lambda and Lambda_M
            for index_f in remaining_file_indices: # sort by file rate
                rate = rate_by_file[index_f]
                # Put the file on the machine with the largest gap of opt_mem_rate - current_mem_rate (could be negative)
                best_fit_index_m = -1  # np.argmin(map(abs, np.add(R_Lambda, np.ones(m)*rate-Lambda)))
                max_diff = -float('Inf')

                # find the best-fit of memory-rate
                for index_m in range(0, m):
                    if R_Lambda[index_m] + rate < mu_vec_iso[index_m]:
                        diff = Lambda[index_u, index_m] - Lambda_D[index_u, index_m] - R_Lambda_M[index_m]
                        if diff > max_diff and c_vec_iso[index_m] - cache_usage[index_m] >= 1: #and Lambda[index_u, index_m] >= R_Lambda[index_m] + rate:
                            best_fit_index_m = index_m
                            max_diff = diff
                if best_fit_index_m != -1:
                    placed_file_indices.append(index_f)
                    R_Lambda_M[best_fit_index_m] += rates[index_u, index_f]
                    R_Lambda[best_fit_index_m] += rates[index_u, index_f]
                    cache_usage[best_fit_index_m] += 1
                    loc_vec[index_f] = best_fit_index_m
                    cache_vec[index_f] = 1
                else:
                    break # no need to continue?

            remaining_file_indices = [item for item in remaining_file_indices if item not in placed_file_indices]
            # next find the best-fit of total rate
            for index_f in remaining_file_indices:
                rate = rate_by_file[index_f]
                best_fit_index_m = -1
                max_diff = -float('Inf')
                for index_m in range(0, m):
                    if R_Lambda[index_m] + rate < mu_vec_iso[index_m]:
                        diff = Lambda[index_u,index_m] - R_Lambda[index_m]
                        if diff > max_diff:
                            best_fit_index_m = index_m
                            max_diff = diff
                if best_fit_index_m != -1:
                    placed_file_indices.append(index_f)
                    R_Lambda[best_fit_index_m] += rates[index_u, index_f]
                    loc_vec[index_f] = best_fit_index_m
                    # if there is sufficient cache space on the best-fit machine:
                    if c_vec_iso[best_fit_index_m] - cache_usage[best_fit_index_m] >= 1:
                        R_Lambda_M[best_fit_index_m] += rates[index_u, index_f]
                        cache_vec[index_f] = 1
                        cache_usage[best_fit_index_m] += 1
                else:
                    break

            remaining_file_indices = [item for item in remaining_file_indices if item not in placed_file_indices]


        # max-free allocation. unstable users starts from here.
        for index_f in remaining_file_indices:
            rate = rate_by_file[index_f]
            #  find the machine with the largeset available rate.

            available_rates = mu_vec_iso - R_Lambda
            best_fit_index_m = np.argmax(available_rates)
            if (available_rates[best_fit_index_m] < rate):
                print 'No machine can hold a file with access rate %s' % rate
                user_iso[index_u] = float('Inf')
                break
            placed_file_indices.append(index_f)
            R_Lambda[best_fit_index_m] += rates[index_u, index_f]
            loc_vec[index_f] = best_fit_index_m
            # if there is sufficient cache space on the best-fit machine:
            if c_vec_iso[best_fit_index_m] - cache_usage[best_fit_index_m] >= 1:
                R_Lambda_M[best_fit_index_m] += rates[index_u, index_f]
                cache_vec[index_f] = 1
                cache_usage[best_fit_index_m] += 1

        if(user_iso[index_u] == float('Inf')):
            continue
        R_Lambda_D = R_Lambda - R_Lambda_M
        #get_cache_hit(rate_by_file, cache_vec)

        latency = 0
        for index_m in range(m):
            latency += R_Lambda[index_m] / (mu_vec_iso[index_m] - R_Lambda[index_m]) + R_Lambda_D[index_m] * delta
        latency /= sum(rate_by_file)
        user_iso[index_u]=latency

        f.write(','.join("{:d}".format(loc) for loc in loc_vec))
        f.write("\n")
        f.write(','.join("{0:.2f}".format(ratio)for ratio in cache_vec))
        f.write("\n")
        f.write(','.join("{:d}".format(id) for id in range(k)))
        f.write("\n")

    f.close()
    avg_iso = np.dot(rate_by_user,iso_latency ) / sum(rate_by_user)
    print 'isolation', user_iso
    return avg_iso, user_iso

def get_cache_hit(rates, cache_vec):
    k = len(rates[:,0])
    n = len(rates[0,:])
    for index_u in range(k):
        rate = rates[index_u,:].copy()
        np.sort(rate)
        print sum(rate[n-int(sum(cache_vec))+1:n]) / sum(rate)

if __name__ == '__main__':
    if(len(sys.argv)< 6):
        print "Usage: bandwidth(per-worker), workercount, filesize, cachesize(per-worker), delta"
    bandwidth = float(sys.argv[1])
    machine_number = int(sys.argv[2])
    filesize = float(sys.argv[3])
    cachesize = float(sys.argv[4])
    delta = float(sys.argv[5])
    iscluster = int(sys.argv[6])

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

    get_cache_hit(rates.copy(),c_vector)
    get_iso_allocation(mu_vector, c_vector, rates, delta, iscluster)
