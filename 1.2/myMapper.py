#!/usr/bin/python
#andrew id: zli1 Name:ZhengLi

import os
import re
import sys

fileName = None

name = "en"
words = ["Media", "Special", "Talk", "User", "User_talk", "Project",
         "Project_talk", "File", "File_talk", "MediaWiki", "MediaWiki_talk",
         "Template", "Template_talk", "Help", "Help_talk", "Category", "Category_talk",
         "Portal", "Wikipedia", "Wikipedia_talk"]
lowerCase = "[a-z]+"
suffix = ["jpg", "gif", "png", "JPG", "GIF", "PNG", "txt", "ico"]
extraWords = ["404_error/", "Main_Page", "Hypertext_Transfer_Protocol", "Search"]


for line in sys.stdin:
    line = line.strip()
    split = line.split(" ")
    
    if split.__len__() != 4 or not split[1].strip():
        continue

    if ':' in split[1]:
        title = split[1].split(":")
        if title[0] in words:
            continue

    if '.' in split[1]:
        tail = split[1].split(".")
        if tail[-1] in suffix:
            continue

    if not split[0] == name or re.match(lowerCase, split[1][0]) or split[1] in extraWords:
        continue
    else:
        fileName = os.environ["mapreduce_map_input_file"]
        fileName = fileName.split("-")[2]
        print '%s\t%s\t%s' % (split[1], fileName, split[2])

