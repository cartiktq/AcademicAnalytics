package edu.osumc.bmi.aa.dataPipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.osumc.bmi.aa.nlp.KeywordLemmatizer;
import edu.osumc.bmi.aa.nlp.Spelling;
import edu.osumc.bmi.aa.util.AcaAnaLogger;
import au.com.bytecode.opencsv.CSVReader;

/**
 * This class takes in input from CSV files that contain info
 * relating authors to the keywords they are associated with and 
 * puts that information in a HashMap
 * @author sara26
 *
 */

public class AuthorKeywordDataProcessor {
	
	public static Logger log = Logger.getLogger(AuthorKeywordDataProcessor.class);
	public static Set<String> setOfInvalidWords;

	public static final String OUTPUT_KEYWORDS_FILE = "data/KeywordsToAuthors.csv";
	public static final String OUTPUT_AUTHORS_FILE = "data/AuthorsToKeywords.csv";
	public static final String INPUT_AASEMSIGS_FILE = "data/AAsemsigs(1perGrpXpersXunit).csv";
	public static final String INPUT_OSUFRLONG_FILE = "data/OSURFlong.csv";
	public static final String INPUT_GRANTSLONG_FILE = "data/grantslong.csv";
	public static final String INPUT_TEDLONG_FILE = "data/tedlong1.csv";
	public static final String INPUT_INVALID_KEYWORDS_FILE = "data/InvalidWords.txt";
	public static final String INPUT_INFLECTIONS_FILE = "data/MisspellingsAndInflections.txt";
	
	public static final int MINIMUM_WORD_LENGTH = 4;
	
	private Spelling spellChecker;
	private KeywordLemmatizer lemmatizer;
	
	/** This is a utility map created to avoid sending multiple String arguments to a method
	 * which compiles a list of keywords with associated researchers and the frequencies of usage
	 * of these keywords in their work. A few example entries in this compilation would be <br>
	 * "Evolution -> Carl Zimmer" <br>
	 * "Cypriniformes -> Brian Sidlauskas" <br>
	 * Each entry comprises a keyword, a personId, and a relative frequency**/
	private enum ComponentsEnum {KEYWORD, PERSONID, RELFREQ};
	
	/** A map that relates each keyword to a list of researchers. The list of researchers on the value
	 * end is implemented as a Map that relates the ID of the researcher to the keyword
	 * A few example entries in this compilation would be <br>
	 * "Evolution -> Carl Zimmer" <br>
	 * "Cypriniformes -> Brian Sidlauskas" <br>
	 * Each entry comprises a keyword, a personId, and a relative frequency**/
	private Map<String, Set<String>> keyword2AuthorsMap;
	
	private Map<String, Set<String>> author2KeywordsMap;
	
	private String[][] dataMatrixForResearchers;
	private Set<String> setOfAuthors;
	
	static{
		setOfInvalidWords = createSetOfInvalidWordsFromFile();
		log.setLevel(Level.INFO);
	}
	
	public AuthorKeywordDataProcessor(){
		dataMatrixForResearchers = new String[2][2];
		keyword2AuthorsMap = new HashMap<String, Set<String>>();
		author2KeywordsMap = new HashMap<String, Set<String>>();
		setOfAuthors = new TreeSet<String>();
		lemmatizer = new KeywordLemmatizer();
		
		try {
			spellChecker = new Spelling(Spelling.BIG_TEXT_FILE);
		} catch (IOException e) {
			log.error("IOException: Problems instantiating the spell checker");
			e.printStackTrace();
		}
		
	}
	
	/** GETTER **/
	public Map<String, Set<String>> getAuthor2KeywordsMap() {
		return author2KeywordsMap;
	}

	/** GETTER**/
	public Map<String, Set<String>> getKeyword2AuthorsMap() {
			return keyword2AuthorsMap;
	}
	
	/** GETTER 	 **/
	public Set<String> getSetOfAuthors() {
		return setOfAuthors;
	}
	
