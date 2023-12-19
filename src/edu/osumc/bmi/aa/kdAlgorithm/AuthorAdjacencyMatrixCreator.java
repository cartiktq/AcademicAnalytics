package edu.osumc.bmi.aa.kdAlgorithm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
 * This class pairs OSU authors together based upon the highest number of 
 * common authors. Essentially, a sparse adjacency matrix is created where only
 * the cells corresponding to author pairs with a cardinality of common authors
 * greater than the threshold are populated.
 * 
 *  M = {(x, y) | |(x , y) | > T}; x, y E {OSUAuthors}
 *  
 * @author cartik
 *
 */
public class AuthorAdjacencyMatrixCreator {
	
	public static Logger log = Logger.getLogger(AuthorAdjacencyMatrixCreator.class);
	
	private static final String COMMA = ",";
	
	private static final String INPUT_COLLAB_FILE = "output/divergent/KWWT-1.75-PWT-35.0-DOS-5/collaborating-authors.csv";
	private static final String OUTPUT_ADJ_MATRIX_FILE = "output/divergent/KWWT-1.75-PWT-35.0-DOS-5/adjacencyMatrix.csv";
	
	private static final int AUTHOR_COUNT_THRESHOLD = 10; 
	
	private Map<String, Set<String>> inputCollaborations;
	
	private List<Integer> listOfCountOfCommonAuthors;
	
	private Set<String> setOfAuthorsFinishedWith;
	private Set<String> largestSetOfCommonAuthors;
	
	private Map<List<String>, Set<String>> clusters;
	
	private PrintWriter pw = null;
	
