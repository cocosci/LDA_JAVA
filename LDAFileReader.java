import java.io.*;
import java.util.*;
import java.util.Map.Entry;
public class LDAFileReader {
	private String inputFile;
	private String outputFile;
	private Integer iterations;
	private Integer totalNumofDistinctWord = 0;
	private int totalNumofDocs = 0;
	
	private double alpha;
	private double beta;
	private int numoftopic;
	private int topkwords;
	
	private HashMap<String,Integer> word2id = new HashMap<String,Integer>();
	private HashMap<Integer,String> id2word = new HashMap<Integer,String>();
	private HashMap<Integer,Integer> id2freq = new HashMap<Integer,Integer>();
	private HashMap<String,Integer> word2freq = new HashMap<String,Integer>();
	
	private ArrayList<String> stopwords =new ArrayList<String> (); 
	
	private ArrayList<ArrayList<String>> wordindoc = new ArrayList<ArrayList<String>>();
	private ArrayList<ArrayList<Integer>> topicass = new ArrayList<ArrayList<Integer>>();
	
	/**Important three counters for Gibbs sampling
	 * Let's say the input data and topic assignments are
	 * doc0: I0 hate1 you2
	 * doc1: I2 love1 you2
	 * Then count for words with topic 1 in doc 1 is 2
	 * count for you with topic 2 is 2
	 * count for words with topic 2 is 3**/
	private ArrayList<ArrayList<Integer>> Count_for_words_with_topicK_in_docJ = new ArrayList<ArrayList<Integer>>();
	private ArrayList<ArrayList<Integer>> Count_for_thisword_with_topicK_ALLdoc = new ArrayList<ArrayList<Integer>>();
	private ArrayList<Integer> Count_for_words_with_topicK_in_ALLdoc = new ArrayList<Integer>();
	
