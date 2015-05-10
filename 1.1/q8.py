path = "output"

# open file, read line
file = open(path)

while 1:
    line = file.readline();
    if not line:
        break
    line = line.strip()
    split = line.split("\t")
    if "(film)" in split[0]:
    	realLine = line
file.close()

realLine = realLine.strip()

print realLine.split("\t")[1]