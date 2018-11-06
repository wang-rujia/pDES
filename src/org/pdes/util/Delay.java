package org.pdes.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Delay {
	private List<Integer> o;
	private List<Double> possibility;
	private List<Integer> awa;
	
	public Delay(){
		o = new ArrayList<Integer>();
		possibility = new ArrayList<Double>();
		awa = new ArrayList<Integer>();
	}
	
	public void addNewValue(int a, double b, int c){
		o.add(a);
		possibility.add(b);
		awa.add(c);
	}
	
	public Map<Double, Integer> getDelayMap(int oc){
		Map<Double, Integer> m = new LinkedHashMap<Double, Integer>();
		for(int i=0; i<o.size();i++){
			if(o.get(i)==oc){
				m.put(possibility.get(i), awa.get(i));
			}
		}
		return m;
	}
	
	public int getSize(){
		return o.size();
	}
	
	public List<Integer> getOList(){
		return this.o;
	}
	
	public List<Double> getPossibilityList(){
		return this.possibility;
	}
	
	public List<Integer> getAwaList(){
		return this.awa;
	}

}
