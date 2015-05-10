path = '/Users/Jackyliz/Desktop/links'
outpath = '/Users/Jackyliz/Desktop/output'

file = open(path)
outfile = open(outpath, 'w')

# while 1:
# 	line = file.readline()
# 	if not line:
# 		break
# 	replace = line.replace(',','\t')
# 	outfile.write(replace)

# tmp = ''

# while 1:
# 	line = file.readline()
# 	if not line:
# 		break
# 	uid = line.split(',')
# 	if tmp != '':
# 		uid[1] = tmp + uid[1]
# 	while 1:
# 		nextLine = file.readline()
# 		if not nextLine:
# 			break
# 		next = nextLine.split(',')
# 		if next[0] == uid[0]:
# 			uid[1] += next[1] 
# 		else:
# 			tmp = next[1]
# 			break
# 	outfile.write(uid[0] + '\t' + uid[1])

d = {}

while 1:
	line = file.readline()
	if not line:
		break
	split = line.split(',')
	if split[0] not in d:
		d[split[0]] = split[1]
	else:
		d[split[0]] += split[1]

for key, value in d.iteritems():
	value = value.replace('\n', ' ')
	value = value[:-1]
	value += '\n'
	outfile.write(key + '\t' + value)







