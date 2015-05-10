#!/usr/bin/python
#andrew id:zli1 Name:ZhengLi

import sys

output = [0 for x in xrange(0,30)]
november = ['20141101', '20141102', '20141103', '20141104', '20141105', '20141106', '20141107', '20141108', '20141109', '20141110', '20141111', '20141112', '20141113', '20141114', '20141115', '20141116', '20141117', '20141118', '20141119', '20141120', '20141121', '20141122', '20141123', '20141124', '20141125', '20141126', '20141127', '20141128', '20141129', '20141130']

current_word = None # store previous word
word = None
current_date = None # store previous date
date = None
tmp = ""

current_count = 0
dateIndex = 0
i = 0


for line in sys.stdin:
	line = line.strip()
	split = line.split('\t')
	word = split[0]
	date = split[1]
	count = split[2]

	try:
		count = int(count)
	except ValueError:
		continue

	# if same word with previous
	if current_word == word:
		current_count += count
		current_date = date
		dateIndex = int(date[6] + date[7])
		output[dateIndex - 1] += count

	# word not same word
	else:
		# if not first word
		if current_word:
			if current_count > 100000:
				while i < 30:
					tmp = tmp + '\t' + november[i] + ':' + str(output[i])
					i += 1
				# empty i
				i = 0
				print (str(current_count) + '\t' + current_word + tmp)
				# empty tmp
				tmp = ""
			output = [0 for x in xrange(0,30)]
		# set word as previous word
		current_count = count
		current_word  = word
		current_date = date
		# record first count of first date
		dateIndex = int(date[6] + date[7])
		output[dateIndex - 1] += count

if current_word == word:
	if current_count > 100000:
		while i < 30:
			tmp = tmp + '\t' + november[i] + ':' + str(output[i])
			i += 1
		i = 0
		print (str(current_count) + '\t' + current_word + tmp)
		tmp = ""
		output = [0 for x in xrange(0,30)]


 