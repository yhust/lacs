from generate_rates import generate_rates
import numpy as np
from isolation import get_iso_latency
from lacs import lacs


rate1 = 4.0
filenumber = 500
factor = 1.5
repeat = 50
for filenumber in np.arange(300,1600,100):
    print filenumber
    for i in range(repeat):
        generate_rates(filenumber, rate1, factor)
        bandwidth = 614
        machine_number = 30
        filesize = 100  # 30 GB - 150 GB
        cachesize = 2000  # 60 GB
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