	/**
	 * Read in invalid words from a text file
	 * @return - a set of invalid words
	 */
	public static Set<String> createSetOfInvalidWordsFromFile() {
		Set<String> setOfInvalidWords = new HashSet<String>();
		String line = "";
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(INPUT_INVALID_KEYWORDS_FILE)));
			while((line = br.readLine()) != null){
				setOfInvalidWords.add(line);
			}
		} catch (FileNotFoundException e) {
			String errMsg = "Unable to find invalid words file at the given location";
			log.error(errMsg);
			e.printStackTrace();
		} catch (IOException e) {
			String errMsg = "Unable to read from invalid words file";
			log.error(errMsg);
			e.printStackTrace();
		} finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {
					log.error("Unable to close buffered reader");
					e.printStackTrace();
				}
			}
		}
		
		return setOfInvalidWords;
	}
	
	/** Reads in a given CSV file into a 2D Array
	 * @param fileName - the full path and name of the file
	 * @param cl - One of a few possible enumerated values in the enum called Classifier
	 * 
	 * **/
	public void readCSVFileIntoMatrix(String fileName) {
		CSVReader csvReader;
		try {
			csvReader = new CSVReader(new FileReader(new File(fileName)));
			List<String[]> list = csvReader.readAll();
			dataMatrixForResearchers = new String[list.size()][];
			dataMatrixForResearchers = list.toArray(dataMatrixForResearchers);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * A method to create a keyword based HashMap from a matrix where different keywords
	 * appear in the same column across different rows. For example, row1 may have keyword "Apple"
	 * in col1, "Banana" in col2, and "Clementine" in Col3.
	 * @param matrix - the input matrix with header "PersonId Keyword1 Freq1 RelFreq1 Keyword2 Freq2 RelFreq2 ..
	 */
	
	public void processMultipleColumnMatrixForKeywords(String[][] matrix){
		Map<ComponentsEnum, String> keywordPersonIdRelFreq = new HashMap<ComponentsEnum, String>();
		
		int colCount = matrix[0].length;
		String pid, kw;
		for(int i = 1; i < matrix.length; i++){
			pid = (matrix[i][0]).trim();
			if(pid.length() < 1){   //skip if person id is missing
				continue;
			}
			keywordPersonIdRelFreq.put(ComponentsEnum.PERSONID, pid);
			for(int j = 1; j < colCount; j += 3){
				kw = (matrix[i][j]).trim();
				
				//eliminate numbers and keywords that are too short and if rel freq is missing
				if(keywordDoesNotPassMinimumChecks(kw)){
					continue;
				}
				putKeywordThroughSplitterSpellCheckerAndLemmatizer(keywordPersonIdRelFreq, kw);
			}
		}
	}

	/**
	 * A method to create a keyword based HashMap from a three- column matrix where different keywords
	 * appear in different rows. For example, row1 may have keyword "Apple"
	 * in col1, while row2 may have keyword "Banana" in col1, and row3 may have "Clementine" in Col1.
	 * @param matrix - the input matrix with header "PersonId Keyword1 Freq1 
	 */
	private void processThreeColumnMatrixForKeywords(String[][] matrix) {
		Map<ComponentsEnum, String> keywordPersonIdRelFreq = new HashMap<ComponentsEnum, String>();
		
		String pid, kw;
		for(int i = 1; i < matrix.length; i++){
			pid = (matrix[i][0]).trim();
			kw = (matrix[i][1]).trim();
			if(pid.length() < 1 || keywordDoesNotPassMinimumChecks(kw)){
				continue;
			}
			keywordPersonIdRelFreq.put(ComponentsEnum.PERSONID, pid);
			putKeywordThroughSplitterSpellCheckerAndLemmatizer(keywordPersonIdRelFreq, kw);
		}
	}

	/**
	 * Keyword has to be at least 4 letters long, must have at least one alphabetic character,
	 * and must not be a stop word
	 * @param keyword
	 * @return
	 */
	private boolean keywordDoesNotPassMinimumChecks(String keyword) {
		return keyword.matches("\\d+") || keyword.length() < MINIMUM_WORD_LENGTH || 
				setOfInvalidWords.contains(keyword) ||
				!keyword.matches("[A-Za-z]+");
	}
	
	/**
	 * The keyword is put through various spell checks, and split (if necessary). Then the base form (lemma) is extracted, checked 
	 * against stop words at every turn
	 * @param keywordPersonIdRelFreq
	 * @param kwAndRf
	 */
	private void putKeywordThroughSplitterSpellCheckerAndLemmatizer(Map<ComponentsEnum, String> keywordPersonIdRelFreq, String kw) {
		//if keyword is not a gene symbol
		if(!kw.matches("^[A-Z][A-Z0-9]+")){
			List<String> lemmaList = new ArrayList<String>();
			if(Spelling.Lexicon.containsKey(kw)){
				String simpleLemma = lemmatizer.findLemmaOfKeyword(kw);
				lemmaList.add(simpleLemma);
			} else {
				lemmaList = this.findLemmasOfSplitAndCorrectedKeyword(kw);
			}
			for(String lemma : lemmaList){
				addKeywordIfNotAStopword(keywordPersonIdRelFreq, lemma);
			}
		} else {
			addKeywordIfNotAStopword(keywordPersonIdRelFreq, kw);
		}
	}

	/**
	 * This method checks for spelling errors and corrects them where possible.
	 * @param keyword
	 * @return
	 */
	private List<String> findLemmasOfSplitAndCorrectedKeyword(String keyword){
		String lemma;
		List<String> lemmaList = new ArrayList<String>();
		String[] kwArray = spellChecker.splitThenCorrectWords(keyword);

		for(String kw : kwArray){
			if(kw.length() < MINIMUM_WORD_LENGTH || setOfInvalidWords.contains(kw)){
				continue;
			}
			lemma = lemmatizer.findLemmaOfKeyword(kw);
			if(lemma == null){
				lemmaList.add(kw);
			} else {
				if(lemma.length() < MINIMUM_WORD_LENGTH || 
						lemma.equals(KeywordLemmatizer.INVALID_WORD) ||setOfInvalidWords.contains(lemma)){
					log.debug("Skipping word: " + kw);
					continue;
				}
				lemmaList.add(lemma);
			}
		}
		return lemmaList;
	}

	private void addKeywordIfNotAStopword(Map<ComponentsEnum, String> keywordPersonIdRelFreq, String kw) {
		if(kw.length() < 3 || 
				(!kw.equals(KeywordLemmatizer.INVALID_WORD) && !setOfInvalidWords.contains(kw))){
			keywordPersonIdRelFreq.put(ComponentsEnum.KEYWORD, kw);
			processKeyword(keywordPersonIdRelFreq);
		}
	}

	/**
	 * Creates a mapping from a keyword to a list of associated authors and the
	 * corresponding relative frequencies of usage
	 * @param componentsMap - a three member aggregate comprising author, keyword, and relative frequency
	 */
	private void processKeyword(Map<ComponentsEnum, String> componentsMap) {
		Set<String> authorSet = null;
		Set<String> keywordsSet = null;
		String keyword = componentsMap.get(ComponentsEnum.KEYWORD).toUpperCase();
		String personID = componentsMap.get(ComponentsEnum.PERSONID);
		if(this.keyword2AuthorsMap.keySet().contains(keyword)){
			authorSet = this.keyword2AuthorsMap.get(keyword);
		} else {
			authorSet = new HashSet<String>();
		}
		authorSet.add(personID);
		this.keyword2AuthorsMap.put(keyword, authorSet);
		
		if(this.author2KeywordsMap.keySet().contains(personID)){
			keywordsSet = this.author2KeywordsMap.get(personID);
		}else{
			keywordsSet = new HashSet<String>();
		}
		keywordsSet.add(keyword);
		this.author2KeywordsMap.put(personID, keywordsSet);
		
		log.debug("Added \"" + personID + "\" and \"" + keyword + "\" to map");
		setOfAuthors.add(personID); //we also keep track of the authors in a set
	}
	
	/**
	 * The purpose of this method is to read in a CSV file and
	 * store the information about researchers and the keywords 
	 * they are associated with in different data structures, which
	 * can be accessed by other classes 
	 */
	public void loadDataFromCsvFileIntoDataStructures(){
		
		AcaAnaLogger.initLogger();
		
		readCSVFileIntoMatrix(INPUT_AASEMSIGS_FILE);
		processMultipleColumnMatrixForKeywords(dataMatrixForResearchers);

		readCSVFileIntoMatrix(INPUT_OSUFRLONG_FILE);
		processThreeColumnMatrixForKeywords(dataMatrixForResearchers);
		
		readCSVFileIntoMatrix(INPUT_GRANTSLONG_FILE);
		processThreeColumnMatrixForKeywords(dataMatrixForResearchers);
		
		readCSVFileIntoMatrix(INPUT_TEDLONG_FILE);
		processThreeColumnMatrixForKeywords(dataMatrixForResearchers);
		
		log.info("Finished loading CSV file data!!");
		log.info("Loaded " + setOfAuthors.size() + " authors" );
		log.info("Loaded " + keyword2AuthorsMap.keySet().size() + " keywords");

		printKeywordsToFile();
		printAuthorsToFile();
		
		System.gc();
	}
	
	/** This method prints all authors to a file **/
	private void printAuthorsToFile(){
		PrintWriter pw = null;
		String line = "";
		Set<String> keywordsSet = null;
		List<String> sortedKeywordsList = null;
		int lineLength = 0, keywordCount = 0;
		
		List<String> sortedAuthors = this.sortStringsInSet(author2KeywordsMap.keySet());
		
		try {
			pw = new PrintWriter(new FileWriter(new File(OUTPUT_AUTHORS_FILE)));
			
			for(String author : sortedAuthors){
				line = author + ",";
				keywordsSet = author2KeywordsMap.get(author);
				keywordCount = keywordsSet.size();
				line += keywordCount + ",";
				sortedKeywordsList = sortStringsInSet(keywordsSet);
				for(String keyword : sortedKeywordsList){
					line += keyword + ",";
				}
				lineLength = line.length();
				line = line.substring(0, lineLength - 1);
				pw.println(line);
			}
		} catch (IOException e) {
			log.error("Unable to write to author file! :-(");
			e.printStackTrace();
		} finally {
			if(pw != null){
				pw.close();
			}
			log.info("Finished printing all the authors to file");
		}
		
	}
	
	/**This method prints all the keywords to a file**/
	private void printKeywordsToFile(){
		PrintWriter pw = null;
		String line = "";
		Set<String> authorSet = null;
		int lineLength = 0, rsrchCount = 0;
		
		List<String> sortedKeywords = this.sortStringsInSet(keyword2AuthorsMap.keySet());
		List<String> sortedAuthorList = new ArrayList<String>();
		
		try {
			pw = new PrintWriter(new FileWriter(new File(OUTPUT_KEYWORDS_FILE)));
			
			for(String keyword : sortedKeywords){
				line = keyword + ",";
				authorSet = keyword2AuthorsMap.get(keyword);
				rsrchCount = authorSet.size();
				line += rsrchCount + ",";
				sortedAuthorList = sortStringsInSet(authorSet);
				for(String author : sortedAuthorList){
					line += author + ",";
				}
				lineLength = line.length();
				line = line.substring(0, lineLength - 1);
				pw.println(line);
			}
		} catch (IOException e) {
			log.error("Unable to write to keyword file! :-(");
			e.printStackTrace();
		} finally {
			if(pw != null){
				pw.close();
			}
			log.info("Finished printing all the keywords to file");
		}
		
	}

	/**
	 * A helper method to sort the words in a set 
	 * @return - a sorted list of keywords
	 */
	public List<String> sortStringsInSet(Set<String> setOfStrings) {
		List<String> stringList = null;
		String[] stringArray = new String[setOfStrings.size()];
		stringArray = setOfStrings.toArray(stringArray);
		stringList = Arrays.asList(stringArray);
		Collections.sort(stringList);
		
		return stringList;
	}
	
	/**
	 * This is for testing the functionality of this class.
	 * TODO: JUnit Test Suite
	 * @param args
	 */
	public static void main(String[] args){
		
		AcaAnaLogger.initLogger();
		
		long startTime = System.currentTimeMillis();
		AuthorKeywordDataProcessor dm = new AuthorKeywordDataProcessor();

		dm.readCSVFileIntoMatrix(INPUT_AASEMSIGS_FILE);
		dm.processMultipleColumnMatrixForKeywords(dm.dataMatrixForResearchers);
		
		dm.readCSVFileIntoMatrix(INPUT_OSUFRLONG_FILE);
		dm.processThreeColumnMatrixForKeywords(dm.dataMatrixForResearchers);
		
		dm.readCSVFileIntoMatrix(INPUT_GRANTSLONG_FILE);
		dm.processThreeColumnMatrixForKeywords(dm.dataMatrixForResearchers);
		
		dm.readCSVFileIntoMatrix(INPUT_TEDLONG_FILE);
		dm.processThreeColumnMatrixForKeywords(dm.dataMatrixForResearchers);
		
		log.info("Finished loading CSV file data!!");
		
		log.info("Loaded " + dm.setOfAuthors.size() + " authors" );
		log.info("Loaded " + dm.keyword2AuthorsMap.keySet().size() + " keywords");
		
		dm.printKeywordsToFile();
		dm.printAuthorsToFile();
		
		long endTime = System.currentTimeMillis();
		
		long execTimeInSeconds = (endTime - startTime)/1000;
		log.info("Execution time: " + execTimeInSeconds + " seconds");
	}

}
