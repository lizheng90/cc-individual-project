path = "output"

count = 0

# open file, read line, count
file = open(path)
while 1:
	line = file.readline()
	if not line:
		break
	count += 1
file.close()

print count