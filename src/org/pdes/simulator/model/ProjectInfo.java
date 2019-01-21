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
import java.util.List;
import java.util.stream.Collectors;

import org.pdes.rcp.model.ProjectDiagram;
import org.pdes.rcp.model.TeamNode;
import org.pdes.simulator.model.base.BaseProjectInfo;

public class ProjectInfo extends BaseProjectInfo {

	private ProjectDiagram diagram;
	private List<Workflow> workflowList;
	private List<Resource> resourceList;
	
	public ProjectInfo(ProjectDiagram diagram, int workflowCount) {
		super(diagram,workflowCount);
		this.diagram = diagram;
		this.resourceList = this.getResourceListFromProjectDiagram();
		this.workflowList = new ArrayList<Workflow>();
		this.workflowList.add(new Workflow(this.getTaskListConsideringOnlyTaskDependency_child()));
	}
	
	private List<Resource> getResourceListFromProjectDiagram(){
		List<Resource> resourceList = new ArrayList<Resource>();
		for(TeamNode t : diagram.getTeamNodeList()){
			for(int i=0;i<t.getWorkerList().size();i++){
				resourceList.add(new Resource(t.getWorkerList().get(i)));
			}
		}
		return resourceList;
	}
	
	private List<Task> getTaskListConsideringOnlyTaskDependency_child(){
		List<Task> taskList = this.diagram.getTaskNodeList().stream()
				.map(node -> new Task(node))
				.collect(Collectors.toList());
		this.diagram.getTaskLinkList().forEach(link -> {
			Task destinationTask = taskList.stream()
					.filter(task -> task.getNodeId().equals(link.getDestinationNode().getId()))
					.findFirst()
					.get();
			Task originTask = taskList.stream()
					.filter(task -> task.getNodeId().equals(link.getOriginNode().getId()))
					.findFirst()
					.get();
			destinationTask.addInputTask(originTask);
			originTask.addOutputTask(destinationTask);
		});
		return taskList;
	}
	
	public List<Workflow> getWorkflowList_child() {
		return workflowList;
	}
	
	public List<Resource> getResourceList_child() {
		return resourceList;
	}
	
	public int getDuration(){
		return workflowList.stream()
				.mapToInt(w -> w.getDuration())
				.max()
				.orElse(0);
	}
	
	public double getCost(){
		double sum=0;
		for(Resource r: resourceList){
			for(int i=0; i < r.getFinishTimeList().size();i++){
				sum += (r.getFinishTimeList().get(i)-r.getStartTimeList().get(i)+1)*r.getCostPerTime();
			}
		}
		return sum;
	}

}
