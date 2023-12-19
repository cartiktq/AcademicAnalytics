package edu.osumc.bmi.aa.graphdb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;

import au.com.bytecode.opencsv.CSVReader;
import edu.osumc.bmi.aa.util.AcaAnaLogger;

public class CsvToGraphDbExporter {
	private static final Logger _log = Logger
			.getLogger(CsvToGraphDbExporter.class);

	private static final String INPUT_AUTHORS_FILE = "data/AuthorsToKeywords.csv";
	private static final String INPUT_KEYWORDS_FILE = "data/KeywordsToAuthors.csv";
	private static final String INPUT_KEYWORD_ANNOTATIONS_FILE = "data/KeywordAnnotations.csv";
	private static final String DB_PATH = "persisted/r2kGraph";
	
	private static enum RelTypes implements RelationshipType {
		ASSOCIATED_WITH
	}

	private static enum NodeProperties {
		ID, WEIGHT, HIERARCHY
	};

	private static enum NodeTypes {
		AUTHOR, KEYWORD
	};

	private GraphDatabaseService graphDB;
	private IndexDefinition authorIndexDef, keywordIndexDef;

	private String[][] dataMatrixForResearchers, dataMatrixForKeywords, usefulKeywordsArray;
	private Map<String, List<String>> mapOfKeywordsToWeightAndHier;
	
