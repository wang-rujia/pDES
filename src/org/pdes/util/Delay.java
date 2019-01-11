package org.pdes.util;

import java.util.ArrayList;
import java.util.List;

public class Delay {
	private List<Integer> o;
	private List<Double> possibility;
	private List<Integer> awa;
	
	public Delay(){
		o = new ArrayList<Integer>();
		possibility = new ArrayList<Double>();
		awa = new ArrayList<Integer>();
	}
	
	public void clear(){
		o.clear();
		possibility.clear();
		awa.clear();
	}
	
	public void addNewValue(int a, double b, int c){
		o.add(a);
		possibility.add(b);
		awa.add(c);
	}
	
	public void removeValue(int a, double b, int c){
		for(int i=0;i<this.getSize();i++){
			if(o.get(i)==a &&  possibility.get(i)==b && awa.get(i).equals(c)){
				o.remove(i);
				possibility.remove(i);
				awa.remove(i);
				break;
			}
		}
	}
	
	/*
	public Map<Double,Integer> getDelayMap(int oc){
		Map<Double, Integer> m = new LinkedHashMap<Double, Integer>();
		for(int i=0; i<o.size();i++){
			if(o.get(i)==oc) m.put(possibility.get(i),awa.get(i));
		}
		ArrayList<Entry<Double, Integer>> list = new ArrayList<Map.Entry<Double, Integer>>(m.entrySet());
		list.sort((e1,e2) -> {
			return Double.compare(e1.getKey(), e2.getKey());
		});
		m.clear();
		for(Map.Entry<Double, Integer> entry: list){
			m.put(entry.getKey(), entry.getValue());
		}
		return m;
	}
	*/
	
	public List<Double> getDpAtO(int oc){
		List<Double> a = new ArrayList<Double>();
		for(int i=0;i<possibility.size();i++){
			if(this.o.get(i)==oc) a.add(possibility.get(i));
		}
		return a;
	}
	
	public List<Integer> getDwaAtO(int oc){
		List<Integer> a = new ArrayList<Integer>();
		for(int i=0;i<possibility.size();i++){
			if(this.o.get(i)==oc) a.add(awa.get(i));
		}
		return a;
	}
	
	public String getInfoByIndex(int i){
		String info = o.get(i).toString()+","+possibility.get(i).toString()+","+awa.get(i).toString();
		return info;
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