	/**
	 * Two important matrices for presenting results.*/
	private ArrayList<ArrayList<Double>> ThePHI = new ArrayList<ArrayList<Double>>();
	private ArrayList<ArrayList<Double>> TheTHETA = new ArrayList<ArrayList<Double>>();
	/**
	 * Constructor with three helper functions as follows.*/
	LDAFileReader(String[] args) throws IOException{
		parseArgs(args);
		ReadStopwords();
		ReadfromFilesandInit();
		InitBig3();
		printVariables();
	}
	/**
	 * Init the big three counters for Gibbs sampling via topicass*/
	private void InitBig3(){
		//for counter2 preprocessing
		
		for(int i=0; i<totalNumofDistinctWord;i++){
			ArrayList<Integer> tempforcounter2 = new ArrayList<Integer>();
			for(int j=0; j<numoftopic;j++){
				tempforcounter2.add(0);
			}
			Count_for_thisword_with_topicK_ALLdoc.add(tempforcounter2);
		}	
		//for counter1 preprocessing
		for(int j = 0; j < numoftopic;j++){
			Count_for_words_with_topicK_in_ALLdoc.add(0);
		}
		
		// init counter1,2 and 3.
		for(int i = 0; i<topicass.size();i++){
			ArrayList<Integer> tempforcounter1 = new ArrayList<Integer>();
			for(int j = 0; j < numoftopic;j++){
				tempforcounter1.add(0);
			}
			
			for(int j = 0; j<topicass.get(i).size();j++){
				//update counter1
				tempforcounter1.set(topicass.get(i).get(j), tempforcounter1.get(topicass.get(i).get(j))+1);
				//update counter3
				Count_for_words_with_topicK_in_ALLdoc.set(topicass.get(i).get(j), Count_for_words_with_topicK_in_ALLdoc.get(topicass.get(i).get(j))+1);
				//update counter2
				String word =  wordindoc.get(i).get(j);
				Integer wordid = word2id.get(word);
				ArrayList<Integer> tempforcounter2 = Count_for_thisword_with_topicK_ALLdoc.get(wordid);
				tempforcounter2.set(topicass.get(i).get(j), tempforcounter2.get(topicass.get(i).get(j))+1);
				Count_for_thisword_with_topicK_ALLdoc.set(wordid, tempforcounter2);
			}
			Count_for_words_with_topicK_in_docJ.add(tempforcounter1);
		}
			
	}
	private void parseArgs(String[] args) {
        inputFile=args[0];
        outputFile=args[1];
        iterations=Integer.parseInt(args[2]);
        alpha = Double.parseDouble(args[3]);
        beta = Double.parseDouble(args[4]);
        numoftopic = Integer.parseInt(args[5]);
        topkwords = Integer.parseInt(args[6]);
       // df=Double.parseDouble(args[3]);
    }
	/**
	 * Read from the input file to initialize variables.*/
	private void ReadStopwords() throws IOException{
		try(BufferedReader br = new BufferedReader(new FileReader("stopwords.txt"))){
			for(String line; (line = br.readLine()) != null; ){
				//System.out.println(line);				
				String split_line[]=line.split(" ");
				for(int i = 0; i < split_line.length; i++){
					stopwords.add(split_line[i]);
				}
			}
		}
		}
	/**
	 * Read from the input file to initialize variables.*/
	private void ReadfromFilesandInit() throws IOException{
		try(BufferedReader br = new BufferedReader(new FileReader(inputFile))){
			String firstline = br.readLine();
			totalNumofDocs = Integer.parseInt(firstline);
			for(String line; (line = br.readLine()) != null; ){
				//clean the input data to remove punctuations..	
				String[] split_line = line.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
				
				// Init wordindoc, word2id,id2freq,word2freq, totalnumofdictinctword, etc.
				ArrayList<String> temp = new ArrayList<String>();
				ArrayList<Integer> temp2 = new ArrayList<Integer>();
				for(int i = 0; i < split_line.length; i++){
					//screen out all stop words
					if(!stopwords.contains(split_line[i])){
						temp.add(split_line[i]);//indexing the current word.
						temp2.add((int)(Math.random() * (numoftopic)));//assign it to a random topic.
						if(!word2id.containsKey(split_line[i])){
							word2id.put(split_line[i], totalNumofDistinctWord);	
							id2word.put(totalNumofDistinctWord, split_line[i]);
							id2freq.put(totalNumofDistinctWord, 1);
							word2freq.put(split_line[i], 1);
							totalNumofDistinctWord++;
						}else{
							id2freq.put(word2id.get(split_line[i]), id2freq.get(word2id.get(split_line[i]))+1);
							word2freq.put(split_line[i], id2freq.get(word2id.get(split_line[i]))+1);
						}
					}
				}
				wordindoc.add(temp);
				topicass.add(temp2);
			}
			// if there are 5 distinct words, then totalNumofDistinctWord is 5. not 4.
		}		
	}
	/**
	 * Print out some private variables and hashmaps for debugging.*/
	private void printVariables(){
		System.out.println("The input:");
		for(int i = 0; i<wordindoc.size();i++){
			for(int j = 0; j<wordindoc.get(i).size();j++){
				System.out.print(wordindoc.get(i).get(j)+" ");
			}
			System.out.println();
		}
		System.out.println("For topic assignments:");
		for(int i = 0; i<topicass.size();i++){
			for(int j = 0; j<topicass.get(i).size();j++){
				System.out.print(topicass.get(i).get(j)+" ");
			}
			System.out.println();
		}
		
		
		System.out.println("totalNumofDistinctWord:"+(totalNumofDistinctWord.toString()));
		Iterator it = word2id.entrySet().iterator();
		System.out.println("word:id");
		while(it.hasNext()){
			Map.Entry<String,Integer> pair = (Map.Entry)it.next();
			System.out.println(pair.getKey()+": "+pair.getValue());
		}
		it = id2freq.entrySet().iterator();
		System.out.println("id:freq");
		while(it.hasNext()){
			Map.Entry<Integer,Integer> pair = (Map.Entry)it.next();
			System.out.println(pair.getKey()+": "+pair.getValue());
		}
		it = word2freq.entrySet().iterator();
		System.out.println("word:freq");
		while(it.hasNext()){
			Map.Entry<String,Integer> pair = (Map.Entry)it.next();
			System.out.println(pair.getKey()+": "+pair.getValue());
		}
		
		
		System.out.println("For counter1:");
		for(int i = 0; i<topicass.size();i++){
			for(int j = 0; j<numoftopic;j++){
				System.out.print(Count_for_words_with_topicK_in_docJ.get(i).get(j)+" ");
			}
			System.out.println();
		}
		
		System.out.println("For counter2:");
		for(int i=0; i<totalNumofDistinctWord;i++){
			String word = id2word.get(i);
			for(int j = 0; j<numoftopic; j++){
				System.out.print(Count_for_thisword_with_topicK_ALLdoc.get(i).get(j)+" ");
			}
			System.out.println();
		}
		
		System.out.println("For counter3:");
		for(int i = 0; i<numoftopic; i++){
			System.out.println(Count_for_words_with_topicK_in_ALLdoc.get(i));
		}		
		
	}
	/**
	 * stopwords
	 * with ,:[]
	 * t<Integer>> Count_for_words_with_topicK_in_docJ = new ArrayList<ArrayList<Integer>>();
	private ArrayList<ArrayList<Integer>> Count_for_thisword_with_topicK_ALLdoc = new ArrayList<ArrayList<Integer>>();
	private ArrayList<Integer> Count_for_words_with_topicK_in_ALLdoc = new ArrayList<Integer>();
	 * */
	
