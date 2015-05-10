sql answer

7
join -t"," -o 2.1 2.2 2.3 1.4 1.5 1.6 1.7 1.8 1.9 1.10 1.11 million_songs_metadata.csv million_songs_sales_data.csv >million_songs_metadata_and_sales.csv

8
sort -t"," -k3 -n -r million_songs_metadata_and_sales.csv > sort.csv
line=$(head -n 1 sort.csv);
echo $line|awk -F ',' '{print $7}'


# Qustion 9
# Write a SQL query that returns the trackid of the song with the maximum duration
answer_9() {
    # Write a SQL query to get the answer to Q9. Do not just echo the answer.
    # Please put your SQL statement within the double quotation marks, and 
    # don't modify the command outside the double quotation marks.
    # If you need to use quotation marks in you SQL statment, please use
    # single quotation marks instead of double.
    mysql --skip-column-names --batch -u root -pdb15319root song_db -e "SELECT id FROM songs WHERE duration=(SELECT MAX(duration) FROM songs);"

}

# Question 10
# A database index is a data structure that improves the speed of data retreival.
# Identify the field that will improve the performance of query in question 9 
# and create a database index on that field
INDEX_NAME="duration_index"
answer_10() {
    # Write a SQL query that will create a index on the field
    mysql --skip-column-names --batch -u root -pdb15319root song_db -e "create index duration_index on songs(duration);"
}

# Question 11
# Write a SQL query that returns the trackid of the song with the maximum duration
# This is the same query as Question 9. Do you see any difference in performance?
answer_11() {
    # Write a SQL query to get the answer to Q11. Do not just echo the answer.
    # Please put your SQL statement within the double quotation marks, and 
    # don't modify the command outside the double quotation marks.
    # If you need to use quotation marks in you SQL statment, please use
    # single quotation marks instead of double.
    mysql --skip-column-names --batch -u root -pdb15319root song_db -e "SELECT id FROM songs WHERE duration=(SELECT MAX(duration) FROM songs);"
}


#Question 12
# Write the SQL query that returns all matches (across any column), 
# similar to the command grep -P 'The Beatles' | wc -l:
answer_12() {
        # Write a SQL query to get the answer to Q12. Do not just echo the answer.
        # Please put your SQL statement within the double quotation marks, and 
        # don't modify the command outside the double quotation marks.
        # If you need to use quotation marks in you SQL statment, please use
        # single quotation marks instead of double.
        mysql --skip-column-names --batch -u root -pdb15319root song_db -e "select * from songs where binary title like '%The Beatles%' OR binary relea like '%The Beatles%' OR binary art_name like '%The Beatles%';"
}

#Question 13
# Write the SQL query that returns all matches (across any column), 
# similar to the command grep -i -P 'The Beatles' | wc -l:
answer_13() {
        # Write a SQL query to get the answer to Q13. Do not just echo the answer.
        # Please put your SQL statement within the double quotation marks, and 
        # don't modify the command outside the double quotation marks.
        # If you need to use quotation marks in you SQL statment, please use
        # single quotation marks instead of double.
        mysql --skip-column-names --batch -u root -pdb15319root song_db -e "select * from songs where title like '%The Beatles%' OR relea like '%The Beatles%' OR art_name like '%The Beatles%';"
}

#Question 14
# Which year has the third-most number of rows in the Table songs? 
# The output should be a number representing the year
# Ignore the songs that do not have a specified year
answer_14() {
        # Write a SQL query to get the answer to Q14. Do not just echo the answer.
        # Please put your SQL statement within the double quotation marks, and 
        # don't modify the command outside the double quotation marks.
        # If you need to use quotation marks in you SQL statment, please use
        # single quotation marks instead of double.
        mysql --skip-column-names --batch -u root -pdb15319root song_db -e "select year from songs GROUP BY year Order BY count(*) DESC LIMIT 3,1;"
}

answer_15() {
        # Write a SQL query to get the answer to Q15. Do not just echo the answer.
        # Please put your SQL statement within the double quotation marks, and 
        # don't modify the command outside the double quotation marks.
        # If you need to use quotation marks in you SQL statment, please use
        # single quotation marks instead of double.
        mysql --skip-column-names --batch -u root -pdb15319root song_db -e "select art_name from songs GROUP BY art_id Order BY count(*) DESC LIMIT 2,1;"
}

#Question 16
# What is the total sales count of Michael Jackson's songs?
# Your query should return a single number.
# Please use artist_id(ARXPPEY1187FB51DF4) as the unique identifier of the artist
answer_16() {
        # Write a SQL query to get the answer to Q16. Do not just echo the answer.
        # Please put your SQL statement within the double quotation marks, and 
        # don't modify the command outside the double quotation marks.
        # If you need to use quotation marks in you SQL statment, please use
        # single quotation marks instead of double.
        mysql --skip-column-names --batch -u root -pdb15319root song_db -e "SELECT SUM(count) FROM songs INNER JOIN sales on songs.id = sales.id WHERE songs.art_id = (SELECT DISTINCT art_id FROM songs WHERE art_id = 'ARXPPEY1187FB51DF4');"
}

#Question 17
# Which artist has the third-most number of songs that have more than 50 units in sales.
# Please use artist_name as the unique identifier of the artist.
# Output the name of the artist.
answer_17() {
        # Write a SQL query to get the answer to Q17. Do not just echo the answer.
        # Please put your SQL statement within the double quotation marks, and 
        # don't modify the command outside the double quotation marks.
        # If you need to use quotation marks in you SQL statment, please use
        # single quotation marks instead of double.
        mysql --skip-column-names --batch -u root -pdb15319root song_db -e "SELECT art_name FROM songs INNER JOIN sales on songs.id = sales.id GROUP BY art_name ORDER BY SUM(count>50) DESC LIMIT 2,1;"
}


