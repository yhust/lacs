'''
Mannually generate user access rates for testing.

'''

import numpy as np
from scipy.stats import zipf
import sys


'''
k: number of users
n: number of files
access_rate_1: access rate of user 1
access_rate_2: access rate of user 2
'''

def generate_rates(n, rate_1,rate_2):
    # k = 2
    rates = np.zeros((2, n))

    rates[0,:] = np.ones((1,n)) / n * rate_1
    rates[1,:] = np.ones((1,n)) / n * rate_2


    log_rates(2, rates)
    return rates

def log_rates(k, rates):
    f = open('python/pop.txt', 'w')
    for index_u in range(k):
        f.write(','.join(np.array(map(str, rates[index_u,:]))))
        f.write('\n')
    f.close()


if __name__ == "__main__":
    #generate_rates(2, 4, 1, 1.05, 2)
    n = (int)(sys.argv[1])
    rate1 = (float)(sys.argv[2])
    rate2 = (float) (sys.argv[3])
    generate_rates(n, rate1, rate2)
