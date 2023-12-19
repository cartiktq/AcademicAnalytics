package edu.osumc.bmi.aa.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class AcaAnaLogger {

	private static Logger logger;
	
	public static void initLogger() {
		loadPreferences();
		logger = Logger.getLogger(AcaAnaLogger.class);
		logger.setLevel(Level.INFO);
		logger.info("Initializing logger: Loading configuration file....");
	}

	public static void loadPreferences() {
		Properties preferences = new Properties();

		FileInputStream configFile;
		try {
			configFile = new FileInputStream(
					"properties/AcaAnaLogger.properties");
			preferences.load(configFile);
			PropertyConfigurator.configure(preferences);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
