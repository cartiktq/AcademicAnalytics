/**
 * This is the class that generates the paths through the graph of
 * authors and keywords
 * @author Cartik
 * Dependencies: 
 * 1. AuthorKeywordDataProcessor to run first. (dataPipeline package)
 * 2. LocHierarchy to run next (loc package).
 * 
 */

package edu.osumc.bmi.aa.kdAlgorithm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.osumc.bmi.aa.loc.LocHierarchy;
import edu.osumc.bmi.aa.util.AcaAnaLogger;
import au.com.bytecode.opencsv.CSVReader;

public class PathFinder{

	public static Logger log = Logger.getLogger(PathFinder.class);

	private static final String COMMA = ",";
	private static final String BLANK = "(blank)";
	
	private static final String OUTPUT_STR = "output";
	private static final String DIRPATH_SEPARATOR = "/";
	private static final String PATHS_OUTPUT_FILE_STR = "collabsFor";
	private static final String COLLAB_PAIRS_FILE_STR = "collaborating-authors";
	private static final String STRONGER_COLLAB_PAIRS_STR = "strongerCollabs";

	private static final String NON_DIVERGENT_STR = "non-divergent";
	private static final String DIVERGENT_STR = "divergent";
	
	private static final String INPUT_K2A_FILE = "data/KeywordsToAuthors.csv";
	private static final String INPUT_A2K_FILE = "data/AuthorsToKeywords.csv";
	
	private static final String INPUT_KEYWORD_ANNOTATIONS_FILE = "data/KeywordAnnotations.csv";

	private static final String CSV_FILE_EXTN = ".csv";
	private static final String OSU_AUTHOR_PATTERN = "^GRT[0-9]+";
	
	private static final int DEGREES_OF_SEPARATION = 5;
	private static final double PATH_WEIGHTAGE_THRESHOLD = 35.0;
	private static final double KEYWORD_WEIGHTAGE_THRESHOLD = 1.25;

	private String OUTPUT_PATHS_DIR_LOC = "";
	private String OUTPUT_COLLAB_AUTHORS_DIR_LOC = "";
	private String STRONGER_COLLAB_AUTHORS_DIR_LOC = "";
			
	private LocHierarchy loch;
	private Map<String, List<String>> a2kMap, k2aMap;
	private Set<String> setOfAuthorsFinishedWith;
	private Map<String, List<String>> mapOfKeywordsToWeightAndHier;

	private List<String> collaboratingAuthorList;
	private List<String> listOfKeywordsFinishedWith;
	
	/**
	 * A flag to indicate if the search needs to avoid using keywords from the same
	 * hierarchy i.e. a more divergent search
	 */
	private boolean useDivergentKeywords = false;
	
	private PrintWriter pwForPaths = null;
	private PrintWriter pwForCollabs = null;
	private PrintWriter pwForStrongerCollabs = null;
	
	static {
		log.setLevel(Level.DEBUG);
	}

