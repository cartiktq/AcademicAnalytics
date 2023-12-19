package edu.osumc.bmi.aa.loc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Hierarchy {

	private Heading root;
	private List<Heading> representation;
	
	public Heading getRoot(){
		return root;
	}
	
	public Hierarchy(Heading root){
		this.root = root;
		createRepresentation();
	}
	
	private void createRepresentation() {
		representation = new ArrayList<Heading>();
		representation.add(root);
		
		addDescendants(root);
	}

	private void addDescendants(Heading h) {
		if(h.getSubheadings() == null){
			return;
		}
		for(Heading sh : h.getSubheadings()){
			representation.add(sh);
			addDescendants(sh);
		}
	}

	public List<Heading> findPathToRoot(List<Heading> path, Heading sh){
		path.add(sh);
		
		if(sh == null || sh.equals(root)){
			return path;
		}

		Heading superH = sh.getSuperheading();
		return findPathToRoot(path, superH);
		
	}

	/**
	 * Tries to match the heading against the headings in all hierarchies
	 * Case-insensitive
	 * @param heading
	 * @return
	 */
	public Heading getHeading(Heading heading) {
		Pattern p1 = Pattern.compile(heading.getName().toLowerCase());
		Pattern p2 = Pattern.compile(heading.getName().toUpperCase());
		Pattern p3 = Pattern.compile(heading.getName());
		for(Heading h : representation){
			Matcher m1 = p1.matcher(h.getName());
			Matcher m2 = p2.matcher(h.getName());
			Matcher m3 = p3.matcher(h.getName());
			if(m1.find() || m2.find() || m3.find()){
				return h;
			}
		} 
		return null;
	}
	
	@Override
	public String toString(){
		String rep = "Hierarchy of " + root.toString().toUpperCase() + ": {";
		for(Heading h : representation){
			rep += h + ", ";
		}
		rep = rep.substring(0, rep.length() - 2) + "}";
		return rep;
	}
}
