package edu.osumc.bmi.aa.kdAlgorithm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;
import edu.osumc.bmi.aa.util.AcaAnaLogger;

/**
 * This class creates clusters of OSU authors together based upon the highest number of 
 * common authors. Essentially, a sparse adjacency matrix is created where only
 * the cells corresponding to author pairs with a cardinality of common authors
 * greater than the threshold are populated.
 * 
 *  M = {(x, y) | |(x , y) | > T}; x, y E {OSUAuthors}
 *  
 * @author cartik
 *
 */
public class AuthorClusterGenerator {
	
	public static Logger log = Logger.getLogger(AuthorClusterGenerator.class);
	
	private static final String COMMA = ",";
	
	private static final String INPUT_ADJ_MATRIX_FILE = "output/divergent/KWWT-1.25-PWT-35.0-DOS-5/adjacencyMatrix.csv";
	private static final String OUTPUT_CLUSTERS_FILE = "output/divergent/KWWT-1.25-PWT-35.0-DOS-5/clusters.csv";
	
	private static final int AUTHOR_COUNT_THRESHOLD = 10; 
	private static final int ITERATION_COUNT = 4;
	
	private Map<List<String>, Set<String>> inputAdjMatrix;
	
	
	private Set<List<String>> setOfAuthorsFinishedWith;
	private Set<String> largestSetOfCommonAuthors;
	
	private Map<List<String>, Set<String>> clusters;
	
	private PrintWriter pw = null;
	
	public AuthorClusterGenerator(){
		log.setLevel(Level.DEBUG);
		
		String[][] author2dArray = readCSVFileIntoMatrix(INPUT_ADJ_MATRIX_FILE);
		inputAdjMatrix = transformKeywordArrayToHashMap(author2dArray);
		setOfAuthorsFinishedWith = new HashSet<List<String>>();
		largestSetOfCommonAuthors = new HashSet<String>();
	}
	
	/**
	 * Reads in a given CSV file into a 2D Array
	 * @param fileName
	 *            - the full path and name of the file
	 * @param cl
	 *            - One of a few possible enumerated values in the enum called
	 *            Classifier
	 * **/
	private String[][] readCSVFileIntoMatrix(String fileName) {
		CSVReader csvReader = null;
		String[][] outputMatrix = null;
		try {
			csvReader = new CSVReader(new FileReader(new File(fileName)));
			List<String[]> list = csvReader.readAll();
			outputMatrix = new String[list.size()][];
			outputMatrix = list.toArray(outputMatrix);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try{
				csvReader.close();
			} catch(IOException ioe){
				log.error("IO EXCEPTION: Problems closing CSV Reader");
				ioe.printStackTrace();
			} finally{
				csvReader = null;
			}
		}
		return outputMatrix;
	}
	
	/** 
	 * Given an input 2D matrix where each line contains a list of authors, 
	 * this method returns a map where the key is the first author and the values
	 * are all the authors that collaborate with the first author
	 * @param array
	 * @return
	 */
	private Map<List<String>, Set<String>> transformKeywordArrayToHashMap(String[][] array) {
		Map<List<String>, Set<String>> hashMap = new HashMap<List<String>, Set<String>>();
		String author1 = "", author2 = ""; 
		Set<String> secondAuthors = null; 
		
		for (int i = 0; i < array.length; i++) {
			List<String> osuAuthorsList = new ArrayList<String>();
			author1 = array[i][0];
			author2 = array[i][1];
			osuAuthorsList.add(author1);
			osuAuthorsList.add(author2);
			secondAuthors = new HashSet<String>();
			for(int j = 3; j < array[i].length; j++){
				secondAuthors.add(array[i][j]);
			}
			hashMap.put(osuAuthorsList, secondAuthors);
		}
		return hashMap;
	}
	
	/**
	 * This method clusters OSU authors using a hierarchical clustering
	 * algorithm equivalent
	 */
	private void clusterAuthors() {
		if(clusters == null){
			findLargestGroupings(inputAdjMatrix);
		} else {
			findLargestGroupings(clusters);
		}
	}

	/**
	 * This method iteratively finds the largest groupings of authors
	 * corresponding to groups of OSU authors
	 * @param hashMap
	 */
	private void findLargestGroupings(Map<List<String>, Set<String>> hashMap) {
		clusters = createClusters(hashMap);
		while(largestSetOfCommonAuthors != null){
			clusters.putAll(createClusters(hashMap));
		}
	}

