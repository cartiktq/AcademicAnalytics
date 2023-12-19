package edu.osumc.bmi.aa.loc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.osumc.bmi.aa.util.AcaAnaLogger;
import au.com.bytecode.opencsv.CSVReader;

public class LocHierarchy {
	public static Logger log = Logger.getLogger(LocHierarchy.class);
	
	private static final String INPUT_K2A_FILE = "data/KeywordsToAuthors.csv";
	
	private static final String INPUT_KEYWORD_SCORES_FILE = "data/KeywordScores.csv";
	private static final String OUTPUT_KEYWORD_SCORES_FILE = "data/KeywordAnnotations.csv";
	
	private Map<String, String> mapOfUsefulKeywordsToWeights;
	private Map<String, Integer> k2aMap;
	
	private Hierarchy agriculture;
	private Hierarchy technology;
	private Hierarchy science;
	private Hierarchy medicine; 
	private Hierarchy socialScience;
	private Hierarchy psychology;
	private Hierarchy geography;
	private Hierarchy philosophy;
	private Hierarchy history;
	private Hierarchy religion;
	private Hierarchy politicalScience;
	private Hierarchy law;
	private Hierarchy education;
	private Hierarchy military;
	private Hierarchy sport;
	private Hierarchy arts;
	private Hierarchy language;
	
	private Hierarchy[] hierarchyArray;

	private Set<Set<Heading>> setOfEquidistantHeadings;
	
	private double medianUsageCount;
	
	public Hierarchy[] getHierarchyArray(){
		return hierarchyArray;
	}
	
	public Set<Set<Heading>> getSetOfEquidistantHeadings(){
		return setOfEquidistantHeadings;
	}
	
	public LocHierarchy(){
		log.setLevel(Level.INFO);
		String[][] k2aArray = readCSVFileIntoMatrix(INPUT_K2A_FILE);
		k2aMap = transformArrayToHashMap(k2aArray);
		
		computeKeywordUsageStatistics();
		
		createLocHierarchy();
		
		hierarchyArray = new Hierarchy[]{
				education, law, politicalScience, religion,
				history, philosophy, psychology, geography, socialScience,
				medicine, science, technology, agriculture, military,
				sport, arts, language
		};
		
		populateSemanticDistancesMap();
	}

