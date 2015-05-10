path = "pagecounts-20141101-000000"

requestCount = 0

# open file, real line, filter, store in map
file = open(path)
while 1:
    line = file.readline();
    if not line:
        break
    line = line.strip()
    split = line.split(" ")
    requestCount += int(split[2])
file.close()

print requestCount