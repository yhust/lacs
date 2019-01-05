'''
Mannually generate user access rates for testing.

'''

import numpy as np
from scipy.stats import zipf
import sys


'''
k: number of users
n: number of files
arrival_rate: request rate of normal users(as Poisson)
zipf_factor: distribution
factor: ratio of request rates (aggressive to normal)

'''

def generate_rates(k, n, arrival_rate, zipf_factor, factor):
    rates = np.zeros((k,n))
    k = (int)(k)
    # for index_u in range(0, k / 2):  # slow
    #     for index_f in range(0, n):
    #         # self.prference[i] = np.random.random(1)
    #         rates[index_u,index_f] = np.random.exponential(Lambda)
    # Lambda *= factor
    # for index_u in range(k/2, k):  # slow
    #     for index_f in range(0, n):
    #         rates[index_u, index_f] = np.random.exponential(Lambda)

    # for index_u in range(0, k/3): #  slow
    #     for index_f in range(0, n):
    #         rates[index_u,index_f] = np.random.exponential(Lambda)
    # Lambda *= factor
    # for index_u in range(k/3 ,k*2/3):  #  median
    #     for index_f in range(0, n):
    #         rates[index_u, index_f] = np.random.exponential(Lambda)
    #
    # Lambda *= factor                    # fast
    # for index_u in range(k *2/ 3, k):  # half fast
    #     for index_f in range(0, n):
    #         rates[index_u, index_f] = np.random.exponential(Lambda)

    # for motivation exp, only one user are fast
    for index_u in range(k/3):  # slow
        preference = np.random.permutation(n)
        for index_f in range(n):
            rates[index_u, index_f] = zipf.pmf(preference[index_f]+1, zipf_factor)
        # normalize
        rates[index_u,:] /= sum(rates[index_u,:])
        rates[index_u, :] *= arrival_rate

    arrival_rate *= factor

    for index_u in np.arange(k/3, 2*k/3):
        preference = np.random.permutation(n)
        for index_f in range(n):
            rates[index_u, index_f] = zipf.pmf(preference[index_f] + 1, zipf_factor)
        rates[index_u, :] /= sum(rates[index_u, :])
        rates[index_u, :] *= arrival_rate

    arrival_rate*= factor

    for index_u in np.arange(k*2/3, k):
        preference = np.random.permutation(n)
        for index_f in range(n):
            rates[index_u, index_f] = zipf.pmf(preference[index_f] + 1, zipf_factor)
        rates[index_u, :] /= sum(rates[index_u, :])
        rates[index_u, :] *= arrival_rate

    log_rates(k, rates)
    return rates

def log_rates(k, rates):
    f = open('pop.txt', 'w')
    for index_u in range(k):
        f.write(','.join(np.array(map(str, rates[index_u,:]))))
        f.write('\n')
    f.close()


if __name__ == "__main__":
    #generate_rates(2, 4, 1, 1.05, 2)
    k = (int)(sys.argv[1])
    n = (int)(sys.argv[2])
    arrival_rate = (float)(sys.argv[3])
    zipf_factor = (float)(sys.argv[4])
    factor = (float)(sys.argv[5])
    generate_rates(k, n, arrival_rate, zipf_factor, factor)
