'''
Mannually generate user access rates for testing.

'''

import numpy as np
from scipy.stats import zipf
import sys
import os


'''
k: number of users
n: number of files
access_rate_slow: access rate of user 1 
access_rate_fast: access rate of user 2 
'''

def generate_microbench_rates(n, rate_slow,rate_fast):
    # k = 2
    rates = np.zeros((2, n))


    for index_f in range(0,n/2):
        rates[0,index_f] = 1.0/(n/2) * rate_slow
        #rates[1,index_f] = 1.0/(n/2) * rate_slow
    #print sum(rates[0,:])


    for index_f in range(n/2,n):
        rates[1, index_f] = 1.0/(n/2) * rate_fast
        #rates[3, index_f] = 1.0/(n/2) * rate_fast
    #print sum(rates[1,:])

    log_rates(2, rates)
    return rates

def log_rates(k, rates):

    print os.getcwd()
    f = open('pop.txt', 'w')
    for index_u in range(k):
        f.write(','.join(np.array(map(str, rates[index_u,:]))))
        f.write('\n')
    f.close()


if __name__ == "__main__":
    #generate_rates(2, 4, 1, 1.05, 2)
    n = (int)(sys.argv[1])
    rate1 = (float)(sys.argv[2])
    rate2 = (float) (sys.argv[3])
    generate_microbench_rates(n, rate1, rate2)
