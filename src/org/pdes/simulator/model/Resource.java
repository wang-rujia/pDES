package org.pdes.simulator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.pdes.rcp.model.base.ResourceElement;

public class Resource {
	protected enum ResourceState {
		FREE,
		WORKING,
	}
	
	// Constraint variables on simulation
	protected final String id; // ID
	protected final String nodeId; // ResourceElement ID
	protected final String name;
	protected final double costPerTime;
	protected Map<String, Double> workAmountSkillMap; // skill map of work amount <taskname, skill point>
	protected Map<String, Double> qualitySkillMap; // skill map of quality <taskname, skill point>
	
	// Changeable variable on simulation
	protected ResourceState state;
	protected double totalCost = 0;
	protected final List<Integer> startTimeList = new ArrayList<Integer>(); // list of start time of one task
	protected final List<Integer> finishTimeList = new ArrayList<Integer>(); // list of finish time of one task
	protected final List<Task> assignedTaskList = new ArrayList<Task>(); // list of worked task
	
	/**
	 * This is the constructor.
	 * @param resourceElement
	 * @param team
	 */
	public Resource(ResourceElement resourceElement) {
		this.id = UUID.randomUUID().toString();
		this.nodeId = resourceElement.getId();
		this.name = resourceElement.getName();
		this.costPerTime = resourceElement.getCost();
		this.workAmountSkillMap = resourceElement.getWorkAmountSkillMap();
		this.qualitySkillMap = resourceElement.getQualitySkillMap();
	}
	
	/**
	 * Initialize
	 */
	public void initialize() {
		state = ResourceState.FREE;
		totalCost = 0;
		startTimeList.clear();
		finishTimeList.clear();
		assignedTaskList.clear();
	}
	
	/**
	 * Check whether this resource is free or not.
	 * @return
	 */
	public boolean isFree() {
		return state == ResourceState.FREE;
	}
	
	/**
	 * Check whether this resource is working or not.
	 * @return
	 */
	public boolean isWorking() {
		return state == ResourceState.WORKING;
	}
	
	/**
	 * Set the state of this resource to "FREE".
	 */
	public void setStateFree() {
		state = ResourceState.FREE;
	}
	
	/**
	 * Set the state of this resource to "WORKING".
	 */
	public void setStateWorking() {
		state = ResourceState.WORKING;
	}
	
	/**
	 * Working in simulation. In this simulation, total cost of this resource have to be updated.
	 */
	public void addLaborCost() {
		totalCost += costPerTime;
	}
	
	/**
	 * Check whether this resource has "task" skill or not.
	 * @param task
	 * @return
	 */
	public boolean hasSkill(Task task) {
		return workAmountSkillMap.containsKey(task.getName()) && workAmountSkillMap.get(task.getName()) > 0.0;
	}
	
	/**
	 * Get the work amount skill point of "task".
	 * if this resource will do multi-tasks at the same time, allocation is divided to working tasks equally. 
	 * @param task
	 * @return
	 */
	public double getWorkAmountSkillPoint(Task task){
		for(Task t : this.assignedTaskList){
			if(t.equals(task)) {
				return this.workAmountSkillMap.get(t.getName());
			}
		}
		return 0.0;
	}
	
	/**
	 * Get the quality skill point of "task".
	 * @param task
	 * @return
	 */
	public double getQualitySkillPoint(Task task){
		if (!hasSkill(task)) return 0.0;
		return qualitySkillMap.get(task.getName());
	}
	
	/**
	 * Get total work amount skill point.
	 * @return
	 */
	public double getTotalWorkAmountSkillPoint() {
		return workAmountSkillMap.values().stream().mapToDouble(v -> v).sum();
	}
	
	/**
	 * Add assigned task.
	 * @param task
	 */
	public void addAssignedTask(Task task) {
		assignedTaskList.add(task);
	}
	
	/**
	 * Add start time.
	 * @param time
	 */
	public void addStartTime(int time) {
		startTimeList.add(time);
	}
	
	/**
	 * Add finish time.
	 * @param time
	 */
	public void addFinishTime(int time) {
		finishTimeList.add(time);
	}
	
	/**
	 * Get first start time in the list of start time.
	 * @return
	 */
	public int getFirstStartTime() {
		if (startTimeList.size() == 0) return -1;
		return startTimeList.get(0);
	}
	
	/**
	 * Get last finish time in the list of finish time.
	 * @return
	 */
	public int getLastFinishTime() {
		if (finishTimeList.size() == 0) return -1;
		return finishTimeList.get(finishTimeList.size() - 1);
	}

	/**
	 * Get the id.
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Get the name.
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the cost per time.
	 * @return the costPerTime
	 */
	public double getCostPerTime() {
		return costPerTime;
	}

	/**
	 * Get the skill map of work amount.
	 * @return the workAmountSkillMap
	 */
	public Map<String, Double> getWorkAmountSkillMap() {
		return workAmountSkillMap;
	}

	/**
	 * Get the skill map of quality.
	 * @return the qualitySkillMap
	 */
	public Map<String, Double> getQualitySkillMap() {
		return qualitySkillMap;
	}

	/**
	 * Get total cost.
	 * @return the totalCost
	 */
	public double getTotalCost() {
		return totalCost;
	}

	/**
	 * Get the list of start time list.
	 * @return the startTimeList
	 */
	public List<Integer> getStartTimeList() {
		return startTimeList;
	}

	/**
	 * Get the list of finish time list.
	 * @return the finishTimeList
	 */
	public List<Integer> getFinishTimeList() {
		return finishTimeList;
	}

	/**
	 * Get the list of worked task.
	 * @return the assignedTaskList
	 */
	public List<Task> getAssignedTaskList() {
		return assignedTaskList;
	}
	
	/**
	 * Transfer to text data.
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(name);
		sb.append("] WA(");
		for (Map.Entry<String, Double> entry : workAmountSkillMap.entrySet()) {
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(entry.getValue());
			sb.append(" ");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(") Q(");
		for (Map.Entry<String, Double> entry : qualitySkillMap.entrySet()) {
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(entry.getValue());
			sb.append(" ");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		return sb.toString();
	}
}
