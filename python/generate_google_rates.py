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

def generate_google_rates(k, n, zipf_factor):
    rates = np.zeros((k,n))
    k = (int)(k)
    fast_rate = 1.0/0.071648
    slow_rate = 1.0/7.429076

    for index_u in range(k/2):  # slow
        preference = np.random.permutation(n)
        for index_f in range(n):
            rates[index_u, index_f] = zipf.pmf(preference[index_f]+1, zipf_factor)
        # normalize
        rates[index_u,:] /= sum(rates[index_u,:])
        rates[index_u, :] *= slow_rate



    for index_u in np.arange(k/2, k):
        preference = np.random.permutation(n)
        for index_f in range(n):
            rates[index_u, index_f] = zipf.pmf(preference[index_f] + 1, zipf_factor)
        rates[index_u, :] /= sum(rates[index_u, :])
        rates[index_u, :] *= fast_rate

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
    zipf_factor = (float)(sys.argv[3])
    generate_google_rates(k, n,zipf_factor)
