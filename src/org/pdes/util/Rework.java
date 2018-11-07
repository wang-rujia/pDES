package org.pdes.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Rework {
	private List<Integer> o;
	private List<Double> progress;
	private List<Double> possibility;
	private List<String> From;
	
	public Rework(){
		o = new ArrayList<Integer>();
		progress = new ArrayList<Double>();
		possibility = new ArrayList<Double>();
		From = new ArrayList<String>();
	}
	
	public void addNewValue(int a, double b, double c, String d){
		o.add(a);
		progress.add(b);
		possibility.add(c);
		From.add(d);
	}
	
	public void removeValue(int a, double b, double c, String d){
		for(int i=0;i<this.getSize();i++){
			if(o.get(i)==a && progress.get(i)==b && possibility.get(i)==c && From.get(i).equals(d)){
				o.remove(i);
				progress.remove(i);
				possibility.remove(i);
				From.remove(i);
				break;
			}
		}
	}
	
	public Rework duplicate(){
		Rework dupli = new Rework();
		boolean ifExist;
		for(int i=0; i < this.o.size();i++){
			ifExist = false;
			for(int j=0; j<dupli.getOList().size();j++){
				if(this.o.get(i)==dupli.getOList().get(j) &&
					this.progress.get(i)>dupli.getProgressList().get(j)-0.00001 && this.progress.get(i)<dupli.getProgressList().get(j)+0.00001 &&
					this.From.get(i).equals(dupli.getFromList().get(j))) ifExist=true;
			}
			if(!ifExist)
				dupli.addNewValue(this.o.get(i), this.progress.get(i), 
						this.possibility.get(i), this.From.get(i));
		}
		return dupli;
	}
	
	public String getInfoByIndex(int i){
		String info = o.get(i).toString()+","+progress.get(i)+","+possibility.get(i).toString()+","+From.get(i);
		return info;
	}
	
	public int getSize(){
		return o.size();
	}
	
	public List<Integer> getOList(){
		return this.o;
	}
	
	public List<Double> getProgressList(){
		return this.progress;
	}
	
	public List<String> getFromList(){
		return this.From;
	}
	
	public List<Double> getPossibilityList(){
		return this.possibility;
	}
	
	public List<Double> getPossibility(int oc, double pro){
		List<Double> l = new ArrayList<Double>();
		for(int i=0;i<o.size();i++){
			if(o.get(i)==oc && progress.get(i)==pro) l.add(possibility.get(i));
		}
		return l;
	}
	
	public Map<Double, String> getReworkMap(int oc, double pro){
		pro=Math.floor(pro*10)/10;
		Map<Double, String> m = new LinkedHashMap<Double, String>();
		double key=0;
		for(int i=0;i<o.size();i++){
			if(o.get(i)==oc && progress.get(i)>pro-0.0001 && progress.get(i)<pro+0.0001) {
				key += possibility.get(i);
				m.put(key, From.get(i));
			}
		}
		return m;
	}
	
	public String getFrom(int oc, double pro){
		for(int i=0;i<o.size();i++){
			if(o.get(i)==oc && progress.get(i)==pro) return From.get(i);
		}
		return null;
	}
}