	/**
	 * Method creates the whole heading hierarchy which is adapted
	 * from the Library of Congress classification scheme
	 * http://www.loc.gov/catdir/cpso/lcco/
	 * 
	 */
	private void createLocHierarchy() {
		//AGRICULTURE HEADING
		
		Heading dairy = new Heading("dairy");
		Heading veterinaryMedicine = new Heading("veterinary");
		Heading meat = new Heading("meat");
		
		List<Heading> animalCultureSubheadings = new ArrayList<Heading>();
		animalCultureSubheadings.add(veterinaryMedicine);
		animalCultureSubheadings.add(dairy);
		animalCultureSubheadings.add(meat);
		
		Heading animalCulture = new Heading("animal-culture");
		animalCulture.setSubheadings(animalCultureSubheadings);
		
		Heading fruits = new Heading("fruits");
		Heading vegetables = new Heading("vegetables");
		Heading flowers = new Heading("flowers");
		Heading crops = new Heading("crops");
		Heading soils = new Heading("soils");
		
		List<Heading> plantCultureSubheadings = new ArrayList<Heading>();
		plantCultureSubheadings.add(flowers);
		plantCultureSubheadings.add(vegetables);
		plantCultureSubheadings.add(crops);
		plantCultureSubheadings.add(fruits);
		plantCultureSubheadings.add(soils);
		
		Heading plantCulture = new Heading("plant-culture");
		plantCulture.setSubheadings(plantCultureSubheadings);
		
		Heading aquaCulture = new Heading("aquaCulture");
		Heading forestry = new Heading("forestry");
		
		List<Heading> agricultureSubheadings = new ArrayList<Heading>();
		agricultureSubheadings.add(aquaCulture);
		agricultureSubheadings.add(animalCulture);
		agricultureSubheadings.add(plantCulture);
		agricultureSubheadings.add(forestry);
		
		Heading agricultureHead = new Heading("agriculture");
		agricultureHead.setSubheadings(agricultureSubheadings);
		
		agriculture = new Hierarchy(agricultureHead);
		
		//TECHNOLOGY HEADING
		
		Heading roads = new Heading("roads");
		Heading construction = new Heading("construction");
		
		List<Heading> civilEngineeringSubheadings = new ArrayList<Heading>();
		civilEngineeringSubheadings.add(roads);
		civilEngineeringSubheadings.add(construction);
		
		Heading civilEngineering = new Heading("civil-engineering");
		civilEngineering.setSubheadings(civilEngineeringSubheadings);
		
		Heading aeronautics = new Heading("aeronautics");
		Heading mechanicalEngineering = new Heading("mechanical-engineering");
		Heading eletricalEngineering = new Heading("electrical-engineering");
		Heading informationTechnology = new Heading("information-technology");
		Heading materials = new Heading("materials");
		
		List<Heading> technologySubheadings = new ArrayList<Heading>();
		technologySubheadings.add(eletricalEngineering);
		technologySubheadings.add(mechanicalEngineering);
		technologySubheadings.add(aeronautics);
		technologySubheadings.add(civilEngineering);
		technologySubheadings.add(informationTechnology);
		technologySubheadings.add(materials);
		
		Heading technologyHead = new Heading("technology");
		technologyHead.setSubheadings(technologySubheadings);
		
		technology = new Hierarchy(technologyHead);
		
		//SCIENCES HEADING
		
		Heading algebra = new Heading("algebra");
		Heading logic = new Heading("logic");
		Heading probability = new Heading("probability");
		Heading calculus = new Heading("calculus");
		Heading vectorAlgebra = new Heading("vector-algebra");
		Heading coordinateGeometry = new Heading("coordinate-geometry");
		
		List<Heading> mathSubheadings = new ArrayList<Heading>();
		mathSubheadings.add(coordinateGeometry);
		mathSubheadings.add(vectorAlgebra);
		mathSubheadings.add(logic);
		mathSubheadings.add(algebra);
		mathSubheadings.add(probability);
		mathSubheadings.add(calculus);
		
		Heading math = new Heading("mathematics");
		math.setSubheadings(mathSubheadings);
		
		Heading acoustics = new Heading("acoustics");
		Heading optics = new Heading("optics");
		Heading thermodynamics = new Heading("thermodynamics");
		Heading electromagnetics = new Heading("electromagnetics");
		Heading meteorology = new Heading("meteorology");
		Heading subatomicPhysics = new Heading("subatomic-physics");
		Heading kinetics = new Heading("kinetics");
		Heading measurement = new Heading("measurement");
		
		List<Heading> physicsSubheadings = new ArrayList<Heading>();
		physicsSubheadings.add(measurement);
		physicsSubheadings.add(kinetics);
		physicsSubheadings.add(meteorology);
		physicsSubheadings.add(electromagnetics);
		physicsSubheadings.add(thermodynamics);
		physicsSubheadings.add(acoustics);
		physicsSubheadings.add(subatomicPhysics);
		physicsSubheadings.add(optics);
		
		Heading physics = new Heading("physics");
		physics.setSubheadings(physicsSubheadings);
		
		Heading organicChemistry = new Heading("organic-chemistry");
		Heading inorganicChemistry = new Heading("inorganic-chemistry");

		Heading chemicalReactions = new Heading("chemical-reactions");
		
		List<Heading> physicalChemistrySubheadings = new ArrayList<Heading>();
		physicalChemistrySubheadings.add(chemicalReactions);
		
		Heading physicalChemistry = new Heading("physicalChemistry");
		physicalChemistry.setSubheadings(physicalChemistrySubheadings);
		
		Heading solid = new Heading("solid");
		Heading liquid = new Heading("liquid");
		Heading gas = new Heading("gas");
		
		List<Heading> matterStatesSubheadings = new ArrayList<Heading>();
		matterStatesSubheadings.add(solid);
		matterStatesSubheadings.add(liquid);
		matterStatesSubheadings.add(gas);
		
		Heading matterStates = new Heading("matter-states");
		matterStates.setSubheadings(matterStatesSubheadings);
		
		Heading element = new Heading("element");
		Heading compound = new Heading("compound");
		Heading mixture = new Heading("mixture");
		Heading ion = new Heading("ion");

		List<Heading> matterTypesSubheadings = new ArrayList<Heading>();
		matterTypesSubheadings.add(mixture);
		matterTypesSubheadings.add(element);
		matterTypesSubheadings.add(compound);
		matterTypesSubheadings.add(ion);
		
		Heading matterTypes = new Heading("matter-types");
		matterTypes.setSubheadings(matterTypesSubheadings);
		
		List<Heading> chemistrySubheadings = new ArrayList<Heading>();
		chemistrySubheadings.add(matterTypes);
		chemistrySubheadings.add(matterStates);
		chemistrySubheadings.add(physicalChemistry);
		chemistrySubheadings.add(inorganicChemistry);
		chemistrySubheadings.add(organicChemistry);
		
		Heading chemistry = new Heading("chemistry");
		chemistry.setSubheadings(chemistrySubheadings);
		
		Heading planets = new Heading("planets");
		Heading stars = new Heading("stars");
		
		List<Heading> astronomySubheadings = new ArrayList<Heading>();
		astronomySubheadings.add(planets);
		astronomySubheadings.add(stars);
		
		Heading astronomy= new Heading("astronomy");
		astronomy.setSubheadings(astronomySubheadings);
		
		Heading minerology = new Heading("minerology");
		Heading tectonics = new Heading("tectonics");
		
		List<Heading> geologySubheadings = new ArrayList<Heading>();
		geologySubheadings.add(tectonics);
		geologySubheadings.add(minerology);
		
		Heading geology = new Heading("geology");
		geology.setSubheadings(geologySubheadings);
		
		Heading anatomyOrgans = new Heading("anatomy-organs");
		Heading anatomyTissues = new Heading ("anatomy-tissues");
		Heading bodyParts = new Heading("body-parts");
		
		List<Heading> anatomySubheadings = new ArrayList<Heading>();
		anatomySubheadings.add(bodyParts);
		anatomySubheadings.add(anatomyTissues);
		anatomySubheadings.add(anatomyOrgans);
		
		Heading anatomy = new Heading("anatomy");
		anatomy.setSubheadings(anatomySubheadings);
		
		Heading botany = new Heading("botany");

		Heading invertebrates = new Heading("invertebrates");
		Heading vertebrates = new Heading("vertebrates");
		
		List<Heading> zoologySubheadings = new ArrayList<Heading>();
		zoologySubheadings.add(vertebrates);
		zoologySubheadings.add(invertebrates);
		
		Heading zoology = new Heading("zoology");
		zoology.setSubheadings(zoologySubheadings);
		
		Heading physiology = new Heading("physiology");
		
		Heading evolution = new Heading("evolution");
		Heading genetics = new Heading("genetics");
		Heading reproduction = new Heading("reproduction");
		Heading ecology = new Heading("ecology");
		Heading cytology = new Heading("cytology");
		
		List<Heading> biologySubheadings = new ArrayList<Heading>();
		biologySubheadings.add(ecology);
		biologySubheadings.add(reproduction);
		biologySubheadings.add(genetics);
		biologySubheadings.add(evolution);
		biologySubheadings.add(cytology);
		
		Heading biology = new Heading("biology");
		biology.setSubheadings(biologySubheadings);
		
		Heading virology = new Heading("virology");
		Heading bacteriology = new Heading("bacteriology");
		Heading immunology = new Heading("immunology");
		
		List<Heading> microbiologySubheadings = new ArrayList<Heading>();
		microbiologySubheadings.add(bacteriology);
		microbiologySubheadings.add(virology);
		microbiologySubheadings.add(immunology);
		
		Heading microbiology = new Heading("microbiology");
		microbiology.setSubheadings(microbiologySubheadings);

		Heading biochemistry = new Heading("biochemistry");
		
		List<Heading> scienceSubheadings = new ArrayList<Heading>();
		scienceSubheadings.add(microbiology);
		scienceSubheadings.add(math);
		scienceSubheadings.add(physics);
		scienceSubheadings.add(chemistry);
		scienceSubheadings.add(biology);
		scienceSubheadings.add(astronomy);
		scienceSubheadings.add(geology);
		scienceSubheadings.add(anatomy);
		scienceSubheadings.add(botany);
		scienceSubheadings.add(zoology);
		scienceSubheadings.add(physiology);
		scienceSubheadings.add(biochemistry);
		
		Heading scienceHead = new Heading("science");
		scienceHead.setSubheadings(scienceSubheadings);
		
		science = new Hierarchy(scienceHead);
		
		//MEDICINE HEAD
		
		Heading biomedicalInstrumentation = new Heading("biomedical-instrumentation");
		Heading publicHealth = new Heading("public-health");
		Heading toxicology = new Heading("toxicology");
		
		Heading pathologyLabTechnique = new Heading("pathology-lab-technique");
		Heading symptoms = new Heading("symptoms");
		
		List<Heading> pathologySubheadings = new ArrayList<Heading>();
		pathologySubheadings.add(pathologyLabTechnique);
		pathologySubheadings.add(symptoms);
		
		Heading pathology = new Heading("pathology");
		pathology.setSubheadings(pathologySubheadings);
		
		Heading oncology = new Heading("oncology");
		Heading neurology = new Heading("neurology");
		Heading allergy = new Heading("allergy");
		Heading deficiency = new Heading("deficiency");
		Heading endocrine = new Heading("endocrine");
		Heading cardiovascular = new Heading("cardiovascular");
		Heading pulmonary = new Heading("pulmonary");
		Heading gastrointestinal = new Heading("gastrointestinal");
		Heading excretory = new Heading("excretory");
		Heading musculoSkeletal = new Heading("musculoskeletal");
		
		List<Heading> internalMedicineSubheadings = new ArrayList<Heading>();
		internalMedicineSubheadings.add(oncology);
		internalMedicineSubheadings.add(neurology);
		internalMedicineSubheadings.add(allergy);
		internalMedicineSubheadings.add(deficiency);
		internalMedicineSubheadings.add(endocrine);
		internalMedicineSubheadings.add(cardiovascular);
		internalMedicineSubheadings.add(pulmonary);
		internalMedicineSubheadings.add(gastrointestinal);
		internalMedicineSubheadings.add(excretory);
		internalMedicineSubheadings.add(musculoSkeletal);
		
		Heading internalMedicine = new Heading("internal-medicine");
		internalMedicine.setSubheadings(internalMedicineSubheadings);
		
		Heading ophthalmology = new Heading("ophthalmology");
		Heading otorhinolaryngology = new Heading("otorhinolaryngology");
		Heading obgyn = new Heading("obstetrics-gynecology");
		Heading pediatrics = new Heading("pediatrics");
		Heading dentistry = new Heading("dentistry");
		Heading dermatology = new Heading("dermatology");
		
		Heading prescription = new Heading("prescription");
		Heading diet = new Heading("diet");
		
		List<Heading> therapeuticsSubheadings = new ArrayList<Heading>();
		therapeuticsSubheadings.add(diet);
		therapeuticsSubheadings.add(prescription);
		
		Heading therapeutics = new Heading("therapeutics");
		therapeutics.setSubheadings(therapeuticsSubheadings);
		
		Heading nutrition = new Heading("nutrition");
		
		Heading anesthesiology = new Heading("anesthesiology");
		Heading orthopedics = new Heading("orthopedics");
		Heading transplantation = new Heading("transplantation");
		Heading surgicalTechnique = new Heading("surgical-technique");
		
		List<Heading> surgerySubheadings = new ArrayList<Heading>();
		surgerySubheadings.add(surgicalTechnique);
		surgerySubheadings.add(orthopedics);
		surgerySubheadings.add(anesthesiology);
		surgerySubheadings.add(transplantation);
		
		Heading surgery = new Heading("surgery");
		surgery.setSubheadings(surgerySubheadings);
		
		Heading pharmacology = new Heading("pharmacology");
		
		List<Heading> medicineSubheadings = new ArrayList<Heading>();
		medicineSubheadings.add(therapeutics);
		medicineSubheadings.add(nutrition);
		medicineSubheadings.add(dermatology);
		medicineSubheadings.add(dentistry);
		medicineSubheadings.add(pediatrics);
		medicineSubheadings.add(obgyn);
		medicineSubheadings.add(otorhinolaryngology);
		medicineSubheadings.add(ophthalmology);
		medicineSubheadings.add(internalMedicine);
		medicineSubheadings.add(pathology);
		medicineSubheadings.add(toxicology);
		medicineSubheadings.add(publicHealth);
		medicineSubheadings.add(surgery);
		medicineSubheadings.add(biomedicalInstrumentation);
		medicineSubheadings.add(pharmacology);
		
		Heading medicineHead = new Heading("medicine");
		medicineHead.setSubheadings(medicineSubheadings);
		
		medicine = new Hierarchy(medicineHead);
		
		//SOCIAL SCIENCES HEAD
		
		Heading accounting = new Heading("accounting");
		Heading banking = new Heading("banking");
		Heading investment = new Heading("investment");
		
		List<Heading> financeSubheadings= new ArrayList<Heading>();
		financeSubheadings.add(investment);
		financeSubheadings.add(banking);
		financeSubheadings.add(accounting);
		
		Heading finance = new Heading("finance");
		finance.setSubheadings(financeSubheadings);
		
		Heading media = new Heading("media");
		
		Heading economics = new Heading("economics");
		Heading statistics = new Heading("statistics");
		
		Heading professions = new Heading("professions");
		Heading management = new Heading("management");
		Heading skills = new Heading("skills");
		
		List<Heading> laborSubheadings = new ArrayList<Heading>();
		laborSubheadings.add(management);
		laborSubheadings.add(professions);
		laborSubheadings.add(skills);
		
		Heading labor = new Heading("labor");
		labor.setSubheadings(laborSubheadings);
		
		Heading industry = new Heading("industry");

		Heading transportation = new Heading("transportation");
		
		Heading trade = new Heading("trade");
		Heading business = new Heading("business");
		
		List<Heading> commerceSubheadings = new ArrayList<Heading>();
		commerceSubheadings.add(business);
		commerceSubheadings.add(trade);
		
		Heading commerce = new Heading("commerce");
		commerce.setSubheadings(commerceSubheadings);
		
		Heading revenue = new Heading("revenue");
		
		List<Heading> publicFinanceSubheadings = new ArrayList<Heading>();
		publicFinanceSubheadings.add(revenue);
		
		Heading publicFinance = new Heading("public-finance");
		publicFinance.setSubheadings(publicFinanceSubheadings);
		
		List<Heading> socialScienceSubheadings = new ArrayList<Heading>();
		socialScienceSubheadings.add(commerce);
		socialScienceSubheadings.add(transportation);
		socialScienceSubheadings.add(industry);
		socialScienceSubheadings.add(labor);
		socialScienceSubheadings.add(statistics);
		socialScienceSubheadings.add(economics);
		socialScienceSubheadings.add(media);
		socialScienceSubheadings.add(finance);
		socialScienceSubheadings.add(publicFinance);
		
		Heading socialScienceHead = new Heading("social-science");
		socialScienceHead.setSubheadings(socialScienceSubheadings);
		
		socialScience = new Hierarchy(socialScienceHead);
		
		//GEOGRAPHY HEAD
				
		Heading waterbody = new Heading("waterbody");
		Heading landform = new Heading("landform");
		Heading naturalDisaster = new Heading("natural-disaster");
		
		List<Heading> physicalGeographySubheadings = new ArrayList<Heading>();
		physicalGeographySubheadings.add(naturalDisaster);
		physicalGeographySubheadings.add(landform);
		physicalGeographySubheadings.add(waterbody);
		
		Heading physicalGeography = new Heading("physical-geography");
		physicalGeography.setSubheadings(physicalGeographySubheadings);
		
		Heading oceanography = new Heading("oceanography");
		Heading environment = new Heading("environment");
		Heading cartography = new Heading("cartography");
		
		List<Heading> geographySubheadings = new ArrayList<Heading>();
		geographySubheadings.add(oceanography);
		geographySubheadings.add(physicalGeography);
		geographySubheadings.add(environment);
		geographySubheadings.add(cartography);
		
		Heading geographyHead = new Heading("geography");
		geographyHead.setSubheadings(geographySubheadings);
		
		geography = new Hierarchy(geographyHead);
		
		//PSYCHOLOGY HEAD
		
		Heading emotion = new Heading("emotion");
		Heading consciousness = new Heading("consciousness");
		Heading behavior = new Heading("behavior");
		Heading disorder = new Heading("disorder");
		
		List<Heading> psychologySubheadings = new ArrayList<Heading>();
		psychologySubheadings.add(consciousness);
		psychologySubheadings.add(emotion);
		psychologySubheadings.add(behavior);
		psychologySubheadings.add(disorder);
		
		Heading psychologyHead = new Heading("psychology");
		psychologyHead.setSubheadings(psychologySubheadings);
		
		psychology = new Hierarchy(psychologyHead);
		
		//PHILOSOPHY HEAD
		
		Heading philosophyHead = new Heading("philosophy");
		
		philosophy = new Hierarchy(philosophyHead);
		
		//MILITARY HEADING
		
		Heading weaponry = new Heading("weaponry");
		Heading reconnaisance = new Heading("reconnaisance");
		
		List<Heading> militarySubheadings = new ArrayList<Heading>();
		militarySubheadings.add(reconnaisance);
		militarySubheadings.add(weaponry);
		
		Heading militaryHead = new Heading("military");
		militaryHead.setSubheadings(militarySubheadings);
		
		military = new Hierarchy(militaryHead);
		
		//HISTORY HEAD
		
		Heading archeology = new Heading("archeology");
		Heading anthropology = new Heading("anthropology");
		
		List<Heading> historySubheadings = new ArrayList<Heading>();
		historySubheadings.add(archeology);
		historySubheadings.add(anthropology);
		
		Heading historyHead = new Heading("history");
		historyHead.setSubheadings(historySubheadings);
		
		history = new Hierarchy(historyHead);
		
		//RELIGION HEAD
		
		Heading religionHead = new Heading("religion");
		religion = new Hierarchy(religionHead);
		
		//POLITICAL SCIENCE HEADING
		
		Heading activism = new Heading("activism");
		Heading politicalSystem = new Heading("political-system");
				
		Heading governmentPolicy = new Heading("government-policy");
		
		List<Heading> governmentSubheadings = new ArrayList<Heading>();
		governmentSubheadings.add(governmentPolicy);
		
		Heading government = new Heading("government");
		government.setSubheadings(governmentSubheadings);
		
		List<Heading> politicalScienceSubheadings = new ArrayList<Heading>();
		politicalScienceSubheadings.add(government);
		politicalScienceSubheadings.add(activism);
		politicalScienceSubheadings.add(politicalSystem);

		Heading politicalScienceHead = new Heading("political-science");
		politicalScienceHead.setSubheadings(politicalScienceSubheadings);
		
		politicalScience = new Hierarchy(politicalScienceHead);
		
		//LAW HEADING
		
		Heading legalProcedure = new Heading("legal-procedure");
		Heading courtOrder = new Heading("court-order");
		Heading civilLaw = new Heading("civil-law");
		Heading corporateLaw = new Heading("corporate-law");
		Heading criminalLaw = new Heading("criminal-law");
		
		List<Heading> lawSubheadings = new ArrayList<Heading>();
		lawSubheadings.add(corporateLaw);
		lawSubheadings.add(courtOrder);
		lawSubheadings.add(criminalLaw);
		lawSubheadings.add(civilLaw);
		lawSubheadings.add(legalProcedure);
		
		Heading lawHead = new Heading("law");
		lawHead.setSubheadings(lawSubheadings);
		
		law = new Hierarchy(lawHead);
		
		//SPORT HEADING
		
		Heading sportHead = new Heading("sport");
		sport = new Hierarchy(sportHead);
		
		//EDUCATION HEADING
		
		Heading teaching = new Heading("teaching");
		
		List<Heading> educationSubheadings = new ArrayList<Heading>();
		educationSubheadings.add(teaching);
		
		Heading educationHead = new Heading("education");
		educationHead.setSubheadings(educationSubheadings);
		
		education = new Hierarchy(educationHead);
		
		//ARTS HEADING
		
		Heading visualArts = new Heading("visual-arts");
		Heading performingArts = new Heading("performing-arts");
		
		List<Heading> artsSubheading = new ArrayList<Heading>();
		artsSubheading.add(performingArts);
		artsSubheading.add(visualArts);
		
		Heading artsHead = new Heading("arts");
		artsHead.setSubheadings(artsSubheading);
		
		arts = new Hierarchy(artsHead);
		
		//LANGUAGE HEADING
		
		Heading languageHead = new Heading("language");
		language = new Hierarchy(languageHead);
	}
	
