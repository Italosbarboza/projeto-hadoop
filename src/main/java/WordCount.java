import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import application.ReadDataFromJSONFILE2;
import application.VariablesJson;

public class WordCount {

  public static class TokenizerMapper
       extends Mapper<Object, Text, Text, IntWritable>{

    private final static IntWritable one = new IntWritable(1);

    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {    
      VariablesJson review = ReadDataFromJSONFILE2.getVariable(value.toString());
      
      if(review.getText() != null) {
    	  StringTokenizer itr = new StringTokenizer(review.getCleanedContent());
    	  while (itr.hasMoreTokens()) {
    	        String word = itr.nextToken();
    	        context.write(new Text(word), one);
    	      }
      }
    }
  }

  public static class IntSumReducer
       extends Reducer<Text,IntWritable,Text,IntWritable> {
    public void reduce(Text key, Iterable<IntWritable> values,
                       Context context
                       ) throws IOException, InterruptedException {
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      context.write(new Text(key + "\t" + sum), null);
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf, "word count");
    job.setJarByClass(WordCount.class);
    job.setMapperClass(TokenizerMapper.class);
    job.setReducerClass(IntSumReducer.class);

    job.setMapOutputKeyClass(Text.class);
	job.setMapOutputValueClass(IntWritable.class);
    
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(NullWritable.class);
    
    FileInputFormat.addInputPath(job, new Path("/home/italo/Área de Trabalho/www/UFC/files/entrada"));
    FileOutputFormat.setOutputPath(job, new Path("/home/italo/Área de Trabalho/www/UFC/files/saida"));
    
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}