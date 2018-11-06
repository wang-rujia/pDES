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
import java.util.UUID;

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
	
	public Task(TaskNode taskNode) {
		this.id = UUID.randomUUID().toString();
		this.nodeId = taskNode.getId();
		this.name = taskNode.getName();
		this.minimumWorkAmount=taskNode.getMinimumWorkAmountMap();
		this.rework=taskNode.getRework();
		this.delay=taskNode.getDelay();
	}
	
	public void initialize() {
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
			for(Resource r : allocatedResourceList) {
				r.setStateWorking();
				r.addStartTime(time);
				r.addAssignedTask(this);
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
	public void checkFinished(int time) {
		if (remainingWorkAmount <= 0) {
			if (isWorking()) {
				addFinishTime(time);
				remainingWorkAmount = 0;
				
				//Finish normally.
				state = TaskState.FINISHED;
				stateInt = 4;
				for(Resource r : allocatedResourceList) {
					if(r.getAssignedTaskList().stream().filter(t -> t.getStateInt() < 4).count() == 0) {
						r.setStateFree();
					}
					r.addFinishTime(time);
				}
			} else if (isWorkingAdditionally()) {
				addFinishTime(time);
				remainingWorkAmount = 0;
				state = TaskState.FINISHED;
				stateInt = 4;
				for(Resource r : allocatedResourceList) {
					r.setStateFree();
					r.addFinishTime(time);
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
	public void perform(int time) {
		if (isWorking() || isWorkingAdditionally()) {
			double workAmount = 0;
			for(Resource r : allocatedResourceList) {
				workAmount += r.getWorkAmountSkillPoint(this);
			}
			actualWorkAmount +=workAmount;
			remainingWorkAmount -= workAmount;
			progress = actualWorkAmount/this.getOccurrenceTime();
		}
	}
	
	public int getOccurrenceTime(){
		return this.o;
	}
	
	/**
	 * Check whether the state of this task is NONE.
	 * @return
	 */
	public boolean isNone() {
		return state == TaskState.NONE;
	}
	
	/**
	 * Check whether the state of this task is READY.
	 * @return
	 */
	public boolean isReady() {
		return state == TaskState.READY;
	}
	
	/**
	 * Check whether the state of this task is WORKING.
	 * @return
	 */
	public boolean isWorking() {
		return state == TaskState.WORKING;
	}
	
	/**
	 * Check whether the state of this task is ADDITIONAL WORKING.
	 * @return
	 */
	public boolean isWorkingAdditionally() {
		return state == TaskState.WORKING_ADDITIONALLY;
	}
	
	/**
	 * Check whether the state of this task is FINISHED.
	 * @return
	 */
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


}
