package edu.osumc.bmi.aa.nlp;

import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class KeywordLemmatizer {

	public static final String INVALID_WORD = "StopWord";
	public static Logger log;
	public static StanfordCoreNLP stanfordNLP;
	public static enum ValidPOS{
		NN, NNS, NNP, NNPS, SYM
	};
	
	static {
		log = Logger.getLogger(KeywordLemmatizer.class);
		log.setLevel(Level.INFO);
		Properties props = new Properties();
		props.put("annotators","tokenize, ssplit, pos, lemma, ner");
		stanfordNLP = new StanfordCoreNLP(props);
	}
	
	public String arrayToString(String[] keywords) {
		String line = "{";
		int i = 0;
		for(i = 0; i < keywords.length - 1; i++){
			line += keywords[i] + ", ";
		}
		line += keywords[i] + "}";
		return line;
	}

	public String findLemmaOfKeyword(String nkw) {
		String lemma = null, pos = null;

		Annotation doc = new Annotation(nkw);
		stanfordNLP.annotate(doc);
		CoreLabel token = doc.get(TokensAnnotation.class).get(0);
		pos = token.get(PartOfSpeechAnnotation.class);
		
		if(posIsOneOf(pos, ValidPOS.values())){
			lemma = token.lemma();
		} else {
			lemma = INVALID_WORD;
		}
		
		if(lemma != null && lemma != INVALID_WORD){
			log.debug("WORD: " + nkw + "\tLEMMA: " + lemma + "\tPOS: " + pos);
		}
		
		return lemma;
	}

	private boolean posIsOneOf(String pos, ValidPOS[] values) {
		for(ValidPOS value : values){
			if (pos.equals(value.toString())){
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args) {
		KeywordLemmatizer kp = new KeywordLemmatizer();
		String lemma = kp.findLemmaOfKeyword(args[0]);
		log.debug("WORD: " + args[0] + "\tLEMMA: " + lemma);
	}

}
