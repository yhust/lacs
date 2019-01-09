from generate_microbench_rates import generate_microbench_rates
import numpy as np
from isolation import get_iso_latency
from lacs import lacs
from mm_default import mm_default


rate1 = 20.0
filenumber = 100
for rate2 in range(29,36):
    generate_microbench_rates(filenumber, rate1, rate2)


    bandwidth = 614
    machine_number = 10
    filesize = 100  # 10 GB
    cachesize = 400  # 5 GB
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
    print rates
    lacs(mu_vector, c_vector, rates, delta, user_si)

    mm_default(mu_vector, c_vector, rates, delta)
