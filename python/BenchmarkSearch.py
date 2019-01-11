from generate_rates import generate_rates
import numpy as np
from isolation import get_iso_latency
from lacs import lacs
from mm_default import mm_default
from isolation import get_iso_allocation


rate1 = 4.0
filenumber = 500
factor_share = 1.5
factor_isolate= 2.4
for factor in np.arange(1.5,1.6,0.1):
    factor = factor_isolate
    print factor
    generate_rates(filenumber, rate1, factor)


    bandwidth = 614
    machine_number = 30
    filesize = 100  # 50 GB
    cachesize = 1000  # 15 GB
    delta = 0.1

    # read the rates from pop.txt
    with open("pop.txt", "r") as f:
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
    #print rates
    lacs(mu_vector, c_vector, rates, delta, user_si,0)

    mm_default(mu_vector, c_vector, rates, delta,0)
    #get_iso_allocation(mu_vector, c_vector, rates, delta, 0)
