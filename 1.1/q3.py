path = "pagecounts-20141101-000000"

originCount = 0

# open file, real line, filter, store in map
file = open(path)
while 1:
    line = file.readline();
    if not line:
        break
    originCount += 1
file.close()

print originCount