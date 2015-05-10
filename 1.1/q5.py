path = "output"

# open file, read line
file = open(path)

while 1:
    line = file.readline();
    if not line:
        break
    realLine = line
file.close()

realLine = realLine.strip()
split = realLine.split("\t")

print split[0]