	/**
	 * This creates a semantic distance map for all pairs of
	 * subheadings which have something in common. For example,
	 * education deals with psychology, esp. of children and these
	 * are paired together as having something in common. The objective is 
	 * to create two different groups, one group (the larger one) where the subheading
	 * pairs have close to nothing in common (military and agriculture for example)
	 * and the other group (such as the ones created here) that Do have
	 * something in common
	 */
	private void populateSemanticDistancesMap() {
		setOfEquidistantHeadings = new HashSet<Set<Heading>>();
		
		Set<Heading> eduAndPsy = new HashSet<Heading>(); 
		eduAndPsy.add(education.getRoot());
		eduAndPsy.add(psychology.getRoot());
		
		setOfEquidistantHeadings.add(eduAndPsy);
		
		Set<Heading> lawAndSci = new HashSet<Heading>();
		lawAndSci.add(law.getRoot());
		lawAndSci.add(science.getRoot());
		
		setOfEquidistantHeadings.add(lawAndSci);
		
		Set<Heading> relAndHist = new HashSet<Heading>();
		relAndHist.add(religion.getRoot());
		relAndHist.add(history.getRoot());
		
		setOfEquidistantHeadings.add(relAndHist);
		
		Set<Heading> relAndPhil = new HashSet<Heading>();
		relAndPhil.add(religion.getRoot());
		relAndPhil.add(philosophy.getRoot());
		
		setOfEquidistantHeadings.add(relAndPhil);
		
		Set<Heading> relAndArts = new HashSet<Heading>();
		relAndArts.add(religion.getRoot());
		relAndArts.add(arts.getRoot());
		
		setOfEquidistantHeadings.add(relAndArts);
	
		Set<Heading> relAndLaw = new HashSet<Heading>();
		relAndLaw.add(religion.getRoot());
		relAndLaw.add(law.getRoot());
		
		setOfEquidistantHeadings.add(relAndLaw);
		
		Set<Heading> polAndHist = new HashSet<Heading>();
		polAndHist.add(politicalScience.getRoot());
		polAndHist.add(history.getRoot());
		
		setOfEquidistantHeadings.add(polAndHist);

		Set<Heading> polAndPhil = new HashSet<Heading>();
		polAndPhil.add(politicalScience.getRoot());
		polAndPhil.add(philosophy.getRoot());
		
		setOfEquidistantHeadings.add(polAndPhil);

		Set<Heading> polAndMil = new HashSet<Heading>();
		polAndMil.add(politicalScience.getRoot());
		polAndMil.add(military.getRoot());
		
		setOfEquidistantHeadings.add(polAndMil);

		Set<Heading> histAndGeog = new HashSet<Heading>();
		histAndGeog.add(history.getRoot());
		histAndGeog.add(geography.getRoot());

		setOfEquidistantHeadings.add(histAndGeog);

		Set<Heading> histAndMil = new HashSet<Heading>();
		histAndMil.add(history.getRoot());
		histAndMil.add(military.getRoot());

		setOfEquidistantHeadings.add(histAndMil);

		Set<Heading> histAndSci = new HashSet<Heading>();
		histAndSci.add(history.getRoot());
		histAndSci.add(science.getRoot());

		setOfEquidistantHeadings.add(histAndSci);
		
		Set<Heading> psyAndMed = new HashSet<Heading>();
		psyAndMed.add(psychology.getRoot());
		psyAndMed.add(medicine.getRoot());

		setOfEquidistantHeadings.add(psyAndMed);

		Set<Heading> psyAndSci = new HashSet<Heading>();
		psyAndSci.add(psychology.getRoot());
		psyAndSci.add(science.getRoot());

		setOfEquidistantHeadings.add(psyAndSci);
		
		Set<Heading> geogAndSci = new HashSet<Heading>();
		geogAndSci.add(geography.getRoot());
		geogAndSci.add(science.getRoot());

		setOfEquidistantHeadings.add(geogAndSci);
		
		Set<Heading> socAndSci = new HashSet<Heading>();
		socAndSci.add(socialScience.getRoot());
		socAndSci.add(science.getRoot());

		setOfEquidistantHeadings.add(socAndSci);
		
		Set<Heading> socAndTech = new HashSet<Heading>();
		socAndTech.add(socialScience.getRoot());
		socAndTech.add(technology.getRoot());

		setOfEquidistantHeadings.add(socAndTech);
		
		Set<Heading> medAndSci = new HashSet<Heading>();
		medAndSci.add(medicine.getRoot());
		medAndSci.add(science.getRoot());

		setOfEquidistantHeadings.add(medAndSci);

		Set<Heading> medAndTech = new HashSet<Heading>();
		medAndTech.add(medicine.getRoot());
		medAndTech.add(technology.getRoot());

		setOfEquidistantHeadings.add(medAndTech);

		Set<Heading> sciAndAgri = new HashSet<Heading>();
		sciAndAgri.add(agriculture.getRoot());
		sciAndAgri.add(science.getRoot());

		setOfEquidistantHeadings.add(sciAndAgri);

		Set<Heading> sciAndTech = new HashSet<Heading>();
		sciAndTech.add(technology.getRoot());
		sciAndTech.add(science.getRoot());

		setOfEquidistantHeadings.add(sciAndTech);

		Set<Heading> sciAndMil = new HashSet<Heading>();
		sciAndMil.add(military.getRoot());
		sciAndMil.add(science.getRoot());

		setOfEquidistantHeadings.add(sciAndMil);

		Set<Heading> sciAndSport = new HashSet<Heading>();
		sciAndSport.add(sport.getRoot());
		sciAndSport.add(science.getRoot());

		setOfEquidistantHeadings.add(sciAndSport);

		Set<Heading> techAndAgri = new HashSet<Heading>();
		techAndAgri.add(agriculture.getRoot());
		techAndAgri.add(technology.getRoot());

		setOfEquidistantHeadings.add(techAndAgri);

		Set<Heading> techAndMil = new HashSet<Heading>();
		techAndMil.add(military.getRoot());
		techAndMil.add(technology.getRoot());

		setOfEquidistantHeadings.add(techAndMil);
	}

