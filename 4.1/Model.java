import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.*;

import org.apache.hadoop.mapreduce.*; 
import org.apache.hadoop.hbase.mapreduce.*;

class ValueComparator implements Comparator<String> {
	 
    Map<String, Double> map;
 
    public ValueComparator(Map<String, Double> base) {
        this.map = base;
    }
 
    public int compare(String a, String b) {
        if (map.get(a) >= map.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys 
    }
}

public class Model {
	
	public static class Map extends Mapper<LongWritable, Text, Text, Text> {
		private Text output_phrase = new Text();
		private Text output_word_count = new Text();
		private int t = 2;
		
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			String line = value.toString();
			String[] split = line.split("\t");
			String phrase_word = split[0];
			String count = split[1];
					
			String[] phrase_word_split = phrase_word.split(" ");
			
			// check t 
			if(Integer.parseInt(count) > t) {

				if(phrase_word_split.length <= 1) {
					return;
				}		
				int lastSpaceIndex = phrase_word.lastIndexOf(" ");												
				String phrase = phrase_word.substring(0, lastSpaceIndex);
				String word   = phrase_word.substring(lastSpaceIndex+1);
				output_phrase.set(phrase);
				output_word_count.set(word + "\t" + count);
				context.write(output_phrase, output_word_count);
			}
		}
	}
	
	public static class Reducer extends TableReducer<Text, Text, ImmutableBytesWritable> {
		private int n = 5;
		@Override
		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			Put put = new Put(Bytes.toBytes(key.toString()));
			double count_phrase = 0;
			TreeMap<String, Double> map = new TreeMap<String, Double>();
			
			for(Text value : values) {
				String line = value.toString();
				String[] split = line.split("\t");
				String word = split[0];
				Double count_word = Double.parseDouble(split[1]);
				
				count_phrase += count_word;
				map.put(word, count_word);
			}
			// convert hashmap to treemap
			ValueComparator vc = new ValueComparator(map);
			TreeMap<String, Double> sortMap = new TreeMap<String, Double>();
			sortMap.putAll(map);
			
			Iterator iterator = sortMap.entrySet().iterator();
			for(int i = 0; i < n && iterator.hasNext(); i++) {
		    	Entry entry = (Entry)iterator.next();
		    	String word = (String)entry.getKey();
		    	Double prob = (Double)entry.getValue();
		    	prob /= count_phrase;
		    	put.add(Bytes.toBytes("probability"), Bytes.toBytes(word), Bytes.toBytes(String.valueOf(prob)));
			}
			
			// write into hbase
		    if(!put.isEmpty()) {
            	context.write(new ImmutableBytesWritable(key.getBytes()), put);
            }
		}
	}
	
	public static void main(String[] args) throws Exception {
		Configuration conf = HBaseConfiguration.create();
		Job job = new Job(conf, "project4.1");
		
		job.setJarByClass(Model.class);
		job.setMapperClass(Map.class);
		job.setReducerClass(Reducer.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
				
		TableMapReduceUtil.initTableReducerJob("project4.1", Reducer.class, job);
		FileInputFormat.addInputPath(job, new Path(args[0]));

		job.waitForCompletion(true);
	}	
}