	public AuthorAdjacencyMatrixCreator(){
		log.setLevel(Level.DEBUG);
		
		String[][] author2dArray = readCSVFileIntoMatrix(INPUT_COLLAB_FILE);
		inputCollaborations = transformKeywordArrayToHashMap(author2dArray);
		listOfCountOfCommonAuthors = new ArrayList<Integer>();
		setOfAuthorsFinishedWith = new HashSet<String>();
		largestSetOfCommonAuthors = new HashSet<String>();
		clusters = new HashMap<List<String>, Set<String>>();
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
	private Map<String, Set<String>> transformKeywordArrayToHashMap(String[][] array) {
		Map<String, Set<String>> hashMap = new HashMap<String, Set<String>>();
		String startAuthor = ""; 
		Set<String> secondAuthors = null; 
		
		for (int i = 0; i < array.length; i++) {
			startAuthor = array[i][0];
			secondAuthors = new HashSet<String>();
			for(int j = 1; j < array[i].length; j++){
				secondAuthors.add(array[i][j]);
			}
			hashMap.put(startAuthor, secondAuthors);
		}
		return hashMap;
	}
	
	/**
	 * This method clusters OSU authors using a hierarchical clustering
	 * algorithm equivalent
	 */
	private void clusterAuthors() {
		clusters.putAll(createClusters(inputCollaborations.keySet()));
		while(largestSetOfCommonAuthors != null){
			clusterAuthors();
		} 
	}

	/**
	 * This method creates groups of authors that share the most number of
	 * common collaborating authors, a nearest distance clustering method where
	 * the number of common collaborating authors is the distance metric
	 */
	private HashMap<List<String>, Set<String>> 
			createClusters(Set<String> inputSetOfFirstAuthors) {
		HashMap<List<String>, Set<String>> clusters = new HashMap<List<String>, Set<String>>();
		String author1, author2;
		List<String> listOfFirstAuthors = null;
		Set<String> setOfCommonAuthors = null;
		List<String> listOfOsuAuthors = new ArrayList<String>(inputSetOfFirstAuthors);
		int maxCount = AUTHOR_COUNT_THRESHOLD;
		for (int i = 0; i < listOfOsuAuthors.size() - 1; i++) {
			author1 = listOfOsuAuthors.get(i);
			if(setOfAuthorsFinishedWith != null && !setOfAuthorsFinishedWith.contains(author1)){
				for (int j = i + 1; j < listOfOsuAuthors.size(); j++) {
					author2 = listOfOsuAuthors.get(j);
					if(!setOfAuthorsFinishedWith.contains(author2)){
						setOfCommonAuthors = findCommonAuthors(author1, author2);
				
						if (setOfCommonAuthors.size() > maxCount) {
							listOfFirstAuthors = new ArrayList<String>();
							listOfFirstAuthors.add(author1);
							listOfFirstAuthors.add(author2);
							maxCount = setOfCommonAuthors.size();
							largestSetOfCommonAuthors = setOfCommonAuthors;
						} 
					}
				}
			}
		}
		if(listOfFirstAuthors != null){
//			log.debug("Found " + largestSetOfCommonAuthors.size() + " common authors for " + 
//				listOfFirstAuthors.get(0) + " and " + listOfFirstAuthors.get(1));
			setOfAuthorsFinishedWith.addAll(listOfFirstAuthors);
			clusters.put(listOfFirstAuthors, largestSetOfCommonAuthors);
			printCluster(listOfFirstAuthors, largestSetOfCommonAuthors);
		} else {
			largestSetOfCommonAuthors = null;
		}
		return clusters;
	}

	/**
	 * Given two sets of collaborators, this method finds the 
	 * intersection
	 * @param author1
	 * @param author2
	 * @return
	 */
	private Set<String> findCommonAuthors(String author1, String author2) {
		Set<String> commonAuthors = new TreeSet<String>();
		Set<String> collabsForAuthor1 = inputCollaborations.get(author1);
		Set<String> collabsForAuthor2 = inputCollaborations.get(author2);
		
		Iterator<String> cIt = collabsForAuthor1.iterator();
		while(cIt.hasNext()){
			String collab = cIt.next();
			if(collabsForAuthor2.contains(collab)){
				commonAuthors.add(collab);
			}
		}
		return commonAuthors;
	}

	private void printCluster(List<String> key, Set<String> values) {
		String line = "";
		int count = values.size();
		listOfCountOfCommonAuthors.add(count);
		line = iterativelyConvertToString(key);
		line += count + COMMA;
		line += iterativelyConvertToString(values);
		line = line.substring(0, line.length() - 1);
		log.debug("Writing " + line + " to file");
		pw.println(line);
	}

	private String iterativelyConvertToString(Collection<String> coll) {
		String line = "";
		Iterator<String> iter = coll.iterator();
		while(iter.hasNext()){
			line += iter.next() + COMMA;
		}
		return line;
	}

	private void computeCommonAuthorCountStats(){
		int median = 0, max, min, midpoint, size;
		
		Collections.sort(listOfCountOfCommonAuthors);
		size = listOfCountOfCommonAuthors.size();
		if(listOfCountOfCommonAuthors.size() > 1){
			max = listOfCountOfCommonAuthors.get(size - 1);
		} else {
			max = listOfCountOfCommonAuthors.get(0);
		}
		min = listOfCountOfCommonAuthors.get(0);
		
		if(size % 2 == 0){
			midpoint = size/2;
			median = (listOfCountOfCommonAuthors.get(midpoint) + listOfCountOfCommonAuthors.get(midpoint - 1))/2;
		} else {
			midpoint = (size - 1)/2;
			median = listOfCountOfCommonAuthors.get(midpoint);
		}
		
		log.info(max + COMMA + min + COMMA + median);
	}
	
	/**
	 * The method that calls all the other methods in sequence
	 */
	private void run(boolean computeStats) {
		try {
			pw = new PrintWriter(new File(OUTPUT_ADJ_MATRIX_FILE));
			clusterAuthors();	
			log.debug("Finished printing to file");
		} catch (FileNotFoundException e) {
			log.error("FileNotFoundException: Problems opening output file to write adjacency matrix");
			e.printStackTrace();
		} finally {
			if (pw != null){
				pw.flush();
				pw.close();
				if(computeStats){
					computeCommonAuthorCountStats();
				}
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
		boolean computeStats = Boolean.parseBoolean(args[0]);
		long startTime = System.currentTimeMillis();
		AuthorAdjacencyMatrixCreator cl = new AuthorAdjacencyMatrixCreator();
		cl.run(computeStats);
		
		long endTime = System.currentTimeMillis();
		long execTimeInSeconds = (endTime - startTime) / 1000;
		log.info("Execution time: " + execTimeInSeconds + " seconds");
		
	}
	
}
