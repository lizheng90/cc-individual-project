import scala.Tuple2;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

class TupleComparator implements Comparator<Tuple2<Tuple2<String, String>, Double>>, Serializable {
    @Override
    public int compare(Tuple2<Tuple2<String, String>, Double> tuple1, Tuple2<Tuple2<String, String>, Double> tuple2) {
        return tuple1._2() < tuple2._2() ? 1 : 0;
    }
}

public final class TFIDF {
	
  private static final Pattern SPACE = Pattern.compile(" ");  
//  private static final String FilePath = "s3n://15619project4/sample.txt";

  private static final String FilePath = "s3n://s15-p42-part1-easy/data/";

  public static void main(String[] args) throws Exception {

    SparkConf sparkConf = new SparkConf().setAppName("TFIDF");
    JavaSparkContext ctx = new JavaSparkContext(sparkConf);
    
    JavaRDD<String> lines = ctx.textFile(FilePath);
    
    // total documents
    final Long N = lines.count();
    System.out.println(N);
    
    // TF tuple
	JavaRDD<Tuple2<Tuple2<String, String>, Integer>> word_document_count = lines.flatMap(new FlatMapFunction<String, Tuple2<Tuple2<String, String>, Integer>>() {
	      @Override
	      public Iterable<Tuple2<Tuple2<String, String>, Integer>> call(String s) {
	    	HashMap<Tuple2<String, String>, Integer> map = new HashMap<Tuple2<String, String>, Integer>();
	    	List<Tuple2<Tuple2<String, String>, Integer>> res = new ArrayList<Tuple2<Tuple2<String, String>, Integer>>();
	    	String title = s.split("\t")[1];
	    	String content = s.split("\t")[3]
	    			.replaceAll("\\<.*?\\>", " ")
	    			.replace("\\n", "")
	    			.toLowerCase()
	    			.replaceAll("[^a-z]", " ")
	    			.trim()
	    			.replaceAll(" +", " ");
	    	for(int i = 0; i < SPACE.split(content).length; i++) {
	    		Tuple2<String, String> tuple = new Tuple2<String, String>(SPACE.split(content)[i], title);
	    		if(map.containsKey(tuple)) {
	    			map.put(tuple, map.get(tuple) + 1);
	    		}
	    		else {
	    			map.put(tuple, 1);
	    		}
	    	}
	    	Iterator it = map.entrySet().iterator();
	    	while (it.hasNext()) {
	    		Entry pair = (Entry) it.next();
	    		Tuple2<Tuple2<String, String>, Integer> newTuple = new Tuple2<Tuple2<String, String>, Integer>((Tuple2<String, String>)pair.getKey(), (Integer)pair.getValue()); 
	    		res.add(newTuple);
	    	}
	        return res;
	      }
	});
	
	// TF pair
	JavaPairRDD<Tuple2<String, String>, Integer> tf_pair = word_document_count.mapToPair(new PairFunction<Tuple2<Tuple2<String, String>, Integer>, Tuple2<String, String>, Integer>() {
		@Override
		public Tuple2<Tuple2<String, String>, Integer> call(Tuple2<Tuple2<String, String>, Integer> tuple) {
			return new Tuple2<Tuple2<String, String>, Integer>(tuple._1(), tuple._2());
		}
	});
    
	// IDF tuple
	JavaRDD<Tuple2<String, String>> dword_document_count = lines.flatMap(new FlatMapFunction<String, Tuple2<String, String>>() {
		@Override
		public Iterable<Tuple2<String, String>> call(String s) {
			// HashSet to check if there's duplicated word
			HashSet<Tuple2<String, String>> set = new HashSet<Tuple2<String, String>>();
	    	List<Tuple2<String, String>> res = new ArrayList<Tuple2<String, String>>();
	    	String title = s.split("\t")[1];
	    	String content = s.split("\t")[3]
	    			.replaceAll("\\<.*?\\>", " ")
	    			.replace("\\n", "")
	    			.toLowerCase()
	    			.replaceAll("[^a-z]", " ")
	    			.trim()
	    			.replaceAll(" +", " ");
	    	for(int i = 0; i < SPACE.split(content).length; i++) {
	    		Tuple2<String, String> tuple = new Tuple2<String, String>(SPACE.split(content)[i], title);
	    		if(set.contains(tuple)) {
	    			continue;
	    		}
	    		else {
	    			res.add(tuple);
		    		set.add(tuple);
	    		}
	    	}
			return res;
		}
	});
	
	// IDF map pair
	JavaPairRDD<Tuple2<String, String>, Double> idf_ones = dword_document_count.mapToPair(new PairFunction<Tuple2<String, String>, Tuple2<String, String>, Double>() {
		@Override
		public Tuple2<Tuple2<String, String>, Double> call(Tuple2<String, String> tuple) {
			return new Tuple2<Tuple2<String, String>, Double>(tuple, 1.0);
		}
	});
	
	// IDF reduce
    JavaPairRDD<Tuple2<String, String>, Double> idf_counts = idf_ones.reduceByKey(new Function2<Double, Double, Double>() {
        @Override
        public Double call(Double d1, Double d2) {
          return d1 + d2;
        }
    }); 
    
    // TF IDF join
    JavaPairRDD<Tuple2<String, String>, Tuple2<Integer, Double>> tfidf = tf_pair.join(idf_counts);
    
    // final TFIDF
    JavaRDD<Tuple2<Tuple2<String, String>, Double>> final_tfidf = tfidf.flatMap(new FlatMapFunction<Tuple2<Tuple2<String, String>, Tuple2<Integer, Double>>, Tuple2<Tuple2<String, String>, Double>>() {
    	@Override
		public Iterable<Tuple2<Tuple2<String, String>, Double>> call(Tuple2<Tuple2<String, String>, Tuple2<Integer, Double>> tuple) {
    		List<Tuple2<Tuple2<String, String>, Double>> res = new ArrayList<Tuple2<Tuple2<String, String>, Double>>();
    		res.add(new Tuple2<Tuple2<String, String>, Double>(tuple._1(), Math.log(N/tuple._2()._2())*tuple._2()._1()));
    		
        	return res;

    	}
    });
    
    // filter cloud
    JavaRDD<Tuple2<Tuple2<String, String>, Double>> filter = final_tfidf.filter(new Function<Tuple2<Tuple2<String, String>, Double>, Boolean>() {
    	public Boolean call(Tuple2<Tuple2<String, String>, Double> tuple) {
    		return tuple._1()._1().equals("cloud");
    	}
    });
    
    // print out
    List<Tuple2<Tuple2<String, String>, Double>> output = final_tfidf.collect();
    
    Collections.sort(output, new TupleComparator());

    for(int i = 0; i < output.size(); i++) {
    	Tuple2<Tuple2<String, String>, Double> tuple = output.get(i);
        System.out.println(tuple._1()._2() + ": " + tuple._2());
    }

//    for (Tuple2<?,?> tuple : output) {
//      System.out.println(tuple._1() + ": " + tuple._2());
//    }
    
    ctx.stop();
  }
}
