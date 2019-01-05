from linear_relaxation import linear_relaxation
import numpy as np
from copy import copy


def rounding_opt(mu_vector, c_vector, rates, delta, cachable_users = False):
    k = len(rates[:, 1])  # user number
    n = len(rates[1, :])  # file number
    m = len(mu_vector)

    if isinstance(cachable_users, bool):
        cachable_users = range(k)

    rate_by_file = np.sum(rates, axis=0)
    cachable_rate_by_file = np.sum(rates[cachable_users,:], axis=0)
    Lambda, Lambda_D, latency_opt = linear_relaxation(mu_vector, c_vector, rate_by_file.copy(), delta, rates[cachable_users,:].copy())
    accuracy_check(Lambda, Lambda_D, latency_opt, mu_vector, c_vector, rate_by_file.copy(), delta)
    Lambda_M = Lambda - Lambda_D



    # rounding
    cache_usage = np.zeros(m)
    #sort_index = np.argsort(-(Lambda-Lambda_D)).tolist() # from the largest memory rate to the smallest
    #rate_by_file[::-1].sort()
    R_Lambda_M = np.zeros((k, m))  # per_user rate after rounding
    R_Lambda = np.zeros((k, m))  # per_user rate after rounding

    #remaining_file_indices = np.argsort(-cachable_rate_by_file)
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
            if sum(R_Lambda[:, index_m]) + rate < mu_vector[index_m]:
                diff = Lambda[index_m] - Lambda_D[index_m] - sum(R_Lambda_M[:, index_m])
                if diff > max_diff and c_vector[index_m] - cache_usage[index_m] >=1  and Lambda[index_m] >= sum(R_Lambda[:,index_m]):
                    best_fit_index_m = index_m
                    max_diff = diff
        if best_fit_index_m != -1:
            placed_file_indices.append(index_f)
            # cache_portial = min(1, c_vector[index_m] - cache_usage[index_m])
            cache_portial = 1
            R_Lambda_M[:, best_fit_index_m] += rates[:, index_f] * cache_portial
            R_Lambda[:, best_fit_index_m] += rates[:, index_f]
            cache_usage[best_fit_index_m] += cache_portial
        else:
            break

    remaining_file_indices = [item for item in remaining_file_indices if item not in placed_file_indices]
    # next find the best-fit of total rate
    for index_f in remaining_file_indices:
        rate = rate_by_file[index_f]
        best_fit_index_m = -1
        max_diff = -float('Inf')
        for index_m in range(0, m):
            if sum(R_Lambda[:, index_m]) + rate < mu_vector[index_m]:
                diff = Lambda[index_m] - sum(R_Lambda[:, index_m])
                if diff > max_diff:
                    best_fit_index_m = index_m
                    max_diff = diff
        if best_fit_index_m != -1:
            placed_file_indices.append(index_f)
            R_Lambda[:, best_fit_index_m] += rates[:, index_f]
            if c_vector[best_fit_index_m] - cache_usage[best_fit_index_m] >=1:
                #cache_portial = min(1, c_vector[index_m] - cache_usage[index_m])
                cache_portial = 1
                R_Lambda_M[:, best_fit_index_m] += rates[:, index_f] * cache_portial
                R_Lambda[:, best_fit_index_m] += rates[:, index_f]
                cache_usage[best_fit_index_m] += cache_portial
        else:
            break

    remaining_file_indices = [item for item in remaining_file_indices if item not in placed_file_indices]
    for index_f in remaining_file_indices:
        rate = rate_by_file[index_f]
        #  find the machine with the largeset available rate.
        available_rates = mu_vector - np.sum(R_Lambda, axis=0)
        best_fit_index_m = np.argmax(available_rates)
        if (available_rates[best_fit_index_m] < rate):
            print 'No machine can hold a file with access rate %s' % rate
            return False
        placed_file_indices.append(index_f)
        R_Lambda[:, best_fit_index_m] += rates[:, index_f]
        if c_vector[best_fit_index_m] - cache_usage[best_fit_index_m] >=1 :
            # cache_portial = min(1, c_vector[index_m] - cache_usage[index_m])
            cache_portial = 1
            R_Lambda_M[:, best_fit_index_m] += rates[:, index_f] * cache_portial
            R_Lambda[:, best_fit_index_m] += rates[:, index_f]
            cache_usage[best_fit_index_m] += cache_portial

    # for index_m in sort_index:
    #     memory_rate = 0
    #     usage = 0
    #     indice_to_put = list()
    #     for index_f in remaining_file_indices: #  from the file with largest cachable rate
    #
    #         if(usage >= c_vector[index_m]):
    #             break
    #         rate = cachable_rate_by_file[index_f]
    #         if( memory_rate + rate - Lambda_M[index_m] <= pow(10, -6)):
    #             memory_rate += rate
    #             R_Lambda_M[:, index_m] += rates[:, index_f]
    #             R_Lambda[:, index_m] += rates[:, index_f]
    #             indice_to_put.append(index_f)
    #             usage += 1
    #     remaining_file_indices = [item for item in remaining_file_indices if item not in indice_to_put]
    #     cache_usage[index_m] = usage
    #
    #     #rate_by_file_cp = np.delete(rate_by_file_cp,indice_to_put)
    #
    # # fill the unused cache space.
    # #for machine_id in range(0,m):
    #  #   if(cache_usage[machine_id]<c_vector[machine_id] and R_Lambda_M< mu_vector[machine_id]):
    #  #       diff = Lambda_M - R_Lambda_M
    #
    #
    #
    # R_Lambda = copy(R_Lambda_M)
    #
    # for index_f in remaining_file_indices:
    #     rate = rate_by_file[index_f]
    #     # Put the file on the machine with the largest gap of opt_rate - current_rate (could be negative)
    #     # todo: check other rounding schemes
    #     best_fit_machine_id = -1  # np.argmin(map(abs, np.add(R_Lambda, np.ones(m)*rate-Lambda)))
    #     max_diff = -float('Inf')
    #     for machine_id in range(0, m):
    #         if sum(R_Lambda[:,machine_id])+ rate < mu_vector[machine_id]:
    #             diff = Lambda[machine_id] - sum(R_Lambda[:, machine_id])
    #             if diff > max_diff:
    #                 best_fit_machine_id = machine_id
    #                 max_diff = diff
    #
    #     if best_fit_machine_id == -1:  # no machine can hold the file
    #         return False
    #
    #     R_Lambda[:, best_fit_machine_id] += rates[:, index_f]
    #     if c_vector[best_fit_machine_id] - cache_usage[best_fit_machine_id] > 0:
    #         this_usage = min(1, c_vector[best_fit_machine_id] - cache_usage[best_fit_machine_id])
    #         cache_usage[best_fit_machine_id] += this_usage
    #         R_Lambda_M[:, best_fit_machine_id] += rates[:, index_f] * this_usage

    '''
    # todo: should we first consider the machines with available cache space
    for index_f in remaining_file_indices:
        rate = rate_by_file[index_f]
        best_fit_machine_id = -1 #np.argmin(map(abs, np.add(R_Lambda, np.ones(m)*rate-Lambda)))
        min_diff = float('Inf')
        for machine_id in range(0,m):
            if R_Lambda[machine_id] + rate < mu_vector[machine_id]:
                diff = abs(R_Lambda[machine_id] + rate -Lambda[machine_id])
                if diff < min_diff:
                    best_fit_machine_id = machine_id
                    min_diff = diff

        if best_fit_machine_id == -1: # no machine can hold the file
            return False

        R_Lambda[best_fit_machine_id] += rate
        if cache_usage[best_fit_machine_id]< c_vector[best_fit_machine_id]:
            cache_usage[best_fit_machine_id] += 1
            R_Lambda_M[best_fit_machine_id] += rate
            cached_file_indices.append(index_f)

    '''
    # now calculate the average latency after rounding

    R_Lambda_D = R_Lambda - R_Lambda_M

    #print R_Lambda_M
    #print R_Lambda_D
    #print R_Lambda
    latency_rounded = (sum(np.multiply(np.array(mu_vector), np.reciprocal(mu_vector - np.sum(R_Lambda, axis=0)))) - m + sum(sum(
        R_Lambda_D)) * delta) / sum(rate_by_file)
    user_latencies_by_machine, user_latencies = user_latency(R_Lambda, R_Lambda_D, mu_vector, delta)
    #print R_Lambda, cache_usage, latency_rounded,cached_file_indices

    if latency_rounded < latency_opt:
        1

    return R_Lambda, user_latencies_by_machine, user_latencies, latency_opt, latency_rounded