	public CsvToGraphDbExporter() {
		graphDB = ServerManager.getGraphDb(DB_PATH);
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
	public String[][] readCSVFileIntoMatrix(String fileName) {
		CSVReader csvReader = null;
		String[][] dataMatrix = null;
		try {
			csvReader = new CSVReader(new FileReader(new File(fileName)));
			List<String[]> list = csvReader.readAll();
			dataMatrix = new String[list.size()][];
			dataMatrix = list.toArray(dataMatrix);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (csvReader != null) {
				try {
					csvReader.close();
				} catch (IOException e) {
					System.err
							.println("Problems closing the CSV reader from OZ");
					e.printStackTrace();
				}
			}
		}
		return dataMatrix;
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
	 * This method is used to write a 2D array to a GraphDB
	 * 
	 * @param array
	 * @return
	 */

	private void writeArrayToGraphDB(String[][] array, NodeTypes type) {
		if(type.equals(NodeTypes.KEYWORD)){
			writeKeywordsToGraphDB(array);
		} else {
			writeAuthorsToGraphDB(array);
		}
	}
	
	private void writeAuthorsToGraphDB(String[][] array){
		String key;
		Node firstNode;
		Label label1 = DynamicLabel.label(NodeTypes.AUTHOR.toString());
		Label label2 = DynamicLabel.label(NodeTypes.KEYWORD.toString());
		int valueCount;
		for (int i = 0; i < array.length; i++) {
			key = array[i][0];
			valueCount = Integer.parseInt(array[i][1].trim());
			if (valueCount < 1) {
				continue;
			}
			if ((firstNode = findNode(key, label1)) == null) {
				firstNode = createNode(key, label1);
			}
			createAssociations(array, firstNode, label2, i);
		}
	}

	private void writeKeywordsToGraphDB(String[][] array){
			String key;
			Node firstNode;
			
			Label label1 = DynamicLabel.label(NodeTypes.KEYWORD.toString());
			Label label2 = DynamicLabel.label(NodeTypes.AUTHOR.toString());
			
			int valueCount;
			for (int i = 0; i < array.length; i++) {
				key = array[i][0];
				valueCount = Integer.parseInt(array[i][1].trim());
				if (valueCount < 1 || !mapOfKeywordsToWeightAndHier.containsKey(key)) {
					continue;
				}
				if ((firstNode = findNode(key, label1)) == null) {
					firstNode = createNode(key, label1);
					addKeywordAttributesToNode(firstNode, key);
				}
				createAssociations(array, firstNode, label2, i);
			}
	}
	
	private IndexDefinition createIndex(String nodeType){
		IndexDefinition indexDef;
		try ( Transaction tx = graphDB.beginTx()){
		    Schema schema = graphDB.schema();
		    indexDef = schema.indexFor( DynamicLabel.label(nodeType)).on(NodeProperties.ID.toString()).create();
		    tx.success();
		}
		return indexDef;
	}

	private void dropIndex(String nodeType){
		try (Transaction tx = graphDB.beginTx()){
		    Label label = DynamicLabel.label( nodeType );
		    for ( IndexDefinition indexDefinition : graphDB.schema().getIndexes(label)){
		        // There is only one index
		        indexDefinition.drop();
		    }
		    tx.success();
		}
	}
	
	private Node findNode(String key, Label label) {
		Node node = null;
		
		try (ResourceIterator<Node> nodes = graphDB.findNodesByLabelAndProperty(label, NodeProperties.ID.toString(), key).iterator()) {
			if (nodes != null) {
				while (nodes.hasNext()) {
					node = nodes.next();
					break;
				}
			}
		}
		return node;
	}

	private Node createNode(String id, Label label) {
		Node node = graphDB.createNode(label);
		node.setProperty(NodeProperties.ID.toString(), id);
		return node;
	}

	private void createAssociations(String[][] array, Node firstNode,
			Label label, int i) {
		String value;
		Node secondNode;
		Relationship associatedWith;
		for (int j = 2; j < array[i].length; j++) {
			if ((value = array[i][j].trim()).length() > 0) {
				if ((secondNode = findNode(value, label)) == null) {
					secondNode = createNode(value, label);
				}
				associatedWith = firstNode.createRelationshipTo(secondNode, RelTypes.ASSOCIATED_WITH);
			}
		}
	}

	private void addKeywordAttributesToNode(Node kwNode, String key){
		String weight = mapOfKeywordsToWeightAndHier.get(key).get(0);
		String hier = mapOfKeywordsToWeightAndHier.get(key).get(1);
		kwNode.setProperty(NodeProperties.WEIGHT.toString(), weight);
		kwNode.setProperty(NodeProperties.HIERARCHY.toString(), hier);
	}
	
	private void run(boolean createIndexes) {
		dataMatrixForResearchers = readCSVFileIntoMatrix(INPUT_AUTHORS_FILE);
		dataMatrixForKeywords = readCSVFileIntoMatrix(INPUT_KEYWORDS_FILE);
		usefulKeywordsArray = readCSVFileIntoMatrix(INPUT_KEYWORD_ANNOTATIONS_FILE);
		mapOfKeywordsToWeightAndHier = transformKeywordArrayToHashMap(usefulKeywordsArray);
		
		if(createIndexes){
			dropIndex(NodeTypes.AUTHOR.toString());
			dropIndex(NodeTypes.KEYWORD.toString());
			
			authorIndexDef = createIndex(NodeTypes.AUTHOR.toString());
			keywordIndexDef = createIndex(NodeTypes.KEYWORD.toString());
		}

		try(Transaction tx = graphDB.beginTx()){
			writeArrayToGraphDB(dataMatrixForKeywords, NodeTypes.KEYWORD);
			_log.info("Finished writing " + dataMatrixForKeywords.length + " keywords to database!");
			
			writeArrayToGraphDB(dataMatrixForResearchers, NodeTypes.AUTHOR);
			_log.info("Finished writing " + dataMatrixForResearchers.length + " authors to database!");
			
			tx.success();
		}
	}

	public static void main(String[] args) {
		AcaAnaLogger.initLogger();

		boolean createIndexes = Boolean.parseBoolean(args[0]);
		
		CsvToGraphDbExporter exporter = new CsvToGraphDbExporter();
		long startTime = System.currentTimeMillis();
		exporter.run(createIndexes);
		long endTime = System.currentTimeMillis();
		
		long duration = (endTime - startTime)/1000;
		
		_log.info("Finished in " + duration + " seconds!!");
	}
}
