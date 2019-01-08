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
package org.pdes.simulator.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.pdes.rcp.model.TaskNode;
import org.pdes.util.Delay;
import org.pdes.util.Rework;

public class Task {
	
	private enum TaskState {
		/** Cannot start task (stateInt=0)*/
		NONE,
		/** Can start task but not start (stateInt=1)*/
		READY,
		/** Doing this task (stateInt=2)*/
		WORKING,
		/** Doing Additional or Exceptional work of this task (stateInt=3)*/
		WORKING_ADDITIONALLY,
		/** Finished this task (stateInt=4)*/
		FINISHED,
	}
	
	// Constraint variables on simulation
	private final String id; // ID
	private final String nodeId; // TaskNode ID
	private final String name;
	private final List<Task> inputTaskList = new ArrayList<>();
	private final List<Task> outputTaskList = new ArrayList<>();
	private Map<Integer, Double> minimumWorkAmount = new LinkedHashMap<Integer, Double>();
	private Rework rework;
	private Delay delay;
	private double defaultWorkAmount;
	private boolean reworkFlag=false;
	
	private boolean wn=false;
	private boolean startTimeAdded=false;
	private List<String> reworkFromLog = new ArrayList<String>();
	private String reworkFrom = "none";
	
	// Changeable variable on simulation
	private int o = 1; //occurrence time
	private double progress = 0.0;  //actual/minimum
	private double est = 0; // Earliest start time
	private double eft = 0; // Earliest finish time
	private double lst = 0; // Latest start time
	private double lft = 0; // Latest finish time
	private double remainingWorkAmount; // remaining work amount
	private double actualWorkAmount; // actual work amount
	private TaskState state = TaskState.NONE; // state of this task
	private int stateInt = 0;
	private List<Integer> readyTimeList = new ArrayList<Integer>(); // list of ready time of one task
	private List<Integer> startTimeList = new ArrayList<Integer>(); // list of start time of one task
	private List<Integer> finishTimeList = new ArrayList<Integer>(); // list of finish time of one task
	private List<Resource> allocatedResourceList = new ArrayList<Resource>();
	private List<Double> resourceCapacityLog = new ArrayList<Double>();
	
	private boolean[][] ifReworked = new boolean[100][100];
	//[occurrenceNumber(1,2,...,10)][progress*10(0,1,2,...,29)]
	
	public Task(TaskNode taskNode) {
		this.id = UUID.randomUUID().toString();
		this.nodeId = taskNode.getId();
		this.name = taskNode.getName();
		this.minimumWorkAmount=taskNode.getMinimumWorkAmountMap();
		this.rework=taskNode.getRework();
		this.delay=taskNode.getDelay();
	}
	
	public void initialize() {
		for(int i=0;i<100;i++){
			for(int j=0;j<100;j++){
				ifReworked[i][j] = false;
			}
		}
		o=1;
		est = 0;
		eft = 0;
		lst = 0;
		lft = 0;
		progress=0.0;
		remainingWorkAmount = minimumWorkAmount.get(1);
		actualWorkAmount = 0;
		state = TaskState.NONE;
		stateInt = 0;
	}
	
	public void initializeForExistingModel(int simNo, Random rand) {
		defaultWorkAmount = generateDuration(minimumWorkAmount.get(1),minimumWorkAmount.get(2),minimumWorkAmount.get(3),simNo,rand);
		actualWorkAmount = 0;
		remainingWorkAmount = defaultWorkAmount;
		wn=false;
		startTimeAdded=false;
	}
	
	/**
	 * Check whether this task has to be READY or not.<br>
	 * If all input tasks are FINISHED and the state of this task is NONE, change the state of this task to READY.
	 */
	public void checkReady(int time) {
		if (isNone() && inputTaskList.stream().allMatch(t -> t.isFinished())){
			state = TaskState.READY;
			stateInt = 1;
			addReadyTime(time);
		}
	}
	
