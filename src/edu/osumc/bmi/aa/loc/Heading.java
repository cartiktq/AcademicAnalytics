package edu.osumc.bmi.aa.loc;

import java.util.Comparator;
import java.util.List;

public class Heading implements Comparator<Heading>{

	private String name;
	private Heading superheading;
	private List<Heading> subheadings;
	
	public Heading getSuperheading(){
		return superheading;
	}
	
	public void setSuperheading(Heading sh){
		superheading = sh;
	}
	
	public List<Heading> getSubheadings(){
		return subheadings;
	}

	public void setSubheadings(List<Heading> sh){
		subheadings = sh;
		for(Heading s : sh){
			s.setSuperheading(this);
		}
	}
	
	public Heading getSubheading(int index){
		return subheadings.get(index);
	}
	
	public void addSubheading(Heading subheading){
		subheadings.add(subheading);
		subheading.setSuperheading(this);
	}
	
	public String getName(){
		return name;
	}
	
	public Heading(String name){
		this.name = name;
	}
	
	@Override
	public boolean equals(Object o){
		if(o != null && o instanceof Heading){
			Heading h = (Heading)o;
			return this.getName().equals(h.getName());
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		int hash = 5;
        hash = hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
	}
	
	@Override
	public String toString(){
		return name;
	}

	@Override
	public int compare(Heading o1, Heading o2) {
		return o1.name.compareTo(o2.name);
	}
}
