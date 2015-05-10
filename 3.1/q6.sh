#!/bin/bash
artist_name=$1
count=0
OLD_IFS="$IFS"
IFS=","
while read line
do
row=(${line})
shopt -s nocasematch
if [[ "${row[6]}" = "$artist_name" ]]
then
((count++))
fi
done < million_songs_metadata.csv
IFS="$OLD_IFS"
shopt -u nocasematch
echo $count

$(awk ' BEGIN {FS = ","} ; {) { print; }}')
echo $(join -1 1 -2 1 -2 2 -2 3 -1 2 -1 3 -1 4 -1 5 -1 6 -1 7 -1 8 -1 9 -1 10 -1 11)

# count=$(($count+1))