	/**
	 * This method calculates the median
	 * of the number of authors associated with each keyword
	 */
	private void computeKeywordUsageStatistics() {
		List<Integer> usageCountList = new ArrayList<Integer>();
		Integer sumOfUsageCounts = 0;
		int usageCountListSize;
		for(String kw : k2aMap.keySet()){
			Integer count = k2aMap.get(kw);
			sumOfUsageCounts += count;
			usageCountList.add(count);
		}
		Collections.sort(usageCountList);
		usageCountListSize = usageCountList.size();
		if(usageCountListSize%2 == 0){
			int midpoint1 = (usageCountListSize/2);
			int midpoint2 = midpoint1 - 1;
			medianUsageCount = ((double)(usageCountList.get(midpoint2)) + (double)(usageCountList.get(midpoint1)))/2.0;
		} else {
			int midpoint = (usageCountListSize - 1)/2;
			medianUsageCount = (double)(usageCountList.get(midpoint));
		}
		
		log.info("MedianUsage: " + medianUsageCount);
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
			try {
				csvReader.close();
			} catch (IOException e) {
				log.error("IOException: Error closing CSV Reader!!");
				e.printStackTrace();
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

	private Map<String, Integer> transformArrayToHashMap(String[][] array) {
		Map<String, Integer> hashMap = new HashMap<String, Integer>();
		String key;
		int valueCount;
		for (int i = 0; i < array.length; i++) {
			key = array[i][0];
			valueCount = Integer.parseInt(array[i][1].trim());
			if(valueCount <= 1){
				continue;
			}
			hashMap.put(key, valueCount);
		}

		return hashMap;
	}
	
	private Map<String, String> transformKeywordArrayToHashMap(String[][] array) {
		Map<String, String> hashMap = new HashMap<String, String>();
		String relation = "", weight = "", hier = "";

		for (int i = 0; i < array.length; i++) {
			String[] components = array[i][0].split(";");
			if (components.length > 1) {
				relation = components[0];
				weight = components[1];
				if (weight.length() > 0) {
					if(components.length == 3){
						hier = components[2];
					}
					hashMap.put(relation, weight + "\t\t||\t" + hier);
				}
			}
		}

		return hashMap;
	}
	
	/**
	 * This method elicits user input for every keyword, asking them to
	 * enter an appropriate subheading, then it computes the path
	 * to the root of the hierarchy that the keyword has been assigned to
	 * as well as the metric for that keywords and writes both of these
	 * to an output CSV file. This should be the core of a web app
	 * that uses AJAX for autocompletion as the user types in the subheading
	 * in the text field
	 * @param pw
	 */
	private void cycleThroughKeywords(PrintWriter pw) {
		String commandLineInput, outputLine = "";
		Scanner scanIn = new Scanner(System.in);
		for(String kw : mapOfUsefulKeywordsToWeights.keySet()){
			if(k2aMap.get(kw) == null || k2aMap.get(kw) <= 1){
				continue;
			}
			List<Heading> path = getPathForHeading(new Heading(kw));
			if(path.size() == 0){
				System.out.println("Enter a subheading for the keyword \"" + kw + "\":" );
				commandLineInput = scanIn.nextLine();
				path = getPathForHeading(new Heading(commandLineInput));
			} 
			outputLine = kw + "," +  calculateWeightForKeyword(kw, path) + "," + pathToString(path);
			pw.println(outputLine);
			log.info(outputLine);
		}
		scanIn.close();
	}
	
	/**
	 * This method computes the metric for the given keyword. The metric is computed as
	 * 		  		L
	 * W = 		---------
	 * 			ln(U)/ln(M)
	 * 
	 * W is the Weight or the Metric of the Keyword that is computed
	 * L is the Length of the Path in the Hierarchy that the keyword is assigned to
	 * U is the Usage of the Keyword, as in how many authors have used the keyword
	 * M is the Median of the Number of Authors who have used all the keywords in our collection
	 * @param kw
	 * @param path
	 * @return
	 */
	private double calculateWeightForKeyword(String kw, List<Heading> path) {
		double metric = 0.0;
		double usageCount;
		double logUsageCount;
		
		if(k2aMap.get(kw) == null){
			return metric;
		} else {
			usageCount = (double)(k2aMap.get(kw));
			logUsageCount = Math.log(usageCount)/Math.log(medianUsageCount);
		}
		int pathLength = path.size();
		
		metric = ((double)pathLength/(double)logUsageCount);
		log.info("Usage: " + usageCount + "\tLogUsageCount: " + logUsageCount + "\tPathLength: " + pathLength + "\tMetric: " + metric);
		return metric;
	}

	
	/**
	 * This method computes the path to the root of a hierarchy
	 * of the heading the keyword is assigned to
	 * For example if the keyword is assigned to "veterinary"
	 * subheading, then the path to the root will return
	 * veterinary - animal-culture - agriculture.
	 * Because "veterinary" comes under the "agriculture" hierarchy
	 * @param heading
	 * @return
	 */
	private List<Heading> getPathForHeading(Heading heading){
		List<Heading> path = new ArrayList<Heading>();
		for(Hierarchy hier : getHierarchyArray()){
			if(hier.getHeading(heading) != null){
				Heading h = hier.getHeading(heading);
				path = hier.findPathToRoot(new ArrayList<Heading>(), h);
			}
		}
		return path;
	}

	/**
	 * This method computes a representation format for the path
	 * to the root of the hierarchy. An example output will look like
	 * veterinary:animal-culture:agriculture
	 * @param path
	 * @return
	 */
	private String pathToString(List<Heading> path) {
		String pathStr = "";
		if(path.size() == 0){
			return pathStr;
		}
		int i = 0;
		for(i = 0; i < path.size() - 1; i++){
			pathStr += path.get(i) + ":";
		}
		pathStr += path.get(i);
		return pathStr;
	}
	
	/**
	 * The method that  elicits user input for each keyword
	 * @param loch
	 * @param pw
	 */
	private void elicitUserAnnotationForKeywords(PrintWriter pw) {
		try {
			pw = new PrintWriter(new File(OUTPUT_KEYWORD_SCORES_FILE));
			cycleThroughKeywords(pw);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally{
			if(pw != null){
				pw.flush();
				pw.close();
			}
		}
	}
	
	/**
	 * This method computes the semantic distance between the given pair of
	 * subheadings. For this, it uses the @param setOfEquidistantHeadings
	 * @param hs1
	 * @param hs2
	 * @return
	 */
	public int computeSemanticDistanceBetweenHeadings(String hs1, String hs2){
		
		if(hs1 == null || hs2 == null || hs1.trim().length() == 0 || hs2.trim().length() == 0){
			return 0;
		}
		
		Heading h1 = new Heading(hs1);
		Heading h2 = new Heading(hs2);
		
		List<Heading> pathForHeading1 = getPathForHeading(h1);
		List<Heading> pathForHeading2 = getPathForHeading(h2);
		
		if(pathForHeading1.size() == 0 || pathForHeading2.size() == 0){
			return 0;
		}
		
		Heading rootForHeading1 = pathForHeading1.get(pathForHeading1.size() - 1);
		Heading rootForHeading2 = pathForHeading2.get(pathForHeading2.size() - 1);
		
		if(rootForHeading1.equals(rootForHeading2)){
			return 0;
		}
		
		int distance = pathForHeading1.size() + pathForHeading2.size() + 2;
		
		for(Set<Heading> headings : setOfEquidistantHeadings){
			if(headings.contains(rootForHeading1) && headings.contains(rootForHeading2)){
				distance -= 1;
				break;
			}
		}
		
		return distance;
	}

	public static void main(String[] args){
		AcaAnaLogger.initLogger();
		LocHierarchy loch = new LocHierarchy();
		Scanner scanIn = new Scanner(System.in);
		String commandLineInput;
		
		String[][] usefulKeywordsArray = loch.readCSVFileIntoMatrix(INPUT_KEYWORD_SCORES_FILE);
		loch.mapOfUsefulKeywordsToWeights = loch.transformKeywordArrayToHashMap(usefulKeywordsArray);
		
		PrintWriter pw = null;
		for(Hierarchy h : loch.getHierarchyArray()){
			log.debug(h.toString());
		}

		System.out.print("DO YOU WANT TO RE-ANNOTATE THE KEYWORD LIST? (Y/N): ");
		commandLineInput = scanIn.nextLine();
		if(commandLineInput.matches("(^Y.*)|(^y.*)")){ //TO AVOID ACCIDENTAL OVERWRITING
			loch.elicitUserAnnotationForKeywords(pw);
		}
		scanIn.close();
	}
}
