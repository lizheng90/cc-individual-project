#!/usr/bin/python

from operator import itemgetter

# open file, read line, store in dict
d = {}

file = open("million_songs_metadata_and_sales.csv")

while 1:
        line = file.readline()
        if not line:
                break
        line = line.strip()
        split = line.split(',')
        if(split[8] in d):
                d[split[8]] += int(split[2])
        else:
                d[split[8]] = int(split[2])

res = sorted(d.items(), key = itemgetter(1), reverse = True)

print res[0][0]

#!/usr/bin/python

from operator import itemgetter

# open file, read line, store in dict
d = {}
name = {}

file = open("million_songs_metadata_and_sales.csv")

while 1:
        line = file.readline()
        if not line:
                break
        line = line.strip().lower()
        split = line.split(',')
        if(split[8] in d):
                d[split[8]] += int(split[2])
        else:
                d[split[8]] = int(split[2])

res = sorted(d.items(), key = itemgetter(1), reverse = True)

print res[0][0]