	public PathFinder(boolean useDivergentKeywords) {
		String[][] a2kArray = readCSVFileIntoMatrix(INPUT_A2K_FILE);
		String[][] k2aArray = readCSVFileIntoMatrix(INPUT_K2A_FILE);

		String[][] usefulKeywordsArray = readCSVFileIntoMatrix(INPUT_KEYWORD_ANNOTATIONS_FILE);
				
		a2kMap = sortValuesInMap(transformArrayToHashMap(a2kArray));
		k2aMap = sortValuesInMap(transformArrayToHashMap(k2aArray));
		log.info("Obtained keywordToAuthorsMap with " + k2aMap.size() + " keywords!");
		log.info("Obtained authorToKeywordsMap with " + a2kMap.size() + " authors!");
		
		loch = new LocHierarchy();
		
		mapOfKeywordsToWeightAndHier = transformKeywordArrayToHashMap(usefulKeywordsArray);
		setOfAuthorsFinishedWith = new HashSet<String>();
		this.useDivergentKeywords = useDivergentKeywords;
		
		if(useDivergentKeywords){
			this.OUTPUT_PATHS_DIR_LOC = OUTPUT_STR + DIRPATH_SEPARATOR + DIVERGENT_STR + DIRPATH_SEPARATOR + 
					"KWWT-" + KEYWORD_WEIGHTAGE_THRESHOLD + "-PWT-" + 
					PATH_WEIGHTAGE_THRESHOLD + "-DOS-" + DEGREES_OF_SEPARATION + 
					DIRPATH_SEPARATOR;
			this.OUTPUT_COLLAB_AUTHORS_DIR_LOC = OUTPUT_STR + DIRPATH_SEPARATOR + DIVERGENT_STR + DIRPATH_SEPARATOR +
					"KWWT-" + KEYWORD_WEIGHTAGE_THRESHOLD + "-PWT-" + 
					PATH_WEIGHTAGE_THRESHOLD + "-DOS-" + DEGREES_OF_SEPARATION + 
					DIRPATH_SEPARATOR +
					COLLAB_PAIRS_FILE_STR + CSV_FILE_EXTN;
			this.STRONGER_COLLAB_AUTHORS_DIR_LOC = OUTPUT_STR + DIRPATH_SEPARATOR + DIVERGENT_STR + DIRPATH_SEPARATOR +
					"KWWT-" + KEYWORD_WEIGHTAGE_THRESHOLD + "-PWT-" + 
					PATH_WEIGHTAGE_THRESHOLD + "-DOS-" + DEGREES_OF_SEPARATION + 
					DIRPATH_SEPARATOR + 
					STRONGER_COLLAB_PAIRS_STR + CSV_FILE_EXTN;
		}
		else {
			this.OUTPUT_PATHS_DIR_LOC = OUTPUT_STR + DIRPATH_SEPARATOR + NON_DIVERGENT_STR + DIRPATH_SEPARATOR + 
					"KWWT-" + KEYWORD_WEIGHTAGE_THRESHOLD + "-PWT-" + 
					PATH_WEIGHTAGE_THRESHOLD + "-DOS-" + DEGREES_OF_SEPARATION + 
					DIRPATH_SEPARATOR;
			this.OUTPUT_COLLAB_AUTHORS_DIR_LOC = OUTPUT_STR + DIRPATH_SEPARATOR + NON_DIVERGENT_STR + DIRPATH_SEPARATOR +
					"KWWT-" + KEYWORD_WEIGHTAGE_THRESHOLD + "-PWT-" + 
					PATH_WEIGHTAGE_THRESHOLD + "-DOS-" + DEGREES_OF_SEPARATION + 
					DIRPATH_SEPARATOR + 
					COLLAB_PAIRS_FILE_STR + CSV_FILE_EXTN;
			this.STRONGER_COLLAB_AUTHORS_DIR_LOC = OUTPUT_STR + DIRPATH_SEPARATOR + NON_DIVERGENT_STR + DIRPATH_SEPARATOR +
					"KWWT-" + KEYWORD_WEIGHTAGE_THRESHOLD + "-PWT-" + 
					PATH_WEIGHTAGE_THRESHOLD + "-DOS-" + DEGREES_OF_SEPARATION + 
					DIRPATH_SEPARATOR +
					STRONGER_COLLAB_PAIRS_STR + CSV_FILE_EXTN;
		}
	}
	
	/**
	 * Reads in a given CSV file into a 2D Array
	 * 
	 * @param fileName
	 *            - the full path and name of the file
	 * @param cl
	 *            - One of a few possible enumerated values in the enum called
	 *            Classifier
	 * 
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
	 * This method is used to transform a 2D array into a hashmap where the
	 * first column is the key and the remaining columns are mapped to values of
	 * that key
	 * 
	 * @param array
	 * @return
	 */

	private Map<String, Set<String>> transformArrayToHashMap(String[][] array) {
		Map<String, Set<String>> hashMap = new HashMap<String, Set<String>>();
		Set<String> values;
		String key;
		int valueCount;
		for (int i = 0; i < array.length; i++) {
			key = array[i][0];
			valueCount = Integer.parseInt(array[i][1].trim());
			if(valueCount <= 1){
				continue;
			}
			values = new HashSet<String>();
			for (int j = 2; j < array[i].length; j++) {
				values.add(array[i][j]);
			}
			hashMap.put(key, values);
		}

		return hashMap;
	}

	private Map<String, List<String>> transformKeywordArrayToHashMap(String[][] array) {
		Map<String, List<String>> hashMap = new HashMap<String, List<String>>();
		String relation = ""; 
		String weight = "";
		String hier = "";
		
		for (int i = 0; i < array.length; i++) {
			relation = array[i][0];
			String[] components = relation.split(";");
			if(components.length == 3){
				String kw = components[0];
				List<String> weightAndHierList = new ArrayList<String>();
				weight = components[1];
				hier = components[2];
				weightAndHierList.add(weight);
				weightAndHierList.add(hier);
				hashMap.put(kw, weightAndHierList);
			}
		}
		return hashMap;
	}
	
