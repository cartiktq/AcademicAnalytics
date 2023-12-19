package edu.osumc.bmi.aa.kdAlgorithm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;
import edu.osumc.bmi.aa.dataPipeline.AuthorKeywordDataProcessor;
import edu.osumc.bmi.aa.util.AcaAnaLogger;

public class PathGenerator implements Serializable {

	/**
	 * 
	 */
	
	public static Logger log = Logger.getLogger(PathGenerator.class);
	
	private static final long serialVersionUID = -5083674826682828479L;
	
	public static final int DEGREES_OF_SEPARATION = 3;
	public static final String SEPARATOR = "->";
	public static final String BLANK = "(blank)";
	public static final String OUTPUT_FILE="output/collabs.txt";
	
	public static final String INPUT_K2A_FILE = "data/KeywordsToAuthors.csv";
	public static final String INPUT_A2K_FILE = "data/AuthorsToKeywords.csv";
	
	public static final String SERIAL_PATHEXT_DIR="temp/extensions/";
	public static final String SERIAL_PATH_DIR="temp/paths/";
	
	public static final String PATH_STR="path";
	public static final String PATH_EXTN_STR="pathExtn";
	
	public static final String FILE_PATH_SEPARATOR = "/";
	
	private static final String SERIAL_FILE_EXTN = ".ser";
	private static final String OSU_AUTHOR_PATTERN = "^GRT[0-9]+";
	
	private static final String PATHFILE_NAME_PATTERN = "^path((Extn)?)[0-9]+.ser$";
	
	private AuthorKeywordDataProcessor akdp;
	private Map<String, List<String>> a2kMap, k2aMap;
	private Set<String> setOfAuthorsFinishedWith;
	
	static{
		log.setLevel(Level.INFO);
	}
	
	public PathGenerator(){
		String[][] a2kArray = readCSVFileIntoMatrix(INPUT_A2K_FILE);
		String[][] k2aArray = readCSVFileIntoMatrix(INPUT_K2A_FILE);
		
		a2kMap = sortValuesInMap(transformArrayToHashMap(a2kArray));
		k2aMap = sortValuesInMap(transformArrayToHashMap(k2aArray));
		setOfAuthorsFinishedWith = new HashSet<String>();
	}
	
	/** Reads in a given CSV file into a 2D Array
	 * @param fileName - the full path and name of the file
	 * @param cl - One of a few possible enumerated values in the enum called Classifier
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
		} finally{
			try {
				csvReader.close();
			} catch (IOException e) {
				log.error("IO EXCEPTION: Problems closing CSV Reader");
				e.printStackTrace();
			} finally{
				csvReader = null;
			}
		}
		return outputMatrix;
	}
	
	/**
	 * This method is used to transform a 2D array into a hashmap
	 * where the first column is the key and the remaining columns
	 * are mapped to values of that key
	 * @param array
	 * @return
	 */
	
	private Map<String, Set<String>> transformArrayToHashMap(String[][] array){
		Map<String, Set<String>> hashMap = new HashMap<String, Set<String>>();
		Set<String> values;
		String key;
		for(int i = 0; i < array.length; i++){
			key = array[i][0];
			values = new HashSet<String>();
			for(int j = 1; j < array[i].length; j++){
				values.add(array[i][j]);
			}
			hashMap.put(key, values);
		}
		
		return hashMap;
	}
	
	/**
	 * Method simply sorts the collection of values in the map by
	 * alphabetical order. May be modified in future or dispensed with. 
	 * @param keyword2AuthorsMap
	 * @return
	 */
	private Map<String, List<String>> sortValuesInMap(
			Map<String, Set<String>> keyword2AuthorsMap) {
		Map<String, List<String>> newMap = new HashMap<String, List<String>>();
		for(Entry<String, Set<String>> entry : keyword2AuthorsMap.entrySet()){
			newMap.put(entry.getKey(), akdp.sortStringsInSet(entry.getValue()));
		}
		
		return newMap;
	}