	/**
	 * This method creates groups of authors that share the most number of
	 * common collaborating authors, a nearest distance clustering method where
	 * the number of common collaborating authors is the distance metric
	 */
	private HashMap<List<String>, Set<String>> 
			createClusters(Map<List<String>, Set<String>> hashMap) {
		HashMap<List<String>, Set<String>> clusters = new HashMap<List<String>, Set<String>>();
		List<String> authorList1, authorList2;
		List<List<String>> listOfListOfFirstAuthors = null;
		List<String> listOfFirstAuthors = null;
		Set<String> setOfCommonAuthors = null;
		List<List<String>> listOfOsuAuthors = new ArrayList<List<String>>(hashMap.keySet());
		int maxCount = AUTHOR_COUNT_THRESHOLD;
		for (int i = 0; i < listOfOsuAuthors.size() - 1; i++) {
			authorList1 = listOfOsuAuthors.get(i);
			if(setOfAuthorsFinishedWith != null && !setOfAuthorsFinishedWith.contains(authorList1)){
				for (int j = i + 1; j < listOfOsuAuthors.size(); j++) {
					authorList2 = listOfOsuAuthors.get(j);
					if(!setOfAuthorsFinishedWith.contains(authorList2)){
						setOfCommonAuthors = findCommonAuthors(authorList1, authorList2, hashMap);
			
						if (setOfCommonAuthors.size() > maxCount) {
							listOfListOfFirstAuthors = new ArrayList<List<String>>();
							listOfListOfFirstAuthors.add(authorList1);
							listOfListOfFirstAuthors.add(authorList2);
							maxCount = setOfCommonAuthors.size();
							largestSetOfCommonAuthors = setOfCommonAuthors;
						} 
					}
				}
			}
		}
		if(listOfListOfFirstAuthors != null){
//			log.debug("Found " + largestSetOfCommonAuthors.size() + " common authors for " + 
//				listOfFirstAuthors.get(0) + " and " + listOfFirstAuthors.get(1));
			setOfAuthorsFinishedWith.addAll(listOfListOfFirstAuthors);
			listOfFirstAuthors = coalesceListOfLists(listOfListOfFirstAuthors);
			clusters.put(listOfFirstAuthors, largestSetOfCommonAuthors);
		} else {
			largestSetOfCommonAuthors = null;
		}
	//	}
		return clusters;
	}

	/**
	 * Given two sets of collaborators, this method finds the 
	 * intersection
	 * @param authorList1
	 * @param authorList2
	 * @return
	 */
	private Set<String> findCommonAuthors(List<String> authorList1, List<String> authorList2, 
			Map<List<String>, Set<String>> hashMap) {
		Set<String> commonAuthors = new TreeSet<String>();
		Set<String> collabsForAuthor1 = hashMap.get(authorList1);
		Set<String> collabsForAuthor2 = hashMap.get(authorList2);
		
		Iterator<String> cIt = collabsForAuthor1.iterator();
		while(cIt.hasNext()){
			String collab = cIt.next();
			
			if(collabsForAuthor2.contains(collab)){
				commonAuthors.add(collab);
			}
		}
		return commonAuthors;
	}

	/**
	 * A method to convert a key - value entry to a comma-delimited string, that includes
	 * the length of the values set
	 * @param key
	 * @param values
	 */
	private void printCluster(Map<List<String>, Set<String>> hashMap) {
		String line = "";
		for(Map.Entry<List<String>, Set<String>> entry : hashMap.entrySet()){
			List<String> key = entry.getKey();
			Set<String> values = entry.getValue();
			int count = values.size();
			line = iterativelyConvertToString(key);
			line += count + COMMA;
			line += iterativelyConvertToString(values);
			line = line.substring(0, line.length() - 1);
			log.debug("Writing " + line + " to file");
			pw.println(line);
		}
	}

	/**
	 * Converts a list of lists to one simple list
	 * @param keyList
	 * @return
	 */
	private List<String> coalesceListOfLists(List<List<String>> keyList) {
		List<String> coalescedList = new ArrayList<String>();
		for(List<String> key : keyList){
			coalescedList.addAll(key);
		}
		return coalescedList;
	}

	private String iterativelyConvertToString(Collection<String> coll) {
		String line = "";
		Iterator<String> iter = coll.iterator();
		while(iter.hasNext()){
			line += iter.next() + COMMA;
		}
		return line;
	}

	/**
	 * The method that calls all the other methods in sequence
	 */
	private void run() {
		for(int i = 0; i < ITERATION_COUNT; i++){
			clusterAuthors();	
		}
		try {
			pw = new PrintWriter(new File(OUTPUT_CLUSTERS_FILE));
			printCluster(clusters);
			log.debug("Finished printing to file");
		} catch (FileNotFoundException e) {
			log.error("FileNotFoundException: Problems opening output file to write adjacency matrix");
			e.printStackTrace();
		} finally {
			if (pw != null){
				pw.flush();
				pw.close();
			}
		}
	}
	
	/**
	 * Tester method
	 * TODO JUnit Test 
	 * @param args
	 */
	public static void main(String[] args){
		AcaAnaLogger.initLogger();
		long startTime = System.currentTimeMillis();
		AuthorClusterGenerator cl = new AuthorClusterGenerator();
		cl.run();
		
		long endTime = System.currentTimeMillis();
		long execTimeInSeconds = (endTime - startTime) / 1000;
		log.info("Execution time: " + execTimeInSeconds + " seconds");
		
	}
	
}
