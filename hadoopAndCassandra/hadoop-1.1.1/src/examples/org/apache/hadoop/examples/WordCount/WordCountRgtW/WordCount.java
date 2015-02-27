package org.apache.hadoop.examples;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class WordCount {

    public static class TokenizerMapper 
	extends Mapper<Object, Text, Text, Text>{
	private final static IntWritable one = new IntWritable(1);
	private Text word = new Text();
	public void map(Object key, Text value, Context context
			) throws IOException, InterruptedException {
	    StringTokenizer itr = new StringTokenizer(value.toString());
	    while (itr.hasMoreTokens()) {
		String str=itr.nextToken();
       		if(str.endsWith("00000")){
		    context.write(new Text(key.toString()),new Text(str+"_m"));
		}
	    }
	}
    }
  
    public static class IntSumReducer 
	extends Reducer<Text,Text,Text,Text> {
	public void reduce(Text key, Iterable<Text> values, 
			   Context context
			   ) throws IOException, InterruptedException {
	    Text val= values.iterator().next();
	    context.write(new Text(""), new Text(val.toString()+"-r"));
	}
    }

    public static void main(String[] args) throws Exception {
	final long startTime = System.currentTimeMillis(); 
	Configuration conf = new Configuration();
	String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
	if (otherArgs.length != 2) {
	    System.err.println("Usage: wordcount <in> <out>");
	    System.exit(2);
	}
	Job job = new Job(conf, "word count");
	job.setJarByClass(WordCount.class);
	job.setMapperClass(TokenizerMapper.class);
	job.setReducerClass(IntSumReducer.class);
	job.setOutputKeyClass(Text.class);
	job.setOutputValueClass(Text.class);
	FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
	FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));   
    
	if(job.waitForCompletion(true)){
	    final double duration = (System.currentTimeMillis() - startTime)/1000.0; // after                                               \
                                                                                                                                             
	    System.out.println();
	    System.out.println("Job Finished in " + duration + " seconds");
	    System.out.println();
	}


    }
}
