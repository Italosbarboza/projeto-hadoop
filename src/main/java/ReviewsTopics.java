import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import application.ReadDataFromJSONFILE2;
import application.VariablesJson;
import application.Corpus;
import application.LdaGibbsSampler;
import application.LdaUtil;


public class ReviewsTopics {

public static class MyMapper extends Mapper<LongWritable, Text, LongWritable, Text> {
		
		public static int reviewCount = 0;
		private static List<String> stopWords = Arrays.asList(new String[]{"n't", "'ll", "'ve", "1-1", "a", "a's", "able", "about", "above", "abroad", "abst", "accordance", "according", "accordingly", "across", "act", "actually", "added", "adj", "adopted", "affected", "affecting", "affects", "after", "afterwards", "again", "against", "ago", "ah", "ahead", "ain't", "all", "allow", "allows", "almost", "alone", "along", "alongside", "already", "also", "although", "always", "am", "amid", "amidst", "among", "amongst", "amoungst", "amount", "an", "and", "announce", "another", "any", "anybody", "anyhow", "anymore", "anyone", "anything", "anyway", "anyways", "anywhere", "apart", "apparently", "appear", "appreciate", "appropriate", "approximately", "are", "area", "areas", "aren", "aren't", "arent", "arise", "around", "as", "aside", "ask", "asked", "asking", "asks", "associated", "at", "auth", "available", "away", "awfully", "b", "back", "backed", "backing", "backs", "backward", "backwards", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "began", "begin", "beginning", "beginnings", "begins", "behind", "being", "beings", "believe", "below", "beside", "besides", "best", "better", "between", "beyond", "big", "bill", "biol", "both", "bottom", "brief", "briefly", "but", "by", "c", "c'mon", "c's", "ca", "call", "called", "came", "can", "can't", "cannot", "cant", "caption", "case", "cases", "cause", "causes", "certain", "certainly", "changes", "clear", "clearly", "co", "co.", "com", "come", "comes", "computer", "con", "concerning", "consequently", "consider", "considering", "contain", "containing", "contains", "corresponding", "could", "couldn't", "couldnt", "course", "cry", "currently", "d", "dare", "daren't", "date", "de", "dear", "definitely", "describe", "described", "despite", "detail", "did", "didn't", "differ", "different", "differently", "directly", "do", "does", "doesn't", "doing", "don't", "done", "down", "downed", "downing", "downs", "downwards", "due", "during", "e", "each", "early", "ed", "edu", "effect", "eg", "eight", "eighty", "either", "eleven", "else", "elsewhere", "empty", "end", "ended", "ending", "ends", "enough", "entirely", "especially", "et", "et-al", "etc", "even", "evenly", "ever", "evermore", "every", "everybody", "everyone", "everything", "everywhere", "ex", "exactly", "example", "except", "f", "face", "faces", "fact", "facts", "fairly", "far", "farther", "felt", "few", "fewer", "ff", "fifteen", "fifth", "fify", "fill", "find", "finds", "fire", "first", "five", "fix", "followed", "following", "follows", "for", "forever", "former", "formerly", "forth", "forty", "forward", "found", "four", "from", "front", "full", "fully", "further", "furthered", "furthering", "furthermore", "furthers", "g", "gave", "general", "generally", "get", "gets", "getting", "give", "given", "gives", "giving", "go", "goes", "going", "gone", "good", "goods", "got", "gotten", "greater", "greatest", "greetings", "group", "grouped", "grouping", "groups", "h", "had", "hadn't", "half", "happens", "hardly", "has", "hasn't", "hasnt", "have", "haven't", "having", "he", "he'd", "he'll", "he's", "hed", "held", "hello", "help", "hence", "her", "here", "here's", "hereafter", "hereby", "herein", "heres", "hereupon", "hers", "herse", "herself", "hes", "hi", "hid", "high", "higher", "highest", "him", "himse", "himself", "his", "hither", "home", "hopefully", "how", "howbeit", "however", "hundred", "i", "i'd", "i'll", "i'm", "i've", "id", "ie", "if", "ignored", "im", "immediate", "immediately", "importance", "important", "in", "inasmuch", "inc", "inc.", "include", "included", "including", "indeed", "index", "indicate", "indicated", "indicates", "information", "inner", "inside", "insofar", "instead", "interest", "interested", "interesting", "interests", "into", "invention", "inward", "is", "isn't", "it", "it'd", "it'll", "it's", "itd", "its", "itse", "itself", "j", "just", "k", "keep", "keeps", "kept", "keys", "kg", "kind", "km", "knew", "know", "known", "knows", "l", "large", "largely", "last", "late", "lately", "later", "latest", "latter", "latterly", "least", "led", "less", "lest", "let", "let's", "lets", "like", "liked", "likely", "likewise", "line", "links", "little", "long", "longer", "longest", "look", "looking", "looks", "low", "lower", "ltd", "m", "made", "mainly", "makes", "making", "man", "many", "may", "maybe", "mayn't", "me", "mean", "means", "meantime", "meanwhile", "member", "members", "men", "merely", "mg", "might", "mightn't", "mill", "million", "mine", "minus", "miss", "ml", "more", "moreover", "most", "mostly", "move", "moved", "mr", "mrs", "much", "mug", "must", "mustn't", "my", "myse", "myself", "n", "na", "name", "namely", "nay", "nd", "near", "nearly", "necessarily", "necessary", "need", "needed", "needing", "needn't", "needs", "neither", "never", "neverf", "neverless", "nevertheless", "new", "newer", "newest", "next", "nine", "ninety", "no", "no-one", "nobody", "non", "none", "nonetheless", "noone", "nor", "normally", "nos", "not", "noted", "nothing", "notwithstanding", "novel", "now", "nowhere", "number", "numbers", "o", "obtain", "obtained", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "older", "oldest", "omitted", "on", "once", "one", "one's", "ones", "only", "onto", "open", "opened", "opening", "opens", "opposite", "or", "ord", "order", "ordered", "ordering", "orders", "other", "others", "otherwise", "ought", "oughtn't", "our", "ours", "ourselves", "out", "outside", "over", "overall", "owing", "own", "p", "page", "pages", "part", "parted", "particular", "particularly", "parting", "parts", "past", "per", "perhaps", "place", "placed", "places", "please", "plus", "point", "pointed", "pointing", "points", "poorly", "possible", "possibly", "potentially", "pp", "predominantly", "present", "presented", "presenting", "presents", "presumably", "previously", "primarily", "probably", "problem", "problems", "promptly", "proud", "provided", "provides", "put", "puts", "q", "que", "quickly", "quite", "qv", "r", "ran", "rather", "rd", "re", "readily", "really", "reasonably", "received", "recent", "recently", "ref", "refs", "regarding", "regardless", "regards", "related", "relatively", "research", "respectively", "resulted", "resulting", "results", "right", "room", "rooms", "round", "run", "s", "said", "same", "saw", "say", "saying", "says", "sec", "second", "secondly", "seconds", "section", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "sees", "self", "selves", "sensible", "sent", "serious", "seriously", "seven", "several", "shall", "shan't", "she", "she'd", "she'll", "she's", "shed", "shes", "should", "shouldn't", "show", "showed", "showing", "shown", "showns", "shows", "side", "sides", "significant", "significantly", "similar", "similarly", "since", "sincere", "six", "sixty", "slightly", "small", "smaller", "smallest", "so", "some", "somebody", "someday", "somehow", "someone", "somethan", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon", "sorry", "specifically", "specified", "specify", "specifying", "state", "states", "still", "stop", "strongly", "sub", "substantially", "successfully", "such", "sufficiently", "suggest", "sup", "sure", "system", "t", "t's", "take", "taken", "taking", "tell", "ten", "tends", "th", "than", "thank", "thanks", "thanx", "that", "that'll", "that's", "that've", "thats", "their", "theirs", "them", "themselves", "then", "thence", "there", "there'd", "there'll", "there're", "there's", "there've", "thereafter", "thereby", "thered", "therefore", "therein", "thereof", "therere", "theres", "thereto", "thereupon", "these", "they", "they'd", "they'll", "they're", "they've", "theyd", "theyre", "thick", "thin", "thing", "things", "think", "thinks", "third", "thirty", "this", "thorough", "thoroughly", "those", "thou", "though", "thoughh", "thought", "thoughts", "thousand", "three", "throug", "through", "throughout", "thru", "thus", "til", "till", "time", "tip", "tis", "to", "today", "together", "too", "took", "top", "toward", "towards", "tried", "tries", "truly", "try", "trying", "ts", "turn", "turned", "turning", "turns", "twas", "twelve", "twenty", "twice", "two", "u", "un", "under", "underneath", "undoing", "unfortunately", "unless", "unlike", "unlikely", "until", "unto", "up", "upon", "ups", "upwards", "us", "use", "used", "useful", "usefully", "usefulness", "uses", "using", "usually", "uucp", "v", "value", "various", "versus", "very", "via", "viz", "vol", "vols", "vs", "w", "want", "wanted", "wanting", "wants", "was", "wasn't", "way", "ways", "we", "we'd", "we'll", "we're", "we've", "wed", "welcome", "well", "wells", "went", "were", "weren't", "what", "what'll", "what's", "what've", "whatever", "whats", "when", "whence", "whenever", "where", "where's", "whereafter", "whereas", "whereby", "wherein", "wheres", "whereupon", "wherever", "whether", "which", "whichever", "while", "whilst", "whim", "whither", "who", "who'd", "who'll", "who's", "whod", "whoever", "whole", "whom", "whomever", "whos", "whose", "why", "widely", "will", "willing", "wish", "with", "within", "without", "won't", "wonder", "words", "work", "worked", "working", "works", "world", "would", "wouldn't", "written", "www", "x", "y", "year", "years", "yes", "yet", "you", "you'd", "you'll", "you're", "you've", "youd", "young", "younger", "youngest", "your", "yourabout", "youre", "yours", "yourself", "yourselves", "z", "zero"});
		
		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			VariablesJson variablesJson = ReadDataFromJSONFILE2.getVariable(value.toString());
			if(variablesJson.getText() != null && !variablesJson.getText().isEmpty()) {
				ArrayList<String> list = new ArrayList<String>();
				
				Properties props = new Properties();
				props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
				StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
				
				Annotation document = new Annotation(variablesJson.getText());
				
				pipeline.annotate(document);
				
				List<CoreMap> sentences = document.get(SentencesAnnotation.class);
				
				for (CoreMap sentence : sentences) {
					for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
						String word = token.get(TextAnnotation.class);
						String ne = token.get(NamedEntityTagAnnotation.class);
						if(!ne.equals("NUMBER") && !ne.equals("ORDINAL") 
								&& !ne.equals("PERCENT")  && !ne.equals("DATE") 
								&& !ne.equals("EMAIL")    && !ne.equals("MONEY") 
								&& !ne.equals("TIME")  	  && !ne.equals("URL") 
								&& !word.startsWith("http://")
								&& word.length() > 2 
								&& !stopWords.contains(word)) {
							list.add(word.toLowerCase().trim());
						}
					}
				}
				context.write(key, new Text(String.join(",", list)));
				reviewCount++;
			}
		}
	}
	
