path = "output"

# open file, read line
file = open(path)
count = 0

while 1:
    line = file.readline();
    if not line:
        break
    split = line.strip().split("\t")
    if int(split[1]) > 10000:
    	count += 1
file.close()

print count