import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import org.apache.hadoop.util.*; 



public class Ngrams {
	
	public static class Map extends Mapper<LongWritable, Text, Text, IntWritable> {
		private final static IntWritable one = new IntWritable(1);
		private Text word = new Text();
		
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
			
			// trim space first, then replace non alphabetical with space
			line = line.trim().replaceAll("[^A-Za-z]+", " ");
			// trim again, replace more than one spaces with one space
			line = line.trim().replaceAll(" +", " ");
			// convert to lowercase
			line = line.toLowerCase();
			
			// maintain a window to get 1-5 grams
			if(line != null && line.length() > 0) {
				String[] split = line.split(" ");
				for(int i = 0; i < split.length; i++) {
					StringBuilder sb = new StringBuilder();
					for(int window = 0; window<5 && i+window < split.length; window++) {
						if(window == 0) {
							sb.append(split[i+ window]);
							word.set(sb.toString());
							context.write(word, one);
						}
						else {
							sb.append(" ")
							  .append(split[i+window]);
							word.set(sb.toString());
							context.write(word, one);
						}
					}
				}
			}
		}
 	}
	
	public static class Reduce extends Reducer<Text, IntWritable, Text, IntWritable> {
		
		public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException{
			int sum = 0;
			for(IntWritable val : values) {
				sum += val.get();
			}
			context.write(key, new IntWritable(sum));
		}
	}
	
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		
		Job job = new Job(conf, "wordcount");
		job.setJarByClass(Ngrams.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		
		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);
		
		job.setInputFormatClass(TextInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		
		job.waitForCompletion(true);
	}
}