	/**
	 * Check whether the state of this task has to be WORKING or not.<br>
	 * If the state of this task is READY and this task is already allocated someone,
	 * change the state of this task and allocated resources to WORKING, and add the information of start time to this task.
	 * @param time
	 */
	public void checkWorking(int time) {
		if (isReady() && allocatedResourceList.size() > 0) {
			state = TaskState.WORKING;
			stateInt = 2;
			addStartTime(time);
			
			List<Resource> aRLwithoutD=allocatedResourceList.stream()
					.distinct()
					.collect(Collectors.toList());
			
			for(Resource a : aRLwithoutD) {
				a.setStateWorking();
				a.addStartTime(time);
				a.addAssignedTask(this);
			}
			
		}
	}
	
	/**
	 * If remaining work amount of this task is lower than 0, check whether the state of this task has to be FINISHED or not.<br>
	 * Whether additional work is assigned to this task or not is judged by additionalTaskFlag.
	 * If finishing normally, the state of this task is changed to FINISHED and the state of assigned resource is changed to FREE, and record the finish time to this task and assigned resource.
	 * If additional work is assigned, add the additional work amount to remaining work amount and record the start time of additional work.
	 * @param time
	 */
	public void checkFinished(int time, List<Task> allTask) {
		
		if (remainingWorkAmount <= 0) {
			if (isWorkingAdditionally()) {
				boolean ifRework=false;
				Random rand = new Random();
				Double p = rand.nextDouble();
				Map<Double, String> reworkMap = rework.getReworkMap(o,-1.0);
				for(Double pSum : reworkMap.keySet()){
					if(p<pSum){
						String FromName = reworkMap.get(pSum);
						//Task From = searchTaskByName(FromName);
						Task From = null;
						for(Task s : allTask){
							if(s.getName().equals(FromName)) From=s;
						}
						if(!From.equals(null)) {
							ifRework=true;
							From.setInitByRework(From.getOccurrenceTime(),time);
						}
					break;
					}
				}
				if(!ifRework){
					addFinishTime(time);
					remainingWorkAmount = 0;
					state = TaskState.FINISHED;
					stateInt = 4;
				}
				
				List<Resource> aRLwithoutD=allocatedResourceList.stream()
						.distinct()
						.collect(Collectors.toList());
				double rc=0.0;
				for(Resource a : aRLwithoutD) {
					a.setStateFree();
					a.addFinishTime(time);
					rc+=a.getWorkAmountSkillPoint(this);
					allocatedResourceList.remove(a);
				}
				addResourceCapacityLog(rc);
			} else if (isWorking()) {
				Random rand = new Random();
				Double p = rand.nextDouble();
				Map<Double, Integer> DelayMap = delay.getDelayMap(this.o);
				Double pSum = 0.0;
				boolean ifDelay = false;
				for(Double key: DelayMap.keySet()){
					pSum += key;
					if(p<pSum){
						state = TaskState.WORKING_ADDITIONALLY;
						stateInt = 5;
						if(DelayMap.get(key)==0) break;
						remainingWorkAmount += DelayMap.get(key);
						ifDelay = true;
						break;
					}
				}
				if(!ifDelay){
					addFinishTime(time);
					remainingWorkAmount = 0;
					state = TaskState.FINISHED;
					stateInt = 4;
					List<Resource> aRLwithoutD=allocatedResourceList.stream()
							.distinct()
							.collect(Collectors.toList());
					double rc=0.0;
					for(Resource a : aRLwithoutD) {
						a.setStateFree();
						a.addFinishTime(time);
						rc+=a.getWorkAmountSkillPoint(this);
						allocatedResourceList.remove(a);
					}
					addResourceCapacityLog(rc);
				}
			}
		}
	}
	
	public void checkFinishedForExistingModel(int time,List<Task> allTaskList,int simNo) {
		if (isWorking() && remainingWorkAmount <=0) {
			addFinishTime(time);
			remainingWorkAmount = 0;
			state = TaskState.FINISHED;
			stateInt = 4;
			addResourceCapacityLog(1);
			Random rand = new Random();
			Double p=0.0;
				
			Double probability = 0.0;
			Double ri = 0.0;
			Task FromTask = null;
			Double probability2 = 0.0;
			Double ri2 = 0.0;
			Task FromTask2 = null;
			Double delta = -10000.0;
			
			for(String From:rework.getFromList()){
				for(Task a: allTaskList){
					if(a.getName().equals(From)){
						FromTask = a;
						break;
					}
				}
				if(!FromTask.equals(null) && (int)From.charAt(0) < (int)this.name.charAt(0)){
					p=rand.nextDouble();
					probability = rework.getPossibilityList().get(rework.getFromList().indexOf(From));
					ri=rework.getProgressList().get(rework.getFromList().indexOf(From));
					if(p>probability){
						delta = FromTask.remainingWorkAmount;
						FromTask.remainingWorkAmount += FromTask.defaultWorkAmount*ri*FromTask.minimumWorkAmount.get(4);
						if(FromTask.remainingWorkAmount>FromTask.defaultWorkAmount) FromTask.remainingWorkAmount = FromTask.defaultWorkAmount*0.9;
						FromTask.actualWorkAmount=FromTask.defaultWorkAmount-FromTask.remainingWorkAmount;
						delta = FromTask.remainingWorkAmount-delta;
						System.out.println("  "+simNo+"[B]Rework From ["+this.name+"]to["+From+"], add duration: "+delta);
						FromTask.reworkFlag=true;
						if(FromTask.isFinished()) FromTask.setNone();
						for(String From2:FromTask.rework.getFromList()){
							for(Task a: allTaskList){
								if(a.getName().equals(From2)){
									FromTask2 = a;
									break;
								}
							}
							if(!FromTask2.equals(null) && (int)From2.charAt(0) > (int)From.charAt(0) && (int)From2.charAt(0) < (int)this.name.charAt(0)){
								p = rand.nextDouble();
								probability2 = FromTask.rework.getPossibilityList().get(FromTask.rework.getFromList().indexOf(From2));
								ri2=FromTask.rework.getProgressList().get(FromTask.rework.getFromList().indexOf(From2));
								if(p>probability2){
									if(FromTask2.isFinished()) FromTask2.setNone();
									delta = FromTask2.remainingWorkAmount;
									FromTask2.remainingWorkAmount += FromTask2.defaultWorkAmount*ri2*FromTask2.minimumWorkAmount.get(4);
									if(FromTask2.remainingWorkAmount>FromTask2.defaultWorkAmount){
										if(FromTask2.reworkFlag){
											FromTask2.remainingWorkAmount=FromTask2.defaultWorkAmount * 0.9;
										}else{
											FromTask2.remainingWorkAmount=FromTask2.defaultWorkAmount;
										}
									}
									delta = FromTask2.remainingWorkAmount-delta;
									FromTask2.actualWorkAmount=FromTask2.defaultWorkAmount-FromTask2.remainingWorkAmount;
								}
							}
						}
					}
				}
			}
		}
	}
	
	public void checkReworkForExistingModel(List<Task> allTaskList){
		//if rework happends, add additional work amount onto remaining work amount
		Random rand1 = new Random();
		Random rand2 = new Random();
		Double p = 0.0;
		Double p2 = 0.0;
		Double additionalWorkAmount = 0.0;
		// look up from j
		for(int i=0; i< allTaskList.indexOf(this);i++){
			String taskName = allTaskList.get(i).getName();
			p = rework.getDSMValueforExistingModel(taskName);
			if(rand1.nextDouble() < p){
//				this.reworkFrom = allTaskList.get(i).getName();
				if(reworkFrom.equals("none")){
					this.reworkFrom=taskName; 
				}else{
					this.addReworkFrom(taskName);
				}
				additionalWorkAmount = rework.getDSM2ValueForExistingModel(taskName) * allTaskList.get(i).getDefaultWorkAmount() * allTaskList.get(i).minimumWorkAmount.get(4);
				allTaskList.get(i).remainingWorkAmount += additionalWorkAmount;
				//System.out.println("UP:"+"Rework From ["+this.name+"]to["+allTaskList.get(i).getName()+"], add duration: "+additionalWorkAmount);
				if(allTaskList.get(i).remainingWorkAmount>allTaskList.get(i).defaultWorkAmount){
					allTaskList.get(i).remainingWorkAmount = allTaskList.get(i).defaultWorkAmount * 0.9;
				}
				//look down from i
				for(int k = i + 1; k< allTaskList.size(); k++){ 
					if(allTaskList.get(k).remainingWorkAmount < allTaskList.get(k).defaultWorkAmount){
						String taskName2 = allTaskList.get(k).getName();
						p2 = allTaskList.get(i).rework.getDSMValueforExistingModel(taskName2);
						if(rand2.nextDouble() < p2){
							additionalWorkAmount = allTaskList.get(i).rework.getDSM2ValueForExistingModel(taskName2) * allTaskList.get(k).getDefaultWorkAmount() * allTaskList.get(k).minimumWorkAmount.get(4);
							allTaskList.get(k).remainingWorkAmount += additionalWorkAmount;
							//System.out.println("DOWN:"+"Rework From ["+allTaskList.get(i).getName()+"]to["+allTaskList.get(k).getName()+"], add duration: "+additionalWorkAmount);
							if(allTaskList.get(k).remainingWorkAmount>allTaskList.get(k).defaultWorkAmount){
								allTaskList.get(k).remainingWorkAmount = allTaskList.get(k).defaultWorkAmount * 0.9;
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Performing this task by following steps:<br>
	 * 1. Decreasing remaining work amount and adding cost to resource.<br>
	 * 2. Updating error value considering quality skill point.<br>
	 * 3. Judging additional work is occurred or not by checking the error value.(if componentErrorRework is TRUE)
	 * @param time
	 * @param componentErrorRework
	 */
	public void perform(int time, List<Task> allTask, int no) {
		if (isWorking() || isWorkingAdditionally()) {
			double workAmount = 0;
			List<Resource> aRLwithoutD=allocatedResourceList.stream()
					.distinct()
					.collect(Collectors.toList());
			for(Resource a : aRLwithoutD) workAmount += a.getWorkAmountSkillPoint(this);

			actualWorkAmount +=workAmount;
			remainingWorkAmount -= workAmount;
			if(minimumWorkAmount.size()>=o){
				if(minimumWorkAmount.get(this.o)>0){
					progress = Math.floor(actualWorkAmount/minimumWorkAmount.get(this.o)*10)/10;
				}else{
					progress = -1.0;
				}
			}else{
				for(int j=o-1;j>0;j--){
					if(minimumWorkAmount.size()>=j){
						if(minimumWorkAmount.get(j)>0){
//							System.out.println("cannot find correct minimum work amount:"+this.o+", using mwa("+j+")");
							progress = Math.floor(actualWorkAmount/minimumWorkAmount.get(j)*10.0)/10.0;
							break;
						}else{
							progress = -1.0;
						}
					}
				}
			}

			Random rand = new Random();
			Double p = rand.nextDouble();
			int progress10 = (int)(progress*10);
			if(!ifReworked[this.o-1][progress10]){
				ifReworked[this.o-1][progress10] = true;
//				System.out.println(String.valueOf(no)+"[reworking]"+this.name+" oc:"+this.o+" progress:"+progress10);
				if(progress<0) System.out.println(rework.getReworkMap(o, progress));
				Map<Double, String> reworkMap = rework.getReworkMap(o, progress);
				for(Double pSum : reworkMap.keySet()){
					if(p<pSum){		
						String FromName = reworkMap.get(pSum);
						Task From = null;
						for(Task s : allTask) if(s.getName().equals(FromName)) From=s;
						if(!From.equals(null) && From.minimumWorkAmount.containsKey(From.getOccurrenceTime()+1)
								&& From.minimumWorkAmount.get(From.getOccurrenceTime()+1)>0) {
							From.setInitByRework(From.getOccurrenceTime(),time);
						}
						break;
					}
				}
			}
		}
	}
	
	public void performForExistingModel(int timestep) {
		actualWorkAmount += timestep;
		remainingWorkAmount -= timestep;
	}
	
	public Task searchTaskByName(String name){
		for(Task t : this.inputTaskList){
			if(t.getName().equals(name)) {
				return t;
			}else{
				return t.searchTaskByName(name);
			}
		}
		return null;
	}
	
	public void setInitByRework(int oc, int time){
		if(isWorking()||isWorkingAdditionally()){
			this.addFinishTime(time);
			this.setOccurrenceTime(oc+1);
			est = 0;
			eft = 0;
			lst = 0;
			lft = 0;
			progress=0.0;
			
			if(minimumWorkAmount.size()>=oc+1){
				remainingWorkAmount = minimumWorkAmount.get(oc+1);
				progress = actualWorkAmount/minimumWorkAmount.get(oc+1);
			}else{
				for(int j=oc;j>0;j--){
					if(minimumWorkAmount.size()>=j){
						remainingWorkAmount = minimumWorkAmount.get(j)*calPercent();
//						System.out.println(this.getName()+",cannot find correct minimum work amount:"+(oc+1)+", using:"+minimumWorkAmount.get(j)+"*"+calPercent()+"="+remainingWorkAmount);
						progress = actualWorkAmount/minimumWorkAmount.get(j);
						break;
					}
				}
			}
			actualWorkAmount = 0;
			state = TaskState.NONE;
			stateInt = 0;
			
			double rc = 0;
			List<Resource> aRLwithoutD=allocatedResourceList.stream()
					.distinct()
					.collect(Collectors.toList());
			for(Resource a : aRLwithoutD){
				a.setStateFree();
				a.addFinishTime(time);
				rc += a.getWorkAmountSkillPoint(this);
				allocatedResourceList.remove(a);
			}
			this.addResourceCapacityLog(rc);
			
		}else if(isFinished()){
			this.setOccurrenceTime(oc+1);
			est = 0;
			eft = 0;
			lst = 0;
			lft = 0;
			progress=0.0;
			
			if(minimumWorkAmount.size()>=oc+1){
				remainingWorkAmount = minimumWorkAmount.get(oc+1);
			}else{
				for(int j=oc;j>0;j--){
					if(minimumWorkAmount.size()>=j){
						remainingWorkAmount = minimumWorkAmount.get(j)*calPercent();
						//System.out.println(this.getName()+",cannot find correct minimum work amount:"+(oc+1)+", using:"+minimumWorkAmount.get(j)+"*"+calPercent()+"="+remainingWorkAmount);
						break;
					}
				}
			}
			
			actualWorkAmount = 0;
			state = TaskState.NONE;
			stateInt = 0;
			for(Task t : this.outputTaskList) {
				t.setInitByRework(t.getOccurrenceTime(),time);
			}
		}
	}
	
	public void setInitByReworkForExistingModel(int time){
		if(isWorking()){
			state = TaskState.NONE;
			stateInt = 0;
			addFinishTime(time);
		}else{
			for(Task t : this.outputTaskList) t.setInitByReworkForExistingModel(time);
		}
	}
	
	
	public int getOccurrenceTime(){
		return this.o;
	}
	
	public void setOccurrenceTime(int o){
		this.o = o;
	}

	public boolean isNone() {
		return state == TaskState.NONE;
	}

	public boolean isReady() {
		return state == TaskState.READY;
	}

	public boolean isWorking() {
		return state == TaskState.WORKING;
	}

	public boolean isWorkingAdditionally() {
		return state == TaskState.WORKING_ADDITIONALLY;
	}

	public boolean isFinished() {
		return state == TaskState.FINISHED;
	}
	
	public void addInputTask(Task task) {
		inputTaskList.add(task);
	}
	
	public void addOutputTask(Task task) {
		outputTaskList.add(task);
	}
	
	public String getNodeId() {
		return nodeId;
	}
	
	public String getId() {
		return id;
	}
	
	public String getName(){
		return name;
	}
	
	public double getEst() {
		return est;
	}

	public void setEst(double est) {
		this.est = est;
	}

	public double getEft() {
		return eft;
	}

	public void setEft(double eft) {
		this.eft = eft;
	}

	public double getLst() {
		return lst;
	}

	public void setLst(double lst) {
		this.lst = lst;
	}

	public double getLft() {
		return lft;
	}

	public void setLft(double lft) {
		this.lft = lft;
	}

	public double getRemainingWorkAmount() {
		return remainingWorkAmount;
	}

	public double getActualWorkAmount() {
		return actualWorkAmount;
	}
	
	public int getStateInt() {
		return stateInt;
	}
	
	public List<Integer> getReadyTimeList() {
		return readyTimeList;
	}

	public List<Integer> getStartTimeList() {
		return startTimeList;
	}

	public List<Integer> getFinishTimeList() {
		return finishTimeList;
	}
	
	public void addReadyTime(int time) {
		readyTimeList.add(time);
	}
	
	public void addStartTime(int time) {
		startTimeList.add(time);
	}
	
	public void addFinishTime(int time) {
		finishTimeList.add(time);
	}
	
	public List<Task> getInputTaskList() {
		return inputTaskList;
	}

	public List<Task> getOutputTaskList() {
		return outputTaskList;
	}
	
	public List<Resource> getAllocatedResourceList() {
		return allocatedResourceList;
	}

	public boolean isAlreadyAssigned(Resource r) {
		return allocatedResourceList.stream().map(w -> w.getId()).anyMatch(r.getId()::equals);
	}
	
	public void addAllocatedResource(Resource r) {
		this.allocatedResourceList.add(r);
	}
	
	public List<Double> getResourceCapacityLog(){
		return this.resourceCapacityLog;
	}
	
	public void addResourceCapacityLog(double a){
		this.resourceCapacityLog.add(a);
	}
	
	public List<String> getReworkFromLog(){
		return this.reworkFromLog;
	}
	
	public void addReworkFromLog(String a){
		this.reworkFromLog.add(a);
	}
	
	public double getDefaultWorkAmount(){
		return this.defaultWorkAmount;
	}
	
	public double generateDuration(double a,double c,double b,int simNo, Random rand){
		double p=rand.nextDouble();
		double F = (c-a) /(b-a);
		if(p<F){
			return a+Math.sqrt(p *(b-a)*(c-a));
		}else{
			return b-Math.sqrt((1-p)*(b-a)*(b-c));
		}
		
		//double x1 = Math.pow(p*(c-a)*(b-a), 0.5)+a;
		//double x2 = b-Math.pow((1-p)*(b-a)*(b-c), 0.5);
		//if(x1>a && x1<c){
		//	return x1;
		//}
		//else if(x2>c && x2<b){
		//	return x2;
		//}
		//else{
		//	System.out.println("wrong triangle");
		//	return 0;
		//}
	}
	
	public boolean ifDependentTask(String task){
		for(Task t: this.inputTaskList){
			if(t.getName().equals(task)){
				return true;
			}else{
				if(t.ifDependentTask(task)==false){
					continue;
				}else{
					return true;
				}
			}
		}
		return false;
	}
	
	public void setNone(){
		this.state=TaskState.NONE;
		this.stateInt=0;
	}
	
	public boolean checkInputForExistingModel(){
		for(Task t: inputTaskList){
			if(t.remainingWorkAmount>0) return false;
		}
		return true;
	}
	
	public boolean allInputTaskFinished(){
		return inputTaskList.stream().allMatch(t -> t.isFinished());
	}
	
	public boolean getWn(){
		return wn;
	}
	
	public boolean getStartTimeAdded(){
		return startTimeAdded;
	}
	
	public void setWnTrue(){
		wn=true;
	}
	
	public void setWnFalse(){
		wn=false;
	}
	
	public void setStartTimeAddedTrue(){
		startTimeAdded = true;
	}
	
	public void setStartTimeAddedFalse(){
		startTimeAdded = false;
	}
	
	public void setRemainingWorkAmountZero(){
		remainingWorkAmount=0;
	}
	
	public String getReworkFrom(){
		return this.reworkFrom;
	}
	
	public void setReworkFrom(String a){
		this.reworkFrom=a;
	}
	
	public void addReworkFrom(String a){
		String b = this.reworkFrom;
		this.reworkFrom = b+";"+a;
	}
	
	public double calPercent(){
		return 0.0;
//		if(minimumWorkAmount.size()>1){
//			int i=2;
//			double perSum=0.0;
//			while(minimumWorkAmount.containsKey(i)){
//				perSum += (double)(minimumWorkAmount.get(i)/minimumWorkAmount.get(i-1));
//				i++;
//			}
//			return (double)(perSum/(i-1));
//		}else{
//			return 1.0;
//		}
	}

}
