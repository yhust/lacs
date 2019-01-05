import numpy as np
from lacs import lacs
from mm_default import mm_default
from isolation import get_iso_allocation
from generate_rates import generate_rates
from datetime import datetime

def measure():
    n = 400 #file
    m = 10 #machine
    k = 30 #user
    zipf_factor = 1.05
    delta = 0.216
    mu_vec = np.ones(m) * 1.2
    c_vec = np.ones(m) * 20
    arrival_rate = 0.1
    factor = 2
    #for arrival_rate in np.arange(0.005, 0.03, 0.002):

    log_overhead = open("logs/overhead.txt", 'w')
    for n in np.arange(1900, 2100, 100):
        #factor =7


        for repeat in range(100):
            rates = generate_rates(k, n, arrival_rate, zipf_factor, factor) # factor for sharing incentive tests
            avg_iso, user_iso = get_iso_allocation(mu_vec, c_vec, rates.copy(),delta)
            if(sum(sum(rates)) >= sum(mu_vec)):# infeasible
                print 'infeasible:  total rate exceeds the total processing rate'
                continue
            start = datetime.now()
            avg_lacs, user_lacs  = lacs(mu_vec, c_vec, rates.copy(), delta, user_iso.copy())
            end = datetime.now()
            log_overhead.write("%s\t" % int((end-start).total_seconds() * 1000))
        log_overhead.write("\n")
    log_overhead.close()
if __name__ == "__main__":
    measure()