	/**
	 * Method simply sorts the collection of values in the map by alphabetical
	 * order. May be modified in future or dispensed with.
	 * 
	 * @param keyword2AuthorsMap
	 * @return
	 */
	private Map<String, List<String>> sortValuesInMap(
			Map<String, Set<String>> keyword2AuthorsMap) {
		Map<String, List<String>> newMap = new HashMap<String, List<String>>();
		for (Entry<String, Set<String>> entry : keyword2AuthorsMap.entrySet()) {
			newMap.put(entry.getKey(), sortStringsInSet(entry.getValue()));
		}

		return newMap;
	}

	/**
	 * A helper method to sort the words in a set
	 * 
	 * @return - a sorted list of keywords
	 */
	private List<String> sortStringsInSet(Set<String> setOfStrings) {
		List<String> stringList = null;
		String[] stringArray = new String[setOfStrings.size()];
		stringArray = setOfStrings.toArray(stringArray);
		stringList = Arrays.asList(stringArray);
		Collections.sort(stringList);

		return stringList;
	}

	/**
	 * This method explores paths in the graph starting with authors 
	 * with ID regex patterns /^GRT[0-9]+$/ and prints those paths
	 * to file
	 */
	private void printCompletePathsForEveryAuthorToFile() {
		for (String author : a2kMap.keySet()) {
			// Every path needs to start with an OSU researcher
			if (!author.equals(BLANK) && author.matches(OSU_AUTHOR_PATTERN)) {
				generatePathsForAuthor(author);
				if(collaboratingAuthorList.size() > 1){
					log.debug("Writing collaborations for " + author);
					pwForCollabs.println(convertCollaboratingAuthorListToCommaDelimitedString(collaboratingAuthorList));
					printStrongerCollaborationsToFile(collaboratingAuthorList);
				}
				setOfAuthorsFinishedWith.add(author);
			}
		}
	}

	/**
	 * Find pairs of authors who occur together more than once at the ends
	 * of paths explored by the algorithm and add them to file.
	 * @param collaboratingAuthorList2
	 */
	private void printStrongerCollaborationsToFile(List<String> authorList) {
		int listSize = authorList.size();
		int occurrenceCount = 1;
		String firstAuthor = authorList.get(0);
		List<String> secondAuthorList = authorList.subList(1, listSize);
		
		Collections.sort(secondAuthorList);
		String currentAuthor = secondAuthorList.get(0);
		
		for(int i = 1; i < listSize - 1; i++){
			if(secondAuthorList.get(i).equals(currentAuthor)){
				++occurrenceCount;
			} else {
				if(occurrenceCount > 1){
					pwForStrongerCollabs.println(firstAuthor + "," + currentAuthor + "," + occurrenceCount);
					occurrenceCount = 1;
					currentAuthor = secondAuthorList.get(i);
				}
			}
		}
	}

	/**
	 * A method to convert a list of collaborating authors to a comma delimited string
	 * after eliminating duplicates
	 * @param list
	 * @return
	 */
	private String convertCollaboratingAuthorListToCommaDelimitedString(List<String> list) {
		String firstAuthor = list.get(0);
		Set<String> set = new TreeSet<String>(list.subList(1, list.size())); //eliminate duplicates
		List<String> sortedList = new ArrayList<String>();
		sortedList.add(firstAuthor);
		sortedList.addAll(set);
		log.debug("Writing " + sortedList.size() + " collaborating authors for " + firstAuthor);
		String cds = "";
		Iterator<String> sit = sortedList.iterator(); 
		while(sit.hasNext()){
			cds += sit.next() + ",";
		}
		cds = cds.substring(0, cds.length() - 1);
		return cds;
	}