	public void GibbsSampling(){
		for(int iter = 0;iter<iterations;iter++){
			System.out.format("======================Gibbs Sampling Iteration %d======================\n",iter);
			for(int i = 0; i < wordindoc.size(); i++){
				for(int j = 0; j < wordindoc.get(i).size(); j++){
					double[] prob_assign_k_to_word = new double[numoftopic];
					for(int k = 0; k < numoftopic; k++){
						//STEP1. pop stack...
						int ori_topic_assign = topicass.get(i).get(j);
						//counter1
						ArrayList<Integer> temp1 = Count_for_words_with_topicK_in_docJ.get(i);
						temp1.set(ori_topic_assign, temp1.get(ori_topic_assign)-1);
						Count_for_words_with_topicK_in_docJ.set(i, temp1);
						//counter2					
						ArrayList<Integer> temp2 = Count_for_thisword_with_topicK_ALLdoc.get(word2id.get(wordindoc.get(i).get(j)));
						temp2.set(ori_topic_assign, temp2.get(ori_topic_assign)-1);
						Count_for_thisword_with_topicK_ALLdoc.set(word2id.get(wordindoc.get(i).get(j)), temp2);
						//counter3					
						Count_for_words_with_topicK_in_ALLdoc.set(ori_topic_assign, Count_for_words_with_topicK_in_ALLdoc.get(ori_topic_assign)-1);
						//STEP2. sampling
						prob_assign_k_to_word[k] = (double)(Count_for_thisword_with_topicK_ALLdoc.get(word2id.get(wordindoc.get(i).get(j))).get(k)+beta)/(Count_for_words_with_topicK_in_ALLdoc.get(k)+totalNumofDistinctWord*beta)
													*(double)(Count_for_words_with_topicK_in_docJ.get(i).get(k)+alpha)/(Count_for_words_with_topicK_in_docJ.get(i).size()-1+numoftopic*alpha);
						
						int new_topic_assign = GibbsGatMax(prob_assign_k_to_word);
						
						ArrayList<Integer> topicasstemp = topicass.get(i);
						topicasstemp.set(j, new_topic_assign);
						topicass.set(i, topicasstemp);
						
						//push stack
						//counter1
						temp1 = Count_for_words_with_topicK_in_docJ.get(i);
						temp1.set(new_topic_assign, temp1.get(new_topic_assign)+1);
						Count_for_words_with_topicK_in_docJ.set(i, temp1);
						//counter2					
						temp2 = Count_for_thisword_with_topicK_ALLdoc.get(word2id.get(wordindoc.get(i).get(j)));
						temp2.set(new_topic_assign, temp2.get(new_topic_assign)+1);
						Count_for_thisword_with_topicK_ALLdoc.set(word2id.get(wordindoc.get(i).get(j)), temp2);
						//counter3					
						Count_for_words_with_topicK_in_ALLdoc.set(new_topic_assign, Count_for_words_with_topicK_in_ALLdoc.get(new_topic_assign)+1);
					}
				}
			}
		}
	}
	private int GibbsGatMax(double[] prob_assign_k_to_word) {
		// TODO Auto-generated method stub
		for(int i = 1; i < prob_assign_k_to_word.length; i++){
			prob_assign_k_to_word[i]+=prob_assign_k_to_word[i-1];
		}
		double randomthreshold = Math.random() * (prob_assign_k_to_word[prob_assign_k_to_word.length-1]);//
		int res = 0;
		for(;res<numoftopic;res++){
			if(prob_assign_k_to_word[res]>randomthreshold){
				return res;
			}
		}
		return res;// the effective return should be the one in that if block!
	}
	private void CalculateThetaandPhi(){
		//calculate Theta from http://u.cs.biu.ac.il/~89-680/darling-lda.pdf
		for(int i = 0; i < wordindoc.size(); i++){
			ArrayList<Double> thetatemp= new ArrayList<Double>();
			for(int k = 0; k < numoftopic; k++){
				thetatemp.add((double)(Count_for_words_with_topicK_in_docJ.get(i).get(k)+alpha)/(wordindoc.get(i).size()+numoftopic*alpha));				
			}
			TheTHETA.add(thetatemp);
		}
		
		//calculate Phi
		for(int i = 0; i < totalNumofDistinctWord; i++){
			ArrayList<Double> phitemp= new ArrayList<Double>();
			for(int k = 0; k < numoftopic; k++){
				phitemp.add((double)(Count_for_thisword_with_topicK_ALLdoc.get(i).get(k)+beta)/(Count_for_words_with_topicK_in_ALLdoc.get(k)+totalNumofDistinctWord*beta));				
			}
			ThePHI.add(phitemp);
		}
	}
	private static List<List<Double>> transpose(ArrayList<ArrayList<Double>> arr2) {
	    List<List<Double>> matrixOut = new ArrayList<List<Double>>();
	    if (!arr2.isEmpty()) {
	        int noOfElementsInList = arr2.get(0).size();
	        for (int i = 0; i < noOfElementsInList; i++) {
	            List<Double> col = new ArrayList<Double>();
	            for (List<Double> row : arr2) {
	                col.add(row.get(i));
	            }
	            matrixOut.add(col);
	        }
	    }

	    return matrixOut;
	}
	public void WriteRes() throws IOException{
		CalculateThetaandPhi();
		List<List<Double>> PHITranspose = new ArrayList<List<Double>>();
		PHITranspose = transpose(ThePHI);
		/*** WRONG CODE: CANNOT DEAL WITH DUPLICATED VALUES.
		//PHI Transpose
		
		
		for(int i = 0; i<topicass.size(); i++){
			for(int j = 0; j < topicass.get(i).size(); j++){
				System.out.print(topicass.get(i).get(j)+" ");
			}
			System.out.println();
		}
		
		//Make a copy for indexing while sorting each inner arraylist.
		ArrayList<ArrayList<Integer>> TopK_ID = new ArrayList<ArrayList<Integer>>();
		for(int k = 0; k < numoftopic; k++){
			ArrayList<Double> tempstore = new ArrayList<Double>(PHITranspose.get(k));
			ArrayList<Integer> tempindex = new ArrayList<Integer>();
			Collections.sort(PHITranspose.get(k),Collections.reverseOrder());
			for(int m = 0; m < topkwords; m++){
				tempindex.add(tempstore.indexOf(PHITranspose.get(k).get(m)));
			}
			TopK_ID.add(tempindex);
		}
		*/
		
		Comparator<Entry<Integer, Double>> comparator = new Hashkey();
		PriorityQueue<Map.Entry<Integer, Double>> pq = new PriorityQueue<Map.Entry<Integer, Double>>(comparator);
		ArrayList<ArrayList<Integer>> TopK_ID = new ArrayList<ArrayList<Integer>>();
		for(int k = 0; k < numoftopic; k++){
			HashMap<Integer,Double> hh = new HashMap<Integer,Double>();
			ArrayList<Integer> tempindex = new ArrayList<Integer>();
			for(int m = 0; m < totalNumofDistinctWord; m++){
				hh.put(m, PHITranspose.get(k).get(m));
			}
			Iterator it = hh.entrySet().iterator();
		    while (it.hasNext()) {
		        pq.offer((Entry<Integer, Double>) it.next());
		    }
		    for(int t = 0; t <  topkwords; t++){
		    	Map.Entry<Integer, Double> e = pq.poll();
		    	tempindex.add(e.getKey());
		    }
		    TopK_ID.add(tempindex);
		}
		//Write top K words for each topic in file.
		File fout = new File(outputFile);
		FileOutputStream fos = new FileOutputStream(fout);
	 
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
		for(int k = 0; k < numoftopic; k++){
			bw.write("====================="+String.format("Top %d words for topic %d", topkwords,k)+"=====================");
			
			bw.newLine();
			for(int m = 0; m < topkwords; m++){
				bw.write(id2word.get(TopK_ID.get(k).get(m)));
				bw.newLine();
			}
			
			
		}
		bw.close();
		
		
	}
}
