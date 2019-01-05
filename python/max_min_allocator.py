# Only consider the cache allocation
# Max-min fairness in terms of cache hit ratio
#

from cvxpy import *
#from tests import generate_rates # for testing
import numpy as np


def max_min_allocator(rates, cache_budget): #rates k x n; cache_budget, scalar
    k = len(rates[:, 1])  # user number
    n = len(rates[1, :])  # file number

    #print rates
    # normalize rate
    for index_u in range(k):
        rates[index_u, :] = rates[index_u, :]/np.sum(rates[index_u, :])

    # Construct the problem.
    x = Variable(n,1)  # allocation vector
    min = Variable(1)   # minimum cache hit ratio
    objective = Maximize(min)
    constraints = [0 <= x, x <= 1, sum_entries(x) <= cache_budget, rates * x >= min* np.ones((k,1))]
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
    except Exception as e:
        result = prob.solve(solver=SCS)
        return False


if __name__ == "__main__":
    k = 2
    n = 3
    cache_budget = 2
    #rates = generate_rates(k,n)
    #max_min_allocator(rates, cache_budget)