def accuracy_check(Lambda, Lambda_D, latency_opt,mu_vector, c_vector, rate_by_file, delta):
    # The delay calculation
    n = len(rate_by_file) #file number
    m = len(mu_vector) # machine number
    if((mu_vector <= Lambda).any()):
        print 'unstable'
    delay = 0
    for index_m in range(m):
        delay += (Lambda[index_m])/ (mu_vector[index_m] - Lambda[index_m]) + Lambda_D[index_m]*delta
    if(delay/sum(rate_by_file) - latency_opt > pow(10,-6)):
        print 'latency wrong'

    max_memory_rate_vec = np.zeros(m)
    rate_by_file[::-1].sort()
    max_memory_rate = sum(rate_by_file[0:sum(c_vector)])
    for index in range(0, m):
        max_memory_rate_vec[index] = sum(rate_by_file[0:c_vector[index]])

    if((Lambda - Lambda_D-max_memory_rate_vec>pow(10,-6)).any()):
        print 'per-machine memory rate too large'
    if (sum(Lambda - Lambda_D) - max_memory_rate > pow(10, -6)):
        print 'total memory rate too large'

    #print 'Optimal solution accuracy check done!'
    #print 'total rate', np.sum(Lambda)
    #print 'machine rate (memory, disk)'
    #for index_m in range(m):
    #    print Lambda[index_m]-Lambda_D[index_m], max_memory_rate_vec[index_m]
    #    print Lambda_D[index_m]
    #print 'total memory rate', np.sum(Lambda - Lambda_D), max_memory_rate


