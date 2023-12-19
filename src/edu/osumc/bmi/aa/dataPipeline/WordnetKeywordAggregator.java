package edu.osumc.bmi.aa.dataPipeline;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

public class WordnetKeywordAggregator {

	public static Logger log = Logger.getLogger(WordnetKeywordAggregator.class);
	static {
		log.setLevel(Level.DEBUG);
	}

	private Map<String, Set<String>> k2rMap;
	private String wnhome, path;
	private IDictionary dict = null;
	private URL url = null;
	private int keywordsWithSynsets = 0;

	public WordnetKeywordAggregator() {
		AuthorKeywordDataProcessor akdp = new AuthorKeywordDataProcessor();
		akdp.loadDataFromCsvFileIntoDataStructures();

		k2rMap = akdp.getKeyword2AuthorsMap();
		wnhome = System.getenv("WNHOME");
		path = wnhome + File.separator + "dict";

		createDictionary();
	}

	private void createDictionary() {
		try {
			url = new URL("file", null, path);
			dict = new Dictionary(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public boolean dictionaryIsOpen() {
		boolean open = false;
		if (dict != null) {
			try {
				open = dict.open();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return open;
	}

	public void consolidateKeywords() {
		if (!dictionaryIsOpen()) {
			return;
		}

		for (String keyword : k2rMap.keySet()) {
			for(POS pos : POS.values()){
				findLemma(keyword, pos);
			}
		}
	}

	public void findLemma(String keyword, POS pos) {
		List<IWord> listOfSynsetWords;
		int lineLength = 0;
		String lemma = "";
		String line = "";

		line = "Looking for keyword: " + keyword;
		line += " using POS " + pos.toString() + ".";
		IIndexWord idxWord = dict.getIndexWord(keyword, pos);

		if (idxWord != null) {
			lemma = idxWord.getLemma();
			line += " Lemma is: " + lemma + ".";
			IWordID wordID = idxWord.getWordIDs().get(0);
			IWord word = dict.getWord(wordID);
			line += " Synset is: {";
			listOfSynsetWords = word.getSynset().getWords();
			if (listOfSynsetWords.size() > 1) {
				++keywordsWithSynsets;
				for (IWord synsetWord : listOfSynsetWords) {
					line += synsetWord.getLemma() + ", ";
				}
				lineLength = line.length();
				line = line.substring(0, lineLength - 2);
			}
			line += "}";
		} 
		log.debug(line);
	}

	public static void main(String[] args) {
		WordnetKeywordAggregator kw = new WordnetKeywordAggregator();
		kw.consolidateKeywords();
		log.info("Found synsets for " + kw.keywordsWithSynsets
				+ " keywords out of " + kw.k2rMap.size());
	}
}
