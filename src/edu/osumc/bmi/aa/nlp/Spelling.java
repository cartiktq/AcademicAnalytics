package edu.osumc.bmi.aa.nlp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared from http://raelcunha.com/spell-correct.php. Thanks to Rael Cunha!
 * 
 * @author rael (http://raelcunha.com/spell-correct.php)
 *
 */

public class Spelling {
	public static final String BIG_TEXT_FILE = "data/big.txt";
	public static HashMap<String, Integer> Lexicon = new HashMap<String, Integer>();
	public static final int MINIMUM_WORD_LENGTH = 4;

	public Spelling(String file) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(file));
		Pattern p = Pattern.compile("\\w+");
		for (String temp = ""; temp != null; temp = in.readLine()) {
			Matcher m = p.matcher(temp.toLowerCase());
			while (m.find())
				Lexicon.put((temp = m.group()),
						Lexicon.containsKey(temp) ? Lexicon.get(temp) + 1 : 1);
		}
		in.close();
	}

	/**
	 * @author sara26
	 * @param word
	 * @return
	 */
	public String[] splitThenCorrectWords(String word) {
		String[] words = null;
		List<String> singleWords = splitJoinedWords(word, new ArrayList<String>());
		words = new String[singleWords.size()];
		for (int i = 0; i < singleWords.size(); i++) {
			String singleWord = singleWords.get(i);
			words[i] = singleWord;
		}
		return words;
	}

	/**
	 * @author sara26
	 * @param joinedWord
	 * @return
	 */
	private final List<String> splitJoinedWords(String joinedWord, List<String> singleWords) {
		String word1 = "", word2 = "";
		if (Lexicon.containsKey(joinedWord)) {
			singleWords.add(joinedWord);
			return singleWords;
		} 
		for (int i = MINIMUM_WORD_LENGTH; i < joinedWord.length(); i++) {
			word1 = joinedWord.substring(0, i);
			if(Lexicon.containsKey(word1)){
				singleWords.add(word1);
				word2 = joinedWord.substring(i);
				splitJoinedWords(word2, singleWords);
			}
		}
		return singleWords;
	}
	
	private final ArrayList<String> edits(String word) {
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < word.length(); ++i)
			result.add(word.substring(0, i) + word.substring(i + 1));
		for (int i = 0; i < word.length() - 1; ++i)
			result.add(word.substring(0, i) + word.substring(i + 1, i + 2)
					+ word.substring(i, i + 1) + word.substring(i + 2));
		for (int i = 0; i < word.length(); ++i)
			for (char c = 'a'; c <= 'z'; ++c)
				result.add(word.substring(0, i) + String.valueOf(c)
						+ word.substring(i + 1));
		for (int i = 0; i <= word.length(); ++i)
			for (char c = 'a'; c <= 'z'; ++c)
				result.add(word.substring(0, i) + String.valueOf(c)
						+ word.substring(i));
		return result;
	}

	private final String correct(String word) {
		if (Lexicon.containsKey(word))
			return word;
		ArrayList<String> list = edits(word);
		HashMap<Integer, String> candidates = new HashMap<Integer, String>();
		for (String s : list)
			if (Lexicon.containsKey(s))
				candidates.put(Lexicon.get(s), s);
		if (candidates.size() > 0)
			return candidates.get(Collections.max(candidates.keySet()));
		for (String s : list)
			for (String w : edits(s))
				if (Lexicon.containsKey(w))
					candidates.put(Lexicon.get(w), w);
		return candidates.size() > 0 ? candidates.get(Collections
				.max(candidates.keySet())) : word;
	}

	public static void main(String args[]) throws IOException {
		if (args.length > 0)
			System.out.println((new Spelling(BIG_TEXT_FILE)).correct(args[0]));
	}
}
