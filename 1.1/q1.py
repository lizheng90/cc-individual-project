import re
from operator import itemgetter

# declare input and output path
path = "/Users/Jackyliz/Desktop/cc project1/test"
outpath = "/Users/Jackyliz/Desktop/cc project1/output"

# define map, and filter condition
d = {}
name = "en"
words = ["Media", "Special", "Talk", "User", "User_talk", "Project",
         "Project_talk", "File", "File_talk", "MediaWiki", "MediaWiki_talk",
         "Template", "Template_talk", "Help", "Help_talk", "Category", "Category_talk",
         "Portal", "Wikipedia", "Wikipedia_talk"]
lowerCase = "[a-z]+"
suffix = ["jpg", "gif", "png", "JPG", "GIF", "PNG", "txt", "ico"]
extraWords = ["404_error/", "Main_Page", "Hypertext_Transfer_Protocol", "Search"]

# open file, real line, filter, store in map
file = open(path)
while 1:
    line = file.readline();
    if not line:
        break
    line = line.strip()
    split = line.split(" ")
    title = split[1].split(":")
    tail = split[1].split(".")
    if not split[0] == name or title[0] in words or tail[-1] in suffix or re.match(lowerCase, split[1][0]) or split[1] in extraWords:
        continue
    else:
        d[line] = int(split[2])
file.close()

# sort
res = sorted(d.items(), key = itemgetter(1))

# write sorted results to output file
outfile = open(outpath, 'w')
for key, value in res:
    line = key.split(" ")
    outfile.write(line[1] + '\t' + line[2] + '\n')
outfile.close()
