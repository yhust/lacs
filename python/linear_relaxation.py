from cvxpy import *
import numpy as np
import sys


# mu_vector: processing rate of machines
#c_vector: cache space of machines (in units)
#rate_vector: access rate of files
#delta: disk I/O delay
# cachable_rates: for la-fair algorithm, excluding the users with high rates.

def linear_relaxation(mu_vector, c_vector, rate_vector, delta, cachable_rates = False):

    m = len(mu_vector) # machine number
    n = len(rate_vector) # file number

    # Construct the problem.
    A = Variable(n, m) # cache matrix
    D = Variable(n, m) # on-disk matrix
    Lambda = Variable(m) # total rate to machines
    Lambda_D = Variable(m) # total rate to disks of machines

    max_memory_rate_vec= np.zeros(m)
    rate_vector[::-1].sort()
    if not isinstance(cachable_rates, bool):
        cachable_rate_vector = np.sum(cachable_rates, axis = 0)
        cachable_rate_vector[::-1].sort()
        max_memory_rate = sum(cachable_rate_vector[0:int(sum(c_vector))])
        for index in range(0, m):
            max_memory_rate_vec[index] = sum(cachable_rate_vector[0:int(c_vector[index])])
    else:
        max_memory_rate = np.sum(rate_vector[0:int(sum(c_vector))])
        for index in range(0,m):
            max_memory_rate_vec[index] = sum(rate_vector[0:int(c_vector[index])])



    #func =0

    #for index_m in range(0, m):
    #    func = func + Lambda[index_m] / (mu_vector[index_m] - Lambda[index_m]) + Lambda_D[index_m]*delta
    #func = func * 1.0 / sum(rate_vector)

    func = (mu_vector.T * inv_pos(mu_vector-Lambda) - m + sum_entries(Lambda_D) * delta)/ sum(rate_vector)

    objective = Minimize(func)
    #constraints = [0 <= A, A <= 1, 0<=D, D<= 1, Lambda< mu_vector, A.T*np.ones((n,1))<= c_vector, (A+D) * np.ones((m,1)) == np.ones((n,1)), Lambda ==(A+D).T*rate_vector, Lambda_D == D.T*rate_vector]
    constraints = [Lambda< mu_vector, Lambda_D<=Lambda, 0<= Lambda_D, Lambda - Lambda_D <= max_memory_rate_vec,sum_entries(Lambda - Lambda_D)<= max_memory_rate, sum_entries(Lambda) == sum(rate_vector)]

    prob = Problem(objective, constraints)
    try:
        result = prob.solve()
        ##print "Optimal value", result

        #print "Preference Matrix"
        #print PMatrix

        #print "Optimal var"
        #print x.value
        if prob.status == 'optimal' or prob.status == 'optimal_inaccurate':
            Lambda = np.squeeze(np.asarray(Lambda.value))
            Lambda_D = np.squeeze(np.asarray(Lambda_D.value))
            Lambda_M = Lambda - Lambda_D
            if Lambda.size == 1:  # only one machine
                Lambda = np.array([Lambda])
                Lambda_D = np.array([Lambda_D])
                Lambda_M = np.array([Lambda_M])
            #print A.value
            #print D.value
            #print Lambda.value
            #print Lambda_D.value
            #print result
            return Lambda, Lambda_D, result
        else:
            return False, False, float('Inf')
    except:
        return False, False, float('Inf')

if __name__ == "__main__":
    n = 10
    m = 5
    #mu_vector = np.ones((m,1)) * 10
    mu_vector = np.array([6, 8, 10, 12, 14])
    #c_vector = np.ones((m,1)) * 1
    c_vector = np.array([1, 1, 2, 1, 1])
    #rate_vector = np.ones((n,1)) * 2
    rate_vector =np.array([0.5,0.5,1,1,1.5,1.5,2,2,2.5,2.5])
    delta = 2

    linear_relaxation(mu_vector, c_vector, rate_vector, delta)