	/**
	 * The main method of this class
	 */
	public void run(){           
		
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new File(OUTPUT_FILE));
			for(String author : a2kMap.keySet()){
				//Every path needs to start with an OSU researcher
				if(!author.equals(BLANK) && author.matches(OSU_AUTHOR_PATTERN)){
					generatePathsForAuthor(author, pw);
					setOfAuthorsFinishedWith.add(author);
				}
			}
		} catch (FileNotFoundException e) {
			log.error("UNABLE TO OPEN OUTPUT FILE");
			e.printStackTrace();
		} finally {
			if(pw != null){
				pw.flush();
				pw.close();
			}
			clearDirectory(SERIAL_PATH_DIR);
		}
	}
	
	/**
	 * This method generates all the paths for the given author
	 * The generated paths are written to an output file
	 * @param author
	 * @param pw
	 */
	private void generatePathsForAuthor(String author, PrintWriter pw){
		
		List<String> path = new ArrayList<String>();
		path.add(author);
		serializePath(path, author, 0);

		for (int i = 0; i < DEGREES_OF_SEPARATION; i++) {
			extendPaths(author);
		}

		File pathDir = new File(SERIAL_PATH_DIR + author);
		for (File pathFile : pathDir.listFiles()) {
			if(pathFile.getName().matches(PATHFILE_NAME_PATTERN)){ //this check to ignore .DS, .git files etc
				List<String> completePath = deserializePath(pathFile);
				pw.println(pathToString(completePath));
			}
		}
	}

	/**
	 * This method extends paths starting with the input author id
	 * @param author
	 */
	private void extendPaths(String author) {
		
		File pathDir = new File(SERIAL_PATH_DIR + author);
		File[] pathFiles = pathDir.listFiles();
	
		for(File pathFile : pathFiles){
			if(pathFile.getName().matches(PATHFILE_NAME_PATTERN)){ //this check to ignore .DS, .git files etc
				List<String> path = deserializePath(pathFile);
				//Write extensions to extensions dir
				getPathExtns(path);
			
				//read extensions out from extensions dir,
				//append extensions to original path and serialize new paths to path dir
				File extnDir = new File(SERIAL_PATHEXT_DIR);
				int count = 0;
				for(File extnFile : extnDir.listFiles()){
					if(pathFile.getName().matches(PATHFILE_NAME_PATTERN)){ //this check to ignore .DS, .git files etc	
			
						List<String> pathExt = deserializePath(extnFile);
						List<String> extendedPath = appendPathExtns(path, pathExt);
						serializePath(extendedPath, author, count++);
					}
				}
				//clear out extensions directory
				clearDirectory(SERIAL_PATHEXT_DIR);
			}
		}
	}

	/**
	 * This method creates all valid path extensions for the given input path
	 * Each created path extension is then serialized 
	 * @param path
	 */
	private void getPathExtns(List<String> path) {
		int pathSize = path.size();
		int count = 0;
		String authorToExtendFrom = path.get(pathSize - 1);
		
		List<String> keywordList = a2kMap.get(authorToExtendFrom);
		for(String keyword : keywordList){
			if(!keyword.equals(BLANK) && !path.contains(keyword)){
				List<String> secondAuthorList = k2aMap.get(keyword);
				for(String secondAuthor : secondAuthorList){
					if(!secondAuthor.equals(BLANK) && !path.contains(secondAuthor) &&
							!setOfAuthorsFinishedWith.contains(secondAuthor)){
						List<String> pathExt = new ArrayList<String>();
						pathExt.add(keyword);
						pathExt.add(secondAuthor);
						serializePath(pathExt, count++);
					}
				}
			}
		}
	}

	/**
	 * Method appends path extensions to path
	 * @param path
	 * @param pathExt
	 * @return
	 */
	private List<String> appendPathExtns(List<String> path, List<String> pathExt) {
		List<String> extPath = new ArrayList<String>();
		extPath.addAll(path);
		extPath.addAll(pathExt);

		return extPath;
	}
	
	/**
	 * Method to serialize a path object. This one is for path extensions
	 * @param pathExt
	 * @param i
	 */
	private void serializePath(List<String> pathExt, int i) {
		String fileName = SERIAL_PATHEXT_DIR + PATH_EXTN_STR + i + SERIAL_FILE_EXTN;
		createDirectory(SERIAL_PATHEXT_DIR);
		serialize(pathExt, fileName);
	}

	/**
	 * Method to serialize a path object. This is an overloaded version and deals with
	 * a path generated for an author
	 * @param path
	 * @param author
	 * @param i
	 */
	//Overloaded method
	private void serializePath(List<String> path, String author, int i) {
		String fileName = SERIAL_PATH_DIR + author + FILE_PATH_SEPARATOR + PATH_STR + i + SERIAL_FILE_EXTN;
		createDirectory(SERIAL_PATH_DIR + author + FILE_PATH_SEPARATOR);
		serialize(path, fileName);
	}
	
	/**
	 * Method to retrieve serialized path object
	 * @param pathFile - the location of the frozen serialized path object
	 * @return
	 */
	private List<String> deserializePath(File pathFile) {
		List<String> path = null;
		
		try {
			FileInputStream fis = new FileInputStream(pathFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			path = (List<String>)ois.readObject();
			ois.close();
			fis.close();
		} catch (FileNotFoundException fnfe) {
			log.error("FileNotFoundException: Could Not Find Path File!");
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			log.error("IOException: Error reading from serialized path file");
			ioe.printStackTrace();
		} catch (ClassNotFoundException cnfe) {
			log.error("ClassNotFoundException: Error reading from serialized path file!! CLASS NOT FOUND!!");
			cnfe.printStackTrace();
		}
		
		return path;
	}
	
	/**
	 * The actual serialization and writing to a file through
	 * ObjectOutputStream method happens here
	 * @param path
	 * @param fileName
	 */
	private void serialize(List<String> path, String fileName) {
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(path);
			oos.close();
			fos.close();
		} catch (FileNotFoundException fnfe) {
			log.error("FileNotFoundException: Serial file cannot be found!!");
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			log.error("IOException: Problems writing to serial file!");
			ioe.printStackTrace();
		}
	}
	
	/**
	 * Method to create a new directory if it does not exist
	 * @param dirName
	 */
	private void createDirectory(String dirName) {
		File dir = new File(dirName);
		if(!dir.exists()){
			dir.mkdir();
		}
	}

	/**
	 * Method to clear all files out of specified directory, recursively
	 * @param dir
	 */
	private void clearDirectory(String dir){
		File pathDir = new File(dir);
		for(File file : pathDir.listFiles()){
			if(file.isFile()){
				file.delete();
			} else {
				if(file.isDirectory()){
					String dirName = file.getName();
					clearDirectory(dirName);
				}
			}
		}
	}
	
	/**
	 * Method to generate a text representation of a generated path
	 * Format is {{AUTHOR}} ====> [AUTHOR]->[KEYWORD1]->[AUTHOR2]->[KEYWORD2]->...
	 * @param wordList
	 * @return
	 */
	private String pathToString(List<String> wordList){
		int lastIndexOfSeparator = 0;
		String line ="[" + wordList.get(0) + "] ====>";
		for(String word : wordList){
			line += "[" + word + "]" + SEPARATOR;
		}
		lastIndexOfSeparator = line.lastIndexOf(SEPARATOR);
		line = line.substring(0, lastIndexOfSeparator);
		return line;
	}
	
	
	public static void main(String[] args) {
		AcaAnaLogger.initLogger();
		
		long startTime = System.currentTimeMillis();
		
		PathGenerator generator = new PathGenerator();
		generator.run();

		long endTime = System.currentTimeMillis();
		long execTimeInSeconds = (endTime - startTime)/1000;
		log.info("Execution time: " + execTimeInSeconds + " seconds");
	}

}
