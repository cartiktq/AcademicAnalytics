package edu.osumc.bmi.aa.graphdb;

import org.apache.log4j.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import edu.osumc.bmi.aa.util.AcaAnaLogger;

public class ServerManager {
	
	static{
		AcaAnaLogger.initLogger();
	}
	private static final Logger _log = Logger.getLogger(ServerManager.class);

	private static GraphDatabaseService graphDb = null;

	/************************
	 * Core Functionalities *
	 ************************/
	public static synchronized GraphDatabaseService getGraphDb(final String storeDir) {
		if (storeDir != null) {
			_log.info("Spinning up database at: " + storeDir);
			graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(storeDir);
		}
		registerShutdownHook();
		return graphDb;
	}

	private static void shutdown() {
		if (graphDb != null) {
			_log.info("Spinning down database at: " + graphDb.toString());
			graphDb.shutdown();
		} else {
			_log.warn("Database not running. Nothing to shut down.");
		}
	}

	/************
	 * Utilites *
	 ************/
	private static void registerShutdownHook() {
		// Registers a shutdown hook for the Neo4j and index service instances
		// so that it shuts down nicely when the VM exits (even if you
		// "Ctrl-C" the running example before it's completed)
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				shutdown();
			}
		});
	}
}
