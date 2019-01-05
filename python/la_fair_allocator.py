# Only consider the cache allocation
# Max-min fairness in terms of cache hit ratio
#

from cvxpy import *
#from tests import generate_rates # for testing
import numpy as np


def la_fair_allocator(rates, cache_budget): #rates k x n; cache_budget, scalar
    k = len(rates[:, 0])  # user number
    n = len(rates[0, :])  # file number

    # normalize rate
    for index_u in range(k):
        rates[index_u, :] /= sum(rates[index_u,:])
    # sharing incentive hit ratios.
    si_hit_rates = np.zeros(k)
    for index_u in range(k):
        this_user_rates = rates[index_u, :].copy()
        this_user_rates[::-1].sort()
        si_hit_rates[index_u] = sum(this_user_rates[0:int(cache_budget/k)])   #todo: fractional cache budget


    # Construct the problem.
    x = Variable(n,1)  # allocation vector
    min = Variable(1)   # minimum cache hit ratio
    objective = Maximize(sum_entries(rates * x))
    constraints = [0 <= x, x <= 1, sum_entries(x) <= cache_budget, rates * x >= si_hit_rates]
    prob = Problem(objective, constraints)
    try:
        prob.solve()
        ##print "Optimal value", result

        # print "Preference Matrix"
        # print PMatrix

        # print "Optimal var"

        if prob.status == 'optimal':
            #print x.value
            return np.squeeze(np.array(x.value))
        else:
            return False
    except:
        return False


if __name__ == "__main__":
    k = 2
    n = 3
    cache_budget = 2
    # rates = generate_rates(k,n)
