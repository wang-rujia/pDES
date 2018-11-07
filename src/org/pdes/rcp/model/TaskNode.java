/*
 * Copyright (c) 2016, Design Engineering Laboratory, The University of Tokyo.
 * All rights reserved. 
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the project nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE PROJECT AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE PROJECT OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package org.pdes.rcp.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.pdes.rcp.model.base.NodeElement;
import org.pdes.util.Delay;
import org.pdes.util.Rework;

/**
 * This is the TaskNode class.<br>
 * @author Taiga Mitsuyuki <mitsuyuki@sys.t.u-tokyo.ac.jp>
 */
public class TaskNode extends NodeElement {

	private static final long serialVersionUID = -3000391819934038992L;
	
	////////////////Variables//////////////////////////////////////////////////////////
	private String name = "";
	private int workAmount;
	private double progress;
	private int additionalWorkAmount; //Additional work amount if the amount of error exceeds the limit.
	private boolean needFacility; //Need facility or not
	
	private Map<Integer,Double> minimumWorkAmountMap = new LinkedHashMap<Integer,Double>();
	
	private Rework rework = new Rework();
	private Delay delay = new Delay();
	//////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * This is the constructor.
	 */
	public TaskNode(){
		String newName = "New Task";
		this.setName(newName);
		this.setWorkAmount(10);
		this.setProgress(0.0);
		setNeedFacility(false);
	}
	
	/**
	 * Get the name of TaskNode.
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of TaskNode.
	 * @param name the name to set
	 */
	public void setName(String name) {
		String old = this.name;
		this.name=name;
		firePropertyChange("name",old,name);
	}
	

	/**
	 * Get the work amount of TaskNode.
	 * @return the workAmount
	 */
	public int getWorkAmount() {
		return workAmount;
	}
	
	/**
	 * Get the progress of TaskNode
	 * @return the progress from 0.0 to 1.0
	 */
	public double getProgress() {
		return progress;
	}
	

	/**
	 * Set the work amount of TaskNode.
	 * @param workAmount the workAmount to set
	 */
	public void setWorkAmount(int workAmount) {
		int old = this.workAmount;
		this.workAmount = workAmount;
		firePropertyChange("workAmount",old,workAmount);
	}
	
	/**
	 * Set the progress of TaskNode.
	 * @param progress the progress to set
	 */
	public void setProgress(double progress) {
		double old = this.progress;
		this.progress = progress;
		firePropertyChange("progress",old,progress);
	}
	
	/**
	 * Get additional work amount of TaskNode.
	 * @return the additionalWorkAmount
	 */
	public int getAdditionalWorkAmount() {
		return additionalWorkAmount;
	}

	/**
	 * Set additional work amount of TaskNode.
	 * @param additionalWorkAmount the additionalWorkAmount to set
	 */
	public void setAdditionalWorkAmount(int additionalWorkAmount) {
		int old = this.additionalWorkAmount;
		this.additionalWorkAmount = additionalWorkAmount;
		firePropertyChange("additionalWorkAmount", old, additionalWorkAmount);
	}
	
	/**
	 * Get the information whether this TaskNode needs facility or not.
	 * @return the needFacility
	 */
	public boolean isNeedFacility() {
		return needFacility;
	}

	/**
	 * Set the information whether this TaskNode needs facility or not.
	 * @param needFacility the needFacility to set
	 */
	public void setNeedFacility(boolean needFacility) {
		boolean old = this.needFacility;
		this.needFacility = needFacility;
		firePropertyChange("needFacility", old, needFacility);
	}
	
	public void addMinimumWorkAmount(int o,double mwa){
		minimumWorkAmountMap.put(o, mwa);
		ArrayList<Entry<Integer, Double>> sortList = new ArrayList<Map.Entry<Integer,Double>>(minimumWorkAmountMap.entrySet());
		sortList.sort((o1,o2) -> {
			return Integer.compare(o1.getKey(), o2.getKey());
		});
		minimumWorkAmountMap.clear();
		for (Map.Entry<Integer, Double> entry: sortList){
			minimumWorkAmountMap.put(entry.getKey(), entry.getValue());
		}
	}
	
	public void addReworkInfo(int a, double b, double c, String d){
		rework.addNewValue(a, b, c, d);
	}
	
	public void addDelayInfo(int a, double b, int c){
		delay.addNewValue(a, b, c);
	}
	
	public void deleteReworkInfo(int a, double b, double c, String d){
		rework.removeValue(a, b, c, d);
	}
	
	public void deleteDelayInfo(int a, double b, int c){
		delay.removeValue(a, b, c);
	}
	
	public Map<Integer,Double> getMinimumWorkAmountMap(){
		return this.minimumWorkAmountMap;
	}
	
	public Rework getRework(){
		return this.rework;
	}
	
	public Delay getDelay(){
		return this.delay;
	}
	
	public void deleteMinimumWorkAmountInfo(int i){
		minimumWorkAmountMap.remove(i);
	}
	
}
