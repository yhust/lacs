from la_fair_allocator import la_fair_allocator
import numpy as np
from cvxpy import *
from math import fabs

'''
cached_files: the files cached by isolated users
cachable_users: users who are allowed to have cache claim
unisolated users: users who are not isolated

'''

def la_fair_rounding(mu_vec, c_vec, rates, delta, cachable_users = False, unisolated_users = False):
    k = len(rates[:,0])  # user number
    n = len(rates[0, :])  # file number
    m = len(c_vec)  # machine number
    loc_vec = np.zeros(n,dtype=np.int32) # location vec


    if isinstance(unisolated_users, bool):
        unisolated_users = range(k)
    if isinstance(cachable_users, bool):
        cachable_users = range(k)


    # try:
    cachable_rates = rates[cachable_users,:].copy()
    cache_vec = la_fair_allocator(cachable_rates.copy(), sum(c_vec)) # the cache ratio vec
    #print "cache_vec", cache_vec

    # except:
    #     1
    unisolated_rates = rates[unisolated_users, :].copy()
    k = len(unisolated_rates[:, 0])  # user number
    #memory_rates = np.dot(rates, cache_vec)
    #print 'Ideal memory rate with la', memory_rates
    rate_by_file = np.sum(unisolated_rates, axis=0)
    cachable_rate_by_file = np.sum(cachable_rates, axis=0)
    cached_rate_by_file = np.multiply(cache_vec, cachable_rate_by_file)
    sorted_file_indices_by_cached_rate = sorted(range(n),key= lambda i : -cached_rate_by_file[i]) # todo check it


    total_memory_rate = np.dot(cache_vec, rate_by_file) #cachable_rate_by_file)
    user_memory_rate = np.dot(unisolated_rates, cache_vec)
    #print 'la, user memory rate', user_memory_rate
    max_memory_rate_by_machine = np.zeros(m)




    for index_m in range(m):
        usage = 0
        rate = 0
        for index_f in sorted_file_indices_by_cached_rate:
            if usage > c_vec[index_m]:
                max_memory_rate_by_machine[index_m] = rate
                break
            usage += cache_vec[index_f]
            rate += cached_rate_by_file[index_f]

    Lambda = Variable(m)  # total rate to machines
    Lambda_D = Variable(m)  # total rate to disks of machines



    func = (mu_vec.T * inv_pos(mu_vec - Lambda) - m + sum_entries(Lambda_D) * delta) / sum(rate_by_file)

    objective = Minimize(func)
    # constraints = [0 <= A, A <= 1, 0<=D, D<= 1, Lambda< mu_vec, A.T*np.ones((n,1))<= c_vec, (A+D) * np.ones((m,1)) == np.ones((n,1)), Lambda ==(A+D).T*rate_vec, Lambda_D == D.T*rate_vec]
    constraints = [Lambda < mu_vec, Lambda_D <= Lambda, 0 <= Lambda_D, Lambda - Lambda_D <= max_memory_rate_by_machine,
                   sum_entries(Lambda - Lambda_D) <= total_memory_rate, sum_entries(Lambda) == sum(rate_by_file)]

    prob = Problem(objective, constraints)
    try:
        result = prob.solve()
        if prob.status == 'optimal' or prob.status == 'optimal_inaccurate':
            # return np.squeeze(np.asarray(Lambda.value)), np.squeeze(np.asarray(Lambda_D.value)),
            latency_la = result
        else:
            return False
    except Exception as e:
            return False

    Lambda = np.squeeze(np.asarray(Lambda.value))
    Lambda_D = np.squeeze(np.asarray(Lambda_D.value))
    Lambda_M = Lambda- Lambda_D
    if Lambda.size == 1: # only one machine
        Lambda = np.array([Lambda])
        Lambda_D = np.array([Lambda_D])
        Lambda_M = np.array([Lambda_M])



    # if we want to check per-user performances, we need to round the results

    R_Lambda_M = np.zeros((k,m))  # user memory rate after rounding
    R_Lambda = np.zeros((k,m))  # user rate after rounding
    cache_usage = np.zeros(m)
    sorted_indices_m = np.argsort(-(Lambda - Lambda_D)).tolist()  # from the largest memory rate to the smallest
    memory_rate_by_file = rate_by_file * cache_vec
    sorted_memory_rate_indices = np.argsort(-memory_rate_by_file).tolist()
    sorted_rate_indices=np.argsort(-rate_by_file).tolist()

    remaining_file_indices = np.copy(sorted_rate_indices)

    placed_file_indices = list()
    countable_m_rate = np.zeros(m)

    # todo: try to reuse the cache of isolated users?

    for index_f in remaining_file_indices:
        rate = rate_by_file[index_f]
        # Put the file on the machine with the largest gap of opt_mem_rate - current_mem_rate (could be negative)
        best_fit_index_m = -1  # np.argmin(map(abs, np.add(R_Lambda, np.ones(m)*rate-Lambda)))
        max_diff = -float('Inf')

        # find the best-fit of memory-rate
        for index_m in range(0, m):
            if sum(R_Lambda[:, index_m]) + rate < mu_vec[index_m]:
                diff = Lambda[index_m] - Lambda_D[index_m] - sum(R_Lambda_M[:, index_m]) #countable_m_rate[index_m] #
                if diff > max_diff and c_vec[index_m] - cache_usage[index_m] >= cache_vec[index_f] and Lambda[index_m] >= sum(R_Lambda[:,index_m]):
                    best_fit_index_m = index_m
                    max_diff = diff
        if best_fit_index_m != -1:
            placed_file_indices.append(index_f)
            R_Lambda_M[:, best_fit_index_m] += unisolated_rates[:, index_f] * cache_vec[index_f]
            countable_m_rate[best_fit_index_m] += sum(cachable_rates[:, index_f] * cache_vec[index_f])
            R_Lambda[:, best_fit_index_m] += unisolated_rates[:, index_f]
            cache_usage[best_fit_index_m] += cache_vec[index_f]
            loc_vec[index_f] = best_fit_index_m
        else:
            break

    remaining_file_indices = [item for item in remaining_file_indices if item not in placed_file_indices]
    # next find the best-fit of total rate
    for index_f in remaining_file_indices:
        rate = rate_by_file[index_f]
        best_fit_index_m = -1
        max_diff = -float('Inf')
        for index_m in range(0, m):
            if sum(R_Lambda[:, index_m]) + rate < mu_vec[index_m]:
                diff = Lambda[index_m] - sum(R_Lambda[:, index_m])
                if diff > max_diff:
                    best_fit_index_m = index_m
                    max_diff = diff
        if best_fit_index_m != -1:
            placed_file_indices.append(index_f)
            R_Lambda[:, best_fit_index_m] += unisolated_rates[:, index_f]
            loc_vec[index_f] = best_fit_index_m
            # if there is sufficient cache space on the best-fit machine:
            if c_vec[best_fit_index_m] - cache_usage[best_fit_index_m] >= cache_vec[index_f]:
                R_Lambda_M[:, best_fit_index_m] += unisolated_rates[:, index_f] * cache_vec[index_f]
                cache_usage[best_fit_index_m] += cache_vec[index_f]
        else:
            break

    remaining_file_indices = [item for item in remaining_file_indices if item not in placed_file_indices]
    for index_f in remaining_file_indices:
        rate = rate_by_file[index_f]
        #  find the machine with the largeset available rate.

        available_rates = mu_vec - np.sum(R_Lambda, axis=0)
        best_fit_index_m = np.argmax(available_rates)
        if (available_rates[best_fit_index_m] < rate):
            print 'No machine can hold a file with access rate %s' % rate
            return False
        placed_file_indices.append(index_f)
        R_Lambda[:, best_fit_index_m] += unisolated_rates[:, index_f]
        loc_vec[index_f] = best_fit_index_m
        # if there is sufficient cache space on the best-fit machine:
        if c_vec[best_fit_index_m] - cache_usage[best_fit_index_m] >= cache_vec[index_f]:
            R_Lambda_M[:, best_fit_index_m] += unisolated_rates[:, index_f] * cache_vec[index_f]
            cache_usage[best_fit_index_m] += cache_vec[index_f]

    R_Lambda_D = R_Lambda - R_Lambda_M
    user_latencies_by_machine, user_latencies = user_latency(R_Lambda, R_Lambda_D, mu_vec, delta)

    #print 'la, user-machine latencies', user_latencies_by_machine, user_latencies
    #print 'la, user-machine total rates', R_Lambda
    #print 'la, user-machine disk rates', R_Lambda_D
    #print 'la, total memory rate', sum(sum(R_Lambda - R_Lambda_D))
    #print 'la, machine rate', np.sum(R_Lambda, axis=0)


    latency_la_rounded = (sum(np.multiply(np.array(mu_vec), np.reciprocal(mu_vec - np.sum(R_Lambda,axis = 0)))) - m + sum(sum(
        R_Lambda_D)) * delta) / sum(rate_by_file)
    return user_latencies, latency_la, latency_la_rounded, loc_vec, cache_vec
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





