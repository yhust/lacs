import numpy as np

# read the rates from pop.txt
with open("pop.txt", "r") as f:
    lines = f.readlines()
    user_number = len(lines)
    file_number = len(lines[0].split(','))
    rates = np.zeros((user_number,file_number))
    for index_u in range(user_number):
        line = lines[index_u]
        rates[index_u,:]= np.asfarray(np.array(line.split(',')), np.float)

k = len(rates[:,0])
print k
n = len(rates[0,:])
for index_u in range(k):
    rate = rates[index_u,:].copy()
    rate = np.sort(rate)
    print sum(rate[n-14+1:n]) / sum(rate)


c = 20*10/15
for index_u in range(k):
    rate = rates[index_u,:].copy()
    rate = np.sort(rate)

    #print sum(rate[n-14+1:n]) / sum(rate)
    count = 500
    hit =0
    for i in range(0, count):
        # get a file id from the popularity
        file_id = np.random.choice(np.arange(0, n), p=rate/sum(rate))
        if(file_id >= n-c+1):
            hit+=1
    print hit*1.0/count
