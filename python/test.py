
from scipy.stats import zipf
import numpy as np

fileNumber = 100
zipfFactor = 1.05
popularity = list()
for i in range(1, fileNumber+1 ,1):
    popularity.append(zipf.pmf(i, zipfFactor))
popularity /= sum(popularity)
#popularity = popularity[::-1]

count = list()
size = list()
for pop in popularity:
    this_count = max(min((int)(300*pop),30),1)
    count.append(this_count)
    size.append(100/this_count)
sum(popularity)
print size
