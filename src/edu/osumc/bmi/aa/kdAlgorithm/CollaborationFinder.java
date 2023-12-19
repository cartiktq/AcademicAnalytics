package edu.osumc.bmi.aa.kdAlgorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.osumc.bmi.aa.dataPipeline.AuthorKeywordDataProcessor;

public class CollaborationFinder {

	public static Logger log = Logger.getLogger(CollaborationFinder.class);

	private Map<String, Set<String>> k2rMap;

	/**
	 * This map relates a pair of potential collaborators to the keywords that
	 * link them
	 **/
	private Map<List<String>, List<String>> mapFromCollabsToKeywords;

	/** GETTER **/
	public Map<List<String>, List<String>> getMapFromCollabsToKeywords() {
		return mapFromCollabsToKeywords;
	}

	public Map<String, Set<String>> getK2rMap() {
		return k2rMap;
	}

	public CollaborationFinder() {
		AuthorKeywordDataProcessor akdp = new AuthorKeywordDataProcessor();
		akdp.loadDataFromCsvFileIntoDataStructures();

		k2rMap = akdp.getKeyword2AuthorsMap();
		mapFromCollabsToKeywords = new HashMap<List<String>, List<String>>();

		log.setLevel(Level.INFO);
	}

	public void generatePossibleCollaborations() {
		Set<String> authorSet;
		
		for (String keyword : k2rMap.keySet()) {
			authorSet = k2rMap.get(keyword);
			if(authorSet.size() > 1){
				log.debug("Working with keyword: \"" + keyword + "\" with " + authorSet.size() + " authors");
			
//				if(authorSet.size() > 20){
//					log.info(keyword + " ----> " + authorSet.size() + " authors");
//					//TODO we need to do something different for these large numbers
//					continue;
//				}
				
				List<List<String>> listOfAuthorPairs = generateListOfAuthorPairs(authorSet);
				for (List<String> authorPair : listOfAuthorPairs) {
					addKeywordToAuthorPairList(keyword, authorPair);
				}
			}
		}
	}

	private List<List<String>> generateListOfAuthorPairs(Set<String> authorSet) {

		String[] authorArray = new String[authorSet.size()];
		authorArray = authorSet.toArray(authorArray);

		List<List<String>> listOfAuthorPairs = generateAuthorPairs(authorArray);
		return listOfAuthorPairs;
	}

	private List<List<String>> generateAuthorPairs(String[] authorArray) {
		List<String> authorPair;
		List<List<String>> listOfAuthorPairs = new ArrayList<List<String>>();

		for (int i = 0; i < authorArray.length; i++) {
			for (int j = i + 1; j < authorArray.length; j++) {
				authorPair = new ArrayList<String>();
				authorPair.add(authorArray[i]);
				authorPair.add(authorArray[j]);
				listOfAuthorPairs.add(authorPair);
			}
		}
		return listOfAuthorPairs;
	}

	private void addKeywordToAuthorPairList(String keyword,
			List<String> authorPair) {
		List<String> keywordList;
		Set<List<String>> setOfAuthorPairs = mapFromCollabsToKeywords.keySet();

		if (authorPairExists(authorPair, setOfAuthorPairs)) {
			keywordList = mapFromCollabsToKeywords.get(authorPair);
		} else {
			keywordList = new ArrayList<String>();
		}
		log.debug("Adding keyword: " + keyword + " for author pair: {"
				+ authorPair.get(0) + ", " + authorPair.get(1) + "}");
		keywordList.add(keyword);
		mapFromCollabsToKeywords.put(authorPair, keywordList);
	}

	private boolean authorPairExists(List<String> authorPair,
			Set<List<String>> setOfAuthorPairs) {
		Iterator<List<String>> setIterator = setOfAuthorPairs.iterator();
		List<String> nextAuthorPair;
		String author1 = authorPair.get(0), author2 = authorPair.get(1);
		String nextAuthor1 = "", nextAuthor2 = "";

		while (setIterator.hasNext()) {
			nextAuthorPair = setIterator.next();
			nextAuthor1 = nextAuthorPair.get(0);
			nextAuthor2 = nextAuthorPair.get(1);
			if (nextAuthor1.equals(author1) && nextAuthor2.equals(author2)) {
				return true;
			}
		}
		return false;
	}

	private void printMapFromAuthorPairsToCommonKeywords() {
		String line = "", author1 = "", author2 = "";
		int i = 0, collabCount = 0;
		for (List<String> authorPair : mapFromCollabsToKeywords.keySet()) {
			++collabCount;
			author1 = authorPair.get(0);
			author2 = authorPair.get(1);
			line = "<" + author1 + "," + author2 + "> ---> {";
			List<String> keywords = mapFromCollabsToKeywords.get(authorPair);
			for (i = 0; i < keywords.size() - 1; i++) {
				line += keywords.get(i) + ", ";
			}
			line += keywords.get(i) + "}";
			log.debug(line);
		}
		log.info(collabCount + " potential collaborations identified!");
	}

	public static void main(String[] args) {
		CollaborationFinder cf = new CollaborationFinder();
		cf.generatePossibleCollaborations();
		cf.printMapFromAuthorPairsToCommonKeywords();
	}

}
