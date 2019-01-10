from rounding_opt import rounding_opt
import numpy as np
from la_fair_rounding import la_fair_rounding
from isolation import get_iso_latency
from copy import copy
import sys
import os

def lacs(mu_vector, c_vector, rates, delta, user_si, is_cluster):
    path = os.getcwd()
    if(is_cluster==1):
        path = os.getcwd() + '/lacs'


    k = len(rates[:, 1])  # user number
    n = len(rates[1, :])  # file number
    m = len(mu_vector)
    #print mu_vector
    #print c_vector
    user_latencies, latency_la, latency_la_rounded , loc_vec, cache_vec= la_fair_rounding(mu_vector, c_vector,
                                                                                           rates.copy(), delta)
    #print 'la first round', latency_la_rounded
    #unisolated_user_ids = range(k)
    isolated_user_ids = list()
    cachable_user_rates = np.sum(rates,axis = 1) # only used for finding the next user with the highest rate. The rates of the removed users will be set to zero
    cachable_user_ids = range(k)
    final_user_latencies = user_latencies.copy()

    rate_by_user = np.sum(rates, axis= 1)
    unisolated_user_ids = np.argsort(rate_by_user)[::-1] # initial: decending order by rates


    if  (user_si[unisolated_user_ids] < final_user_latencies[unisolated_user_ids]).any():

        # release the cache budget of all users whose rates are above the isolated threshold
        for index_u in range(k):
            if rate_by_user[index_u] >= sum(mu_vector)/k:
                cachable_user_rates[index_u] = 0
                cachable_user_ids.remove(index_u)
                print "user ", index_u, "'s cache is revoked"
        final_user_latencies, latency_la, latency_la_rounded , loc_vec, cache_vec= la_fair_rounding(
            mu_vector, c_vector, rates.copy(), delta, cachable_user_ids, unisolated_user_ids)

        while (np.round(user_si[unisolated_user_ids], decimals=4) < np.round(final_user_latencies[unisolated_user_ids], decimals=4)).any() and len(unisolated_user_ids)>0:

            # todo: check whether we could re-balance the rates of this user so that no users will be affected.

            # if the above solutions do not work, block its rate to isolation: the worst case is where everyone gets isolated
            #if (user_si[unisolated_user_ids] < final_user_latencies[unisolated_user_ids]).any():

            this_user_id = unisolated_user_ids[0] # the first one is the largest one  # np.argmax(rate_by_user[unisolated_user_ids])
            unisolated_user_ids = np.delete(unisolated_user_ids,0)
            #unisolated_user_ids.remove(this_user_id)
            isolated_user_ids.append(this_user_id)
            print 'user', this_user_id, 'is isolated'
            final_user_latencies[this_user_id]  = float('Inf')
            final_user_latencies[unisolated_user_ids], latency_la, latency_la_rounded , loc_vec, cache_vec= la_fair_rounding(mu_vector * ( 1.0 * len(unisolated_user_ids)/k), c_vector, rates.copy(), delta, cachable_user_ids, unisolated_user_ids)

    # log the loc_vec, cache_vec and block_list in alloc.txt
    f = open(path+'/alloc.txt','w')
    f.write(','.join("{:d}".format(loc) for loc in loc_vec))
    f.write("\n")
    f.write(','.join("{0:.2f}".format(ratio)for ratio in cache_vec))
    f.write("\n")
    f.write(','.join("{0:d}".format(id) for id in isolated_user_ids)) #todo: check what will happen if isolated_user_ids is empty
    f.write("\n")
    f.close()

    # for debug
    f = open(path+'/alloc_la.txt','w')
    f.write(','.join("{:d}".format(loc) for loc in loc_vec))
    f.write("\n")
    f.write(','.join("{0:.2f}".format(ratio)for ratio in cache_vec))
    f.write("\n")
    f.write(','.join("{0:d}".format(id) for id in isolated_user_ids)) #todo: check what will happen if isolated_user_ids is empty
    f.write("\n")
    f.close()

    print 'lacs:', final_user_latencies
    print 'lacs isolated users',  isolated_user_ids
    return latency_la_rounded, final_user_latencies

if __name__ == '__main__':
    if(len(sys.argv)< 6):
        print "Usage: bandwidth(per-worker), workercount, filesize, cachesize(per-worker),delta"
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
    with open(path+'/pop.txt', "r") as f:
        lines = f.readlines()
        user_number = len(lines)
        file_number = len(lines[0].split(','))
        rates = np.zeros((user_number,file_number))
        for index_u in range(user_number):
            line = lines[index_u]
            rates[index_u,:]= np.asfarray(np.array(line.split(',')), np.float)

    f.close()
    mu_vector = np.ones(machine_number)*bandwidth/filesize
    c_vector = np.ones(machine_number)*cachesize/filesize
    avg_si, user_si, Lambda, Lambda_D = get_iso_latency(mu_vector, c_vector, rates,delta)
    print rates
    lacs(mu_vector, c_vector, rates, delta, user_si,iscluster)


    # todo: 1. for those isolated users, allow them to have their files cached. no!

