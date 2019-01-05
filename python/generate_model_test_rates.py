'''
Mannually generate user access rates for testing.

'''

import numpy as np
from scipy.stats import zipf
import sys


'''
file_number 
zipf_factor: distribution

'''

def generate_model_test_rates(file_number, zipf_factor):

    preference = np.random.permutation(file_number)
    rates = np.zeros(file_number)
    for index_f in range(file_number):
        rates[index_f] = zipf.pmf(preference[index_f]+1, zipf_factor)
        # normalize
        rates /= sum(rates)

    log_rates(rates)

def log_rates(rates):
    f = open('pop.txt', 'w')
    f.write(','.join(np.array(map(str, rates))))
    f.write('\n')
    f.close()


if __name__ == "__main__":
    generate_model_test_rates(int(sys.argv[1]),float(sys.argv[2]))