	public static class MyReducer extends Reducer<LongWritable, Text, Text, NullWritable> {
		
		public static int resultCount = 0;
		private static Corpus corpus = new Corpus();
		
		public void reduce(LongWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
			Text array = values.iterator().next();
			ArrayList<String> document = new ArrayList<String>();
			String[] words = array.toString().split(",");
			for (String text : words) {
				document.add(text.toString().trim());
			}
			resultCount++;
			
			if(MyMapper.reviewCount != resultCount) {
				return;
			}
			
			corpus.addDocument(document);

			LdaGibbsSampler ldaGibbsSampler = new LdaGibbsSampler(corpus.getDocument(), corpus.getVocabularySize());

			ldaGibbsSampler.gibbs(10);

			double[][] phi = ldaGibbsSampler.getPhi();
			Map<String, Double>[] topicMap = LdaUtil.translate(phi, corpus.getVocabulary(), 10);
			
			int i = 0;
			for (Map<String, Double> topics : topicMap) {
				ArrayList<String> result = new ArrayList<String>();
				result.add(String.format("Topic %d", i++));
				
				for (Map.Entry<String, Double> entry : topics.entrySet()) {
					result.add(entry.getKey() + " (" + String.format("%.4f", entry.getValue()) + ")");
				}
				
				context.write(new Text(String.join("\t", result)), null);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
	    Job job = Job.getInstance(conf, "review topics");
	    job.setJarByClass(ReviewsTopics.class);
	    job.setMapperClass(MyMapper.class);
	    job.setReducerClass(MyReducer.class);

	    job.setMapOutputKeyClass(LongWritable.class);
		job.setMapOutputValueClass(Text.class);
	    
	    job.setOutputKeyClass(Text.class);
	    job.setOutputValueClass(NullWritable.class);
	    
	    FileInputFormat.addInputPath(job, new Path("/home/italo/Área de Trabalho/www/UFC/files/entrada"));
	    FileOutputFormat.setOutputPath(job, new Path("/home/italo/Área de Trabalho/www/UFC/files/saida"));
	    
	    System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}