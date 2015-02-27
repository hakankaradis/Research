import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.io.UnsupportedEncodingException;
import org.apache.cassandra.thrift.*;
import org.apache.cassandra.hadoop.ColumnFamilyOutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.IColumn;
import org.apache.cassandra.hadoop.ColumnFamilyInputFormat;
import org.apache.cassandra.hadoop.ConfigHelper;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * This counts the occurrences of words in ColumnFamily Standard1, that has a single column (that we care about)
 * "text" containing a sequence of words.
 *
 * For each word, we output the total number of occurrences across all texts.
 *
 * When outputting to Cassandra, we write the word counts as a {word, count} column/value pair,
 * with a row key equal to the name of the source column we read the words from.
 */
public class WordCount extends Configured implements Tool
{
    private static final Logger logger = LoggerFactory.getLogger(WordCount.class);

    //    static final String KEYSPACE = "wordcount";
    //  static final String COLUMN_FAMILY = "input_words";
    static final String KEYSPACE = "LogData";
    static final String COLUMN_FAMILY = "User";

    static final String OUTPUT_REDUCER_VAR = "output_reducer";
    static final String OUTPUT_COLUMN_FAMILY = "output";
    // private static final String OUTPUT_PATH_PREFIX = "word_count-output";
    private static final String OUTPUT_PATH_PREFIX = "/import/linux/home/bsendir1/word_count-output";
    private static final String CONF_COLUMN_NAME = "columnname";

    public static void main(String[] args) throws Exception
    {
        // Let ToolRunner handle generic command-line options
        ToolRunner.run(new Configuration(), new WordCount(), args);
        System.exit(0);
    }

    public static class TokenizerMapper extends Mapper<ByteBuffer, SortedMap<ByteBuffer, IColumn>, Text, Text>
    {
        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();
        private ByteBuffer sourceColumn;

        protected void setup(org.apache.hadoop.mapreduce.Mapper.Context context)
	    throws IOException, InterruptedException
	    {
	    }

        public void map(ByteBuffer key, SortedMap<ByteBuffer, IColumn> columns, Context context) throws IOException, InterruptedException
	    {
		
		//here
		
		for (IColumn column : columns.values())
		    {
			String name  = ByteBufferUtil.string(column.name());
			String value = null;
			
			value = ByteBufferUtil.string(column.value());

			logger.debug("read {}:{}={} from {}",
				     new Object[] {ByteBufferUtil.string(key), name, value, context.getInputSplit()});

			StringTokenizer itr = new StringTokenizer(value);
			while (itr.hasMoreTokens())
			    {
				// word.set(value + "-m");//itr.nextToken());
				// context.write(word,new Text(value+"-m"));
				String str = itr.nextToken();
				
				StringBuilder concat=new StringBuilder();
				for(int i=0;i<10;i++)
				    concat.append(str);
				context.write(new Text(ByteBufferUtil.string(key)),new Text(concat.toString()+"_m"));                  
			    }
		    }
		
		//here
	    }
    }

    public static class ReducerToFilesystem extends Reducer<Text, Text, Text, Text>
    {
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException
	    {
		/*
		String sum = "";
		Text val = values.iterator().next();
		context.write(new Text(""), new Text(val.toString()+"-r"));
		*/
	    }
    }

    public static class ReducerToCassandra extends Reducer<Text, Text, ByteBuffer, List<Mutation>>
    {
        private ByteBuffer outputKey;

        /*protected void setup(org.apache.hadoop.mapreduce.Reducer.Context context)
	    throws IOException, InterruptedException
	    {
		outputKey = ByteBufferUtil.bytes(context.getConfiguration().get(CONF_COLUMN_NAME));
	    }
	*/
        public void reduce(Text word, Iterable<Text> values, Context context) throws IOException, InterruptedException
	    {
		ByteBuffer outputKey = ByteBuffer.wrap(word.toString().getBytes("UTF-8"));
		//int sum = 0;
		//for (IntWritable val : values)
		//  sum += val.get();
		Text val = values.iterator().next();
		//context.write(new Text(""), new Text(val.toString()+"-r"));
		context.write(outputKey, Collections.singletonList(getMutation(word, new Text(val.toString()+"-r"))));
		//context.write(outputKey, Collections.singletonList(getMutation(word, sum)));

	    }

	public static ByteBuffer toByteBuffer(String value)throws UnsupportedEncodingException
	{
	    return ByteBuffer.wrap(value.getBytes("UTF-8"));
	}

        private static Mutation getMutation(Text word, Text sum)
        {
	    Column c = new Column();
	    try{
            
		c.setName(Arrays.copyOf(word.getBytes(), word.getLength()));
		c.setValue(toByteBuffer(sum.toString()));
		//(ByteBufferUtil.bytes(sum));
		c.setTimestamp(System.currentTimeMillis());
	    } catch(UnsupportedEncodingException e){}

            Mutation m = new Mutation();
            m.setColumn_or_supercolumn(new ColumnOrSuperColumn());
            m.column_or_supercolumn.setColumn(c);
            return m;
	   
        }
    }///XXXXXX


    public int run(String[] args) throws Exception
    {

	///start
	final long startTime = System.currentTimeMillis();  
        String outputReducerType = "filesystem";
        if (args != null && args[0].startsWith(OUTPUT_REDUCER_VAR))
	    {
		String[] s = args[0].split("=");
		if (s != null && s.length == 2)
		    outputReducerType = s[1];
	    }
        logger.info("output reducer type: " + outputReducerType);

        // use a smaller page size that doesn't divide the row count evenly to exercise the paging logic better
        ConfigHelper.setRangeBatchSize(getConf(), 99);

        for (int i = 0; i < WordCountSetup.TEST_COUNT; i++)
	    {
		String columnName="userId";
		//	if(i==0){columnName="userId";}
		//	if(i==1){columnName="action";}
		//	if(i==2){columnName="link";}
		
		//System.out.println("test");
		Job job = new Job(getConf(), "wordcount");
		job.setJarByClass(WordCount.class);
		job.setMapperClass(TokenizerMapper.class);
		//System.out.println("test");

		if (outputReducerType.equalsIgnoreCase("filesystem"))
		    {
			//job.setCombinerClass(ReducerToFilesystem.class);
			job.setReducerClass(ReducerToFilesystem.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);
			FileOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH_PREFIX + i));
		    }
		else
		    {
			job.setReducerClass(ReducerToCassandra.class);

			job.setMapOutputKeyClass(Text.class);
			job.setMapOutputValueClass(Text.class);
			job.setOutputKeyClass(ByteBuffer.class);
			job.setOutputValueClass(List.class);

			job.setOutputFormatClass(ColumnFamilyOutputFormat.class);

			ConfigHelper.setOutputColumnFamily(job.getConfiguration(), KEYSPACE, OUTPUT_COLUMN_FAMILY);
			job.getConfiguration().set(CONF_COLUMN_NAME, "sum");
		    }
		//System.out.println("tes4t");

		job.setInputFormatClass(ColumnFamilyInputFormat.class);

		ConfigHelper.setInputRpcPort(job.getConfiguration(), "9160");
		ConfigHelper.setInputInitialAddress(job.getConfiguration(), "localhost");
		ConfigHelper.setInputPartitioner(job.getConfiguration(), "RandomPartitioner");
		ConfigHelper.setInputColumnFamily(job.getConfiguration(), KEYSPACE, COLUMN_FAMILY);
		//System.out.println("tessssst");

		SlicePredicate predicate = new SlicePredicate().setColumn_names(Arrays.asList(ByteBufferUtil.bytes(columnName))); 
		ConfigHelper.setInputSlicePredicate(job.getConfiguration(), predicate);

		// this will cause the predicate to be ignored in favor of scanning everything as a wide row
          
		//Son degisiklik
		// ConfigHelper.setInputColumnFamily(job.getConfiguration(), KEYSPACE, COLUMN_FAMILY, true);
		//System.out.println("tessssssaaat");

		ConfigHelper.setOutputInitialAddress(job.getConfiguration(), "localhost");
		ConfigHelper.setOutputPartitioner(job.getConfiguration(), "RandomPartitioner");

		job.waitForCompletion(true);
	    }
	
	//end
	//print
	final double duration = (System.currentTimeMillis() - startTime)/1000.0; // after
        System.out.println();
        System.out.println("Job Finished in " + duration + " seconds");
        System.out.println();

        return 0;
    }
}
