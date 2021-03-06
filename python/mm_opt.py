from max_min_allocator import max_min_allocator
import numpy as np
from cvxpy import *
from math import fabs

def mm_opt(mu_vec, c_vec, rates, delta):
    k = len(rates[:, 1])  # user number
    n = len(rates[1, :])  # file number
    m = len(c_vec)  # machine number

    loc_vec = np.zeros(n)
    cache_vec = max_min_allocator(rates.copy(), sum(c_vec))
    memory_rates = np.dot(rates,cache_vec)
    #print 'Ideal memory rate with mm', memory_rates
    rate_by_file = np.sum(rates, axis=0)
    sorted_rate_by_file = np.copy(rate_by_file)
    sorted_rate_by_file[::-1].sort()
    total_memory_rate = np.dot(cache_vec, rate_by_file)
    max_memory_rate_by_machine = np.zeros(m)
    for index_m in range(m):
        max_memory_rate_by_machine[index_m] = sum(sorted_rate_by_file[0:c_vec[index_m]])  #  todo: more precise bounding.

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
            # print A.value
            # print D.value
            # print Lambda.value
            # print Lambda_D.value
            # print result
            # return np.squeeze(np.asarray(Lambda.value)), np.squeeze(np.asarray(Lambda_D.value)),
            latency_mm = result
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
    # We first consider the machine with high memory rates

    R_Lambda_M = np.zeros((k,m))  # per_user rate after rounding
    R_Lambda = np.zeros((k,m))  # per_user rate after rounding
    cache_usage = np.zeros(m)
    sorted_indices_m = np.argsort(-(Lambda - Lambda_D)).tolist()  # from the largest memory rate to the smallest
    memory_rate_by_file = rate_by_file * cache_vec
    sorted_memory_rate_indices = np.argsort(-memory_rate_by_file).tolist()

    #remaining_file_indices = np.copy(sorted_memory_rate_indices)
    sorted_rate_indices = np.argsort(-rate_by_file).tolist()

    remaining_file_indices = np.copy(sorted_rate_indices)

    placed_file_indices = list()
    for index_f in remaining_file_indices:
        rate = rate_by_file[index_f]
        # Put the file on the machine with the largest gap of opt_mem_rate - current_mem_rate (could be negative)
        best_fit_index_m = -1  # np.argmin(map(abs, np.add(R_Lambda, np.ones(m)*rate-Lambda)))
        max_diff = -float('Inf')

        # first find the best-fit of memory-rate
        for index_m in range(0, m):
            if sum(R_Lambda[:, index_m]) + rate < mu_vec[index_m]:
                diff = Lambda[index_m] - Lambda_D[index_m] - sum(R_Lambda_M[:,index_m])
                if diff > max_diff and c_vec[index_m] - cache_usage[index_m] >= cache_vec[index_f] and Lambda[index_m] >= sum(R_Lambda[:,index_m]):
                    best_fit_index_m = index_m
                    max_diff = diff
        if best_fit_index_m != -1:
            placed_file_indices.append(index_f)
            R_Lambda_M[:, best_fit_index_m] += rates[:, index_f] * cache_vec[index_f]
            R_Lambda[:, best_fit_index_m] += rates[:, index_f]
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
            R_Lambda[:, best_fit_index_m] += rates[:, index_f]
            loc_vec[index_f] = best_fit_index_m
            if c_vec[best_fit_index_m] - cache_usage[best_fit_index_m] >= cache_vec[index_f]:
                R_Lambda_M[:, best_fit_index_m] += rates[:, index_f] * cache_vec[index_f]
                cache_usage[best_fit_index_m] += cache_vec[index_f]
        else:
            break

    remaining_file_indices = [item for item in remaining_file_indices if item not in placed_file_indices]
    for index_f in remaining_file_indices:
        rate = rate_by_file[index_f]
        #  find the machine with the largeset available rate.

        available_rates = mu_vec - np.sum(R_Lambda, axis = 0)
        best_fit_index_m = np.argmax(available_rates)
        if(available_rates[best_fit_index_m]<0):
            print 'No machine can hold a file with access rate %s' % rate
            return False
        placed_file_indices.append(index_f)
        R_Lambda[:, best_fit_index_m] += rates[:, index_f]
        loc_vec[index_f] = best_fit_index_m
        if c_vec[best_fit_index_m] - cache_usage[best_fit_index_m] >= cache_vec[index_f]:
            R_Lambda_M[:, best_fit_index_m] += rates[:, index_f] * cache_vec[index_f]
            cache_usage[best_fit_index_m] += cache_vec[index_f]

    # now calculate the average latency after rounding

    R_Lambda_D = R_Lambda - R_Lambda_M
    user_latencies_by_machine, user_latencies = user_latency(R_Lambda, R_Lambda_D, mu_vec, delta)
    #print 'mm, user-machine total rate', R_Lambda
    #print 'mm, user-machine memory rate', R_Lambda_M
    #print 'mm, user-machine latencies', user_latencies_by_machine, user_latencies
    #print 'mm, user-machine total rates', R_Lambda
    #print 'mm, user-machine disk rates', R_Lambda_D

    #print 'mm, user total rate', np.sum(R_Lambda,axis=1)
    #print 'mm, user memory rate',np.sum(R_Lambda - R_Lambda_D,axis=1), 'user disk rate', np.sum(R_Lambda_D,axis=1)
    #print 'mm, total memory rate', sum(sum(R_Lambda - R_Lambda_D))
    #print 'mm, machine rate', np.sum(R_Lambda,axis = 0)


    latency_mm_rounded = (sum(np.multiply(np.array(mu_vec), np.reciprocal(mu_vec - np.sum(R_Lambda,axis = 0)))) - m + sum(sum(
        R_Lambda_D)) * delta) / sum(rate_by_file)
    if latency_mm_rounded >= 50 * latency_mm: # debug
        1


    # log the loc_vec, cache_vec and block_list in alloc.txt
    f = open('alloc.txt','w')
    f.write(','.join(str(loc) for loc in loc_vec))
    f.write("\n")
    f.write(','.join(str(ratio) for ratio in cache_vec))
    f.write("\n")
    f.write("\n")
    f.close()

    # for debug
    f = open('alloc_mm_opt.txt','w')
    f.write(','.join(str(loc) for loc in loc_vec))
    f.write("\n")
    f.write(','.join(str(ratio) for ratio in cache_vec))
    f.write("\n")
    f.write("\n")
    f.close()



    return user_latencies, latency_mm, latency_mm_rounded
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


