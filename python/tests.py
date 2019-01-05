import numpy as np
from lacs import lacs
from mm_default import mm_default
from isolation import get_iso_allocation
from generate_rates import generate_rates
from generate_google_rates import generate_google_rates

def tests():
    n = 4000 #file
    m = 10 #machine
    k = 10 #user
    zipf_factor = 1.05
    delta = 0.216
    mu_vec = np.ones(m) * 100
    c_vec = np.ones(m) * n / 20
    # arrival_rate = 0.1

    # n = 400 #file
    # m = 10 #machine
    # k = 15 #user
    # zipf_factor = 1.05
    # delta = 0.216
    # mu_vec = np.ones(m) * 0.9
    # c_vec = np.ones(m) * n / 20
    # arrival_rate = 0.2

    log_avg_latency_mm = open("logs/avg_latency_mm.txt", 'w')
    log_avg_latency_lacs = open("logs/avg_latency_lacs.txt", 'w')
    log_avg_latency_iso = open("logs/avg_latency_si.txt", 'w')


    log_user_latency_iso = open("logs/user_latency_iso.txt", 'w')
    log_user_latency_mm = open("logs/user_latency_mm.txt", 'w')
    log_user_latency_lacs = open("logs/user_latency_lacs.txt", 'w')

    log_violation_mm = open("logs/violation_mm.txt", 'w')
    log_violation_lacs = open("logs/violation_lacs.txt", 'w')




    factor = 4
    #for arrival_rate in np.arange(0.005, 0.03, 0.002):
    #for factor in np.arange(2, 3.5, 0.1):
    for k in np.arange(6,18,2):
        #factor =7
        print 'k:', k
        for repeat in range(1):
            #rates = generate_rates(k, n, arrival_rate, zipf_factor, factor) # factor for sharing incentive tests
            rates = generate_google_rates(k,n,zipf_factor)
            if(sum(sum(rates)) >= sum(mu_vec)):# infeasible
                print 'infeasible:  total rate exceeds the total processing rate'
                continue

            # try:

            avg_iso, user_iso = get_iso_allocation(mu_vec, c_vec, rates.copy(),delta)
            avg_lacs, user_lacs  = lacs(mu_vec, c_vec, rates.copy(), delta, user_iso.copy())
            avg_mm, user_mm = mm_default(mu_vec, c_vec, rates.copy(), delta)


            if (user_iso < user_mm).any():
                1
            if (user_iso < avg_lacs).any():
                1

            if avg_iso == float('Inf'):
                log_avg_latency_iso.write("%s\t" % (-1))
            else:
                log_avg_latency_iso.write("%s\t" % avg_iso)
            log_avg_latency_lacs.write("%s\t" %avg_lacs )
            log_avg_latency_mm.write("%s\t" % avg_mm)


            for item in user_iso:
                if item == float('Inf'):
                    log_user_latency_iso.write("%s \t" % (-1))
                else:
                    log_user_latency_iso.write("%s \t" % item)
            for item in user_mm:
                if item == float('Inf'):
                    log_user_latency_mm.write("%s \t" % (-1))
                else:
                    log_user_latency_mm.write("%s \t" % item)

            for item in user_lacs:
                if item == float('Inf'):
                    log_user_latency_lacs.write("%s \t" % (-1))
                else:
                    log_user_latency_lacs.write("%s \t" % item)


            log_violation_mm.write("%s\t" % np.count_nonzero(user_iso < user_mm))
            log_violation_lacs.write("%s\t" % np.count_nonzero(user_iso < user_lacs))


            # except Exception as e:
            #     print 'Failed: ' + str(e)
            #     continue


        # heterogeneous machines
        log_avg_latency_iso.write("\n")
        log_avg_latency_mm.write("\n")
        log_avg_latency_lacs.write("\n")
        log_user_latency_iso.write("\n")
        log_user_latency_mm.write("\n")
        log_user_latency_lacs.write("\n")
        log_violation_mm.write("\n")
        log_violation_lacs.write("\n")
if __name__ == "__main__":
    tests()