# get the latency of each user
def user_latency(R_Lambda, R_Lambda_D, mu_vector,delta):
    k = len(R_Lambda[:,0])
    m = len(R_Lambda[0,:])
    user_latencies= np.zeros(k)
    user_latencies_by_machine = np.zeros((k,m))
    rate_by_user = np.sum(R_Lambda, axis = 1)
    for index_u in range(k):
        for index_m in range(m):
            user_latencies_by_machine[index_u, index_m] = R_Lambda[index_u, index_m] / (mu_vector[index_m] - sum(R_Lambda[:,index_m])) + R_Lambda_D[index_u,index_m] * delta
            user_latencies[index_u] += R_Lambda[index_u,index_m] / (mu_vector[index_m] - sum(R_Lambda[:,index_m])) + R_Lambda_D[index_u,index_m] * delta
        user_latencies[index_u] /=  rate_by_user[index_u]

    # check accuracy:
    #print np.dot(user_latencies, rate_by_user)/sum(rate_by_user)
    return user_latencies_by_machine, user_latencies


if __name__ == "__main__":
    n = 10
    m = 5
    # mu_vector = np.ones((m,1)) * 10
    mu_vector = np.array([6, 8, 10, 12, 14])
    # c_vector = np.ones((m,1)) * 1
    c_vector = np.array([1, 1, 2, 1, 1])
    # rate_by_file = np.ones((n,1)) * 2
    rate_by_file = np.array([0.5, 0.5, 1, 1, 1.5, 1.5, 2, 2, 2.5, 2.5])
    delta = 0.5

    rounding_opt(mu_vector, c_vector, rate_by_file, delta)