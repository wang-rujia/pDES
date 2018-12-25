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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Workflow{

	private final String id;
	private List<Task> taskList = new ArrayList<Task>();
	private int criticalPathLength;
	
	public Workflow(List<Task> t) {
		this.id = UUID.randomUUID().toString();
		this.taskList=t;
	}
	
	public void initialize() {
		taskList.forEach(t -> t.initialize());
		criticalPathLength = 0;
		updatePERTData(0);
		checkReady(0);
	}
	
	public void updatePERTData(int time) {
		setEstEftData(time);
		setLstLftData();
	}
	
	private void setEstEftData(int time){
		List<Task> inputTaskList = new ArrayList<Task>();
		
		// 1. Set the earliest finish time of head tasks.
		for(Task task : taskList){
			task.setEst(time); // for initializing
			if(task.getInputTaskList().size()==0){
				task.setEft(time + task.getRemainingWorkAmount());
				inputTaskList.add(task);
			}
		}
		
		// 2. Calculate PERT information of all tasks
		while (true){
			if(inputTaskList.size() == 0) break;
			List<Task> nextTaskList = new ArrayList<Task>();
			for(Task inputTask : inputTaskList){
				for(Task task : taskList){
					List<Task> _inputTaskList = task.getInputTaskList();
					for(Task _inputTask : _inputTaskList){
						if(inputTask.equals(_inputTask)){
							Double preEst = task.getEst();
							Double inputEst = inputTask.getEst();
							Double est = Double.valueOf(inputEst) + inputTask.getRemainingWorkAmount();
							Double eft = Double.valueOf(est) + task.getRemainingWorkAmount();
							if(est >= preEst){
								task.setEst(est);
								task.setEft(eft);
								for (int l = 0; l < nextTaskList.size(); l++) {
									if (nextTaskList.get(l).getId().equals(task.getId())) {
										nextTaskList.remove(l);
									}
								}
								nextTaskList.add(task);
							}
						}
					}
				}
			}
			inputTaskList = nextTaskList;
		}
	}
	
	/**
	 * Calculate latest start / finish time of all tasks by using only remaining work amount.
	 */
	private void setLstLftData(){
		List<Task> lateTaskList = new ArrayList<Task>();
		
		//1. Extract the list of tail tasks.
		List<String> lastTaskIdList = taskList.stream().map(task -> task.getId()).collect(Collectors.toList());
		for(Task task : taskList){
			for(Task inputTask : task.getInputTaskList()){
				String inputTaskId = inputTask.getId();
				for(int k=0; k< lastTaskIdList.size(); k++){
					String id = lastTaskIdList.get(k);
					if(id.equals(inputTaskId)){
						lastTaskIdList.remove(k);
						break;
					}
				}
			}
		}
		
		//2. Update the information of critical path of this workflow.
		for(String lastTaskId : lastTaskIdList){
			for(Task task : taskList){
				if(lastTaskId.equals(task.getId())) {
					lateTaskList.add(task);
					if(criticalPathLength < task.getEft()) criticalPathLength = (int)task.getEft();
				}
			}
		}
		
		
		//3. Calculate the PERT information of all tasks.
		for(Task task : taskList){
			for(Task lateTask : lateTaskList){
				if(task.getId().equals(lateTask.getId())){
					task.setLft(criticalPathLength);
					task.setLst(criticalPathLength - task.getRemainingWorkAmount());
					registerLsLf(task);
				}
			}
		}
		
	}
	
	/**
	 * Calculate latest start / finish time of all tasks by using only remaining work amount.
	 * @param task
	 */
	private void registerLsLf(Task task) {
		double length = task.getLst();
		
		List<Task> inputTaskList = new ArrayList<Task>();
		for(Task inputTask : taskList){
			for(Task it : task.getInputTaskList()){
				String inputId = it.getId();
				if(inputTask.getId().equals(inputId)) inputTaskList.add(inputTask);
			}
		}
		
		for(Task inputTask : inputTaskList){
			if (inputTask.getLft() <= length) { 
				inputTask.setLft(length);
				inputTask.setLst(length - (inputTask.getRemainingWorkAmount()));	
				registerLsLf(inputTask);
			}
		}
	}
	
	public boolean isFinished() {
		return taskList.stream().allMatch(t -> t.isFinished());
	}

	public void checkReady(int time) {
		taskList.forEach(t -> t.checkReady(time));
	}
	
	public List<Task> getReadyTaskList() {
		return taskList.stream().filter(t -> t.isReady()).collect(Collectors.toList());
	}

	public List<Task> getWorkingTaskList() {
		return taskList.stream().filter(t -> t.isWorking()).collect(Collectors.toList());
	}
	
	public void checkWorking(int time) {
		taskList.forEach(t -> t.checkWorking(time));
	}

	public void checkFinished(int time,List<Task> allTask) {
		taskList.forEach(t -> t.checkFinished(time,allTask));
	}
	
	public void perform(int time, List<Task> allTask) {
		taskList.forEach(t -> t.perform(time, allTask));
	}
	
	public List<Task> getTaskList(){
		return this.taskList;
	}
	
	public String getId(){
		return this.id;
	}
	
	public int getDuration() {
		int a=this.taskList.stream()
				.mapToInt(t -> t.getFinishTimeList().stream()
						.max(Comparator.naturalOrder())
						.orElse(0))
				.max()
				.orElse(0);
		return a+1;
	}
	
	public double getTotalWorkAmount() {
		return 0;
	}

}