	/**
	 * This method generates all the paths for the given author The generated
	 * paths are written to an output file
	 * 
	 * @param author
	 */
	private void generatePathsForAuthor(String author) {
		List<String> path = new ArrayList<String>();
		String fileStr = OUTPUT_PATHS_DIR_LOC + PATHS_OUTPUT_FILE_STR + 
						author + CSV_FILE_EXTN;
		collaboratingAuthorList = new ArrayList<String>();
		listOfKeywordsFinishedWith = new ArrayList<String>();
		collaboratingAuthorList.add(author);
		File outputFile = new File(fileStr);
		try {
			pwForPaths = new PrintWriter(outputFile);
			path.add(author);
			extendPath(path, 0);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally{
			if(pwForPaths != null){
				pwForPaths.flush();
				pwForPaths.close();
			}
		}
	}

	/**
	 * The most crucial method in the class. Extends paths recursively with valid 
	 * author-keyword combinations. If the path is complete (adequate number of
	 * keywords), it invokes a method to write the path to file
	 * @param path
	 * @param dosCount
	 */
	private void extendPath(List<String> path, int dosCount){
		List<String> authorListForKw, kwListForAuthor, extendedPath;
		Iterator<String> kwIter;
		String authorToExtend = path.get(path.size() - 1);
		String pathRep = "", lastAuthor = "";
		
		if(dosCount == DEGREES_OF_SEPARATION){
			pathRep = pathToString(path).trim();
			if(pathRep.length() > 0){
				pwForPaths.println(pathRep);
				lastAuthor = path.get(path.size() - 1);
				collaboratingAuthorList.add(lastAuthor);
			}
			return;
		}
		
		kwListForAuthor = a2kMap.get(authorToExtend);
		if(kwListForAuthor == null){
			return;
		}
		
		kwIter = kwListForAuthor.iterator();
	
		while (kwIter.hasNext()) {
			String kw = kwIter.next();
			if (kwIsValid(path, kw)) {
				authorListForKw = k2aMap.get(kw);
				if(authorListForKw == null){
					continue;
				}
				for (String nextAuthor : authorListForKw) {
					if (nextAuthorIsValid(path, nextAuthor)) {
						extendedPath = new ArrayList<String>();
						int localDosCount = dosCount;
						extendedPath.addAll(path);
						extendedPath.add(kw);
						addToKeywordListIfNecessary(kw);
						extendedPath.add(nextAuthor);
						extendPath(extendedPath, ++localDosCount);
					}
				}
			}
		}
	}

	/**
	 * Add non-duplicate keywords to list that contains list of keywords
	 * that we have finished expanding upon
	 * @param kw
	 */
	private void addToKeywordListIfNecessary(String kw) {
		if(!listOfKeywordsFinishedWith.contains(kw)){
			listOfKeywordsFinishedWith.add(kw);
		}
	}

	/**
	 * To check a host of conditions for keyword
	 * @param path
	 * @param kw
	 * @return
	 */
	private boolean kwIsValid(List<String> path, String kw) {
		boolean keywordIsBasicallyValid = mapOfKeywordsToWeightAndHier.keySet().contains(kw) && !path.contains(kw) &&
				Double.parseDouble(mapOfKeywordsToWeightAndHier.get(kw).get(0)) >= KEYWORD_WEIGHTAGE_THRESHOLD;
		if(keywordIsBasicallyValid){		
			if(useDivergentKeywords){ //every keyword must come from a different hierarchy
				if(listOfKeywordsFinishedWith.size() > 1){
					String heading1 = findHeadingForKeyword(kw);
					for(int previousKeywordIndex = 0; 
							previousKeywordIndex < listOfKeywordsFinishedWith.size(); previousKeywordIndex++){
						String previousKw = listOfKeywordsFinishedWith.get(previousKeywordIndex);
						String heading2 = findHeadingForKeyword(previousKw);
						if(loch.computeSemanticDistanceBetweenHeadings(heading1, heading2) == 0){
							return false;
						}
					}
					return true;
				}
				return true;
			}
			return true;
		} 
		return false;
	}

	/**
	 * A method to make sure the next author is: a) not a blank, b) not 
	 * on the path already, c) not an author we have seen before, and d) not an
	 * OSU author either
	 * 
	 * @param author
	 * @param path
	 * @param kw
	 * @return
	 */
	private boolean nextAuthorIsValid(List<String> path, String nextAuthor) {
		return (!nextAuthor.equals(BLANK) && !path.contains(nextAuthor)
				&& !setOfAuthorsFinishedWith.contains(nextAuthor) && 
				!nextAuthor.matches(OSU_AUTHOR_PATTERN));
	}

	/**
	 * Method to generate a text representation of a generated path Format is
	 * [AUTHOR]->[KEYWORD1]->[AUTHOR2]->[KEYWORD2]->...
	 * If path weight (metric) is less than the preset threshold, a blank string
	 * is returned
	 * @param wordList
	 * @return
	 */
	private String pathToString(List<String> wordList) {
		int lastIndexOfSeparator = 0;
		double pathMetric = 0.0;
		double weight = 0.0;
		String line = "";
		for (int i = 0; i < wordList.size(); i++) {
			String word = wordList.get(i);
			if(mapOfKeywordsToWeightAndHier.containsKey(word)){
				weight = Double.parseDouble(mapOfKeywordsToWeightAndHier.get(word).get(0));
				String heading1 = findHeadingForKeyword(word);
				pathMetric += weight;
				if(i - 2 >= 1 && mapOfKeywordsToWeightAndHier.containsKey(wordList.get(i - 2))){
					String previousWord = wordList.get(i - 2);
					String heading2 = findHeadingForKeyword(previousWord);
					pathMetric += loch.computeSemanticDistanceBetweenHeadings(heading1, heading2);
				}
			}
			line += word + COMMA;
		}
		lastIndexOfSeparator = line.lastIndexOf(COMMA);
		if(pathMetric >= PATH_WEIGHTAGE_THRESHOLD){
			line = pathMetric + "," + line.substring(0, lastIndexOfSeparator);
		} else {
			line = "";
		}
		return line;
	}

	/**
	 * This method finds the heading a keyword is classified under
	 * @param word
	 * @return
	 */
	private String findHeadingForKeyword(String word) {
		String heading = "";
		if(mapOfKeywordsToWeightAndHier.containsKey(word)){
			List<String> weightAndHierList = mapOfKeywordsToWeightAndHier.get(word);
			String completeHierarchy = weightAndHierList.get(1);
			String[] completeHierarchyComponents = completeHierarchy.split(":");
			heading = completeHierarchyComponents[0];
		}
		return heading;
	}

	/**
	 * This method deletes ALL files from the 
	 * output directory. If directory does not exist,
	 * creates it
	 */
	private void cleanUpOutputDir(){
		File dir = new File(OUTPUT_PATHS_DIR_LOC);
		if(!dir.exists()){
			dir.mkdir();
		}
		for(File file : dir.listFiles()){
				file.delete();
		}
	}

	/**
	 * This method eliminates all the empty files from
	 * the output directory
	 */
	private void cleanUpOutputDirOfEmptyFiles(){
		File dir = new File(OUTPUT_PATHS_DIR_LOC);
		for(File file : dir.listFiles()){
			if(file.length() == 0){
				file.delete();
			}
		}
		int numberOfFiles = dir.listFiles().length;
		log.info(numberOfFiles + " files written to output! Done!!");
	}

	/**
	 * The main method of this class
	 */
	public void run() {
		cleanUpOutputDir();
		log.info("Beginning to explore collaborations for authors...");
		try{
			pwForCollabs = new PrintWriter(new File(OUTPUT_COLLAB_AUTHORS_DIR_LOC));
			pwForStrongerCollabs = new PrintWriter(new File(STRONGER_COLLAB_AUTHORS_DIR_LOC));
			printCompletePathsForEveryAuthorToFile();
			cleanUpOutputDirOfEmptyFiles();
		} catch(IOException ioe){
			log.error("IOException: Problems opening file for writing collaborating author pairs");
			ioe.printStackTrace();
		} finally{
			if (pwForCollabs != null){
				pwForCollabs.flush();
				pwForCollabs.close();
			}
			if(pwForStrongerCollabs != null){
				pwForStrongerCollabs.flush();
				pwForStrongerCollabs.close();
			}
		}
	}

	public static void main(String[] args) {
		AcaAnaLogger.initLogger();
		
		PathFinder finder;
		Scanner scanIn = new Scanner(System.in);
		String commandLineInput;

		System.out.println("Are you sure you want to do this? Every file will be overwritten!!");
		System.out.println("It will be hours before the new files are created and completed!!");
		System.out.print("ARE YOU SURE? (YES/NO): ");
		commandLineInput = scanIn.nextLine();
		if(commandLineInput.matches("(^Y.*)|(^y.*)")){ //TO AVOID ACCIDENTAL OVERWRITING
		
			scanIn.close();
			
			long startTime = System.currentTimeMillis();
			
			boolean useDivergentKeywords = Boolean.parseBoolean(args[0]);
			finder = new PathFinder(useDivergentKeywords);
			finder.run();

			long endTime = System.currentTimeMillis();
			long execTimeInSeconds = (endTime - startTime) / 1000;
			log.info("Execution time: " + execTimeInSeconds + " seconds");
		}
	}

}
