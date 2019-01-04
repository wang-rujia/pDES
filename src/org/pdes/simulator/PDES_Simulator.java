package org.pdes.simulator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.pdes.simulator.model.ProjectInfo;
import org.pdes.simulator.model.Resource;
import org.pdes.simulator.model.Task;
import org.pdes.simulator.model.Workflow;

public class PDES_Simulator{

	protected ProjectInfo project;
	protected List<Workflow> workflowList;
	protected List<Resource> resourceList;
	protected int time = 0;
	private int count=0;
	
	public PDES_Simulator(ProjectInfo project) {
		this.project=project;
		this.workflowList=project.getWorkflowList_child();
		this.resourceList=project.getResourceList_child();
	}
	
	public void execute() {
		this.initialize();
		while(true){
			
			//0. Check finished or not.
			if(checkAllTasksAreFinished()) return;
			
			//1. Get ready task and free resources
			List<Task> workingTaskList = this.getWorkingTaskList();
			List<Task> readyTaskList = this.getReadyTaskList();
			List<Resource> freeResourceList = this.getFreeResourceList();
			List<Task> readyAndWorkingTaskList = Arrays.asList(readyTaskList,workingTaskList).stream().flatMap(list -> list.stream()).collect(Collectors.toList());
			
			//2. Sort ready task and free resources
			this.sortTasks(readyAndWorkingTaskList);
			this.sortResources(freeResourceList);
			
			//3. Allocate ready tasks to free resources
			this.allocateReadyTasksToFreeResourcesForSimuation(readyAndWorkingTaskList, freeResourceList);
			
			//4. Perform WORKING tasks and update the status of each task.
			this.performAndUpdateAllWorkflow(time);
			time++;
		}
	}
	
	public void initialize(){
		this.time = 0;
		workflowList.forEach(w -> w.initialize());
		resourceList.forEach(r -> r.initialize());
	}

	public int getTime() {
		return time;
	}

	public boolean checkAllTasksAreFinished(){
		return workflowList.stream().allMatch(w -> w.isFinished());
	}

	public List<Task> getReadyTaskList(){
		return workflowList.stream()
				.map(w -> w.getReadyTaskList())
				.collect(
						() -> new ArrayList<>(),
						(l, t) -> l.addAll(t),
						(l1, l2) -> l1.addAll(l2)
						);
	}

	public List<Task> getWorkingTaskList(){
		return workflowList.stream()
				.map(w -> w.getWorkingTaskList())
				.collect(
						() -> new ArrayList<>(),
						(l, t) -> l.addAll(t),
						(l1, l2) -> l1.addAll(l2)
						);
	}
	
	public List<Resource> getFreeResourceList(){
		return resourceList.stream()
				.filter(r -> r.isFree())
				.collect(Collectors.toList());
	}
	
	/**
	 * Sort Tasks as followings:<br>
	 * 1. Due date<br>
	 * 2. TSLACK (a task which Slack time(LS-ES) is lower has high priority)
	 * @param resourceList
	 */
	public void sortTasks(List<Task> taskList){
		taskList.sort((t1, t2) -> {
			double slack1 = t1.getLst() - t1.getEst();
			double slack2 = t2.getLst() - t2.getEst();
			return Double.compare(slack1, slack2);
		});
	}
	
	/**
	 * Sort Worker as followings:<br>
	 * 1. SSP (a resource which amount of skill point is lower has high priority)
	 * @param resourceList
	 */
	public void sortResources(List<Resource> resourceList){
		resourceList.sort((w1, w2) -> {
			double sp1 = w1.getTotalWorkAmountSkillPoint();
			double sp2 = w2.getTotalWorkAmountSkillPoint();
			return Double.compare(sp1, sp2);
		});
	}
	
	public void allocateReadyTasksToFreeResourcesForSimuation(List<Task> readyAndWorkingTaskList, List<Resource> freeResourceList){		
		this.sortTasks(readyAndWorkingTaskList);
		readyAndWorkingTaskList.stream().forEachOrdered(task -> {
				List<Resource> availableResource = freeResourceList.stream().filter(w -> w.hasSkill(task)).collect(Collectors.toList());
				for(Resource r : availableResource) {
					task.addAllocatedResource(r);
					r.setStateWorking();
					freeResourceList.remove(r);
				}
		});
	}
	
	/**
	 * Allocate ready tasks to free workers and facilities if necessary.<br>
	 * This method is only for single-task worker simulator.
	 * @param time 
	 * @param readyTaskList
	 * @param freeWorkerList
	 */
	public void allocateReadyTasksToFreeResourcesForSingleTaskWorkerSimuation(List<Task> readyTaskList, List<Resource> freeResourceList){		
		this.sortTasks(readyTaskList);
		readyTaskList.stream().forEachOrdered(task -> {
				Optional<Resource> availableResource = freeResourceList.stream().filter(w -> w.hasSkill(task)).findFirst();
				availableResource.ifPresent(resource ->{
					task.addAllocatedResource(resource);
					freeResourceList.remove(resource);
				});
		});
	}
	
	/**
	 * Allocate ready tasks to free workers and facilities if necessary.<br>
	 * This method is only for single-task workers simulator.
	 * @param time 
	 * @param readyAndWorkingTaskList
	 * @param freeWorkerList
	 * @param freeFacilityList
	 */
	public void allocateReadyTasksToFreeResourcesForSingleTaskWorkersSimulation(List<Task> readyAndWorkingTaskList, List<Resource> freeResourceList){
		this.sortTasks(readyAndWorkingTaskList);
		readyAndWorkingTaskList.stream().forEachOrdered(task -> {
			List<Resource> allocatingResources = freeResourceList.stream().filter(w -> w.hasSkill(task)).collect(Collectors.toList());
			for(Resource r : allocatingResources) {
				task.addAllocatedResource(r);
				freeResourceList.remove(r);
			}
		});
	}
	
	/**
	 * Allocate ready and working tasks to all workers and free facilities if necessary.<br>
	 * This method is only for multi-task worker simulation.
	 * @param readyTaskAndWorkingTaskList
	 * @param allWorkerList
	 * @param freeFacilityList
	 */
	public void allocateTaskToResourcesForMultiTaskWorkerSimulation(List<Task> readyTaskAndWorkingTaskList, List<Resource> allResourceList) {
		readyTaskAndWorkingTaskList.stream().forEachOrdered(task->{
			allResourceList.stream().filter(w -> w.hasSkill(task)).forEach(w -> {
				if(!task.isAlreadyAssigned(w)) {
					task.addAllocatedResource(w);
				}
			});
		});
	}
	
	/**
	 * Perform and update all workflow in this time.
	 * @param time 
	 * @param componentErrorRework 
	 */
	public void performAndUpdateAllWorkflow(int time){
		workflowList.forEach(w -> w.checkWorking(time));//READY -> WORKING
		workflowList.forEach(w -> w.perform(time, w.getTaskList()));//update information of WORKING task in each workflow
		workflowList.forEach(w -> w.checkFinished(time, w.getTaskList()));// WORKING -> WORKING_ADDITIONALLY or FINISHED
		workflowList.forEach(w -> w.checkReady(time));// NONE -> READY
		workflowList.forEach(w -> w.updatePERTData(time));//Update PERT information
	}

	public void saveResultFilesInDirectory(String outputDir, String no){
		String fileName = this.getResultFileName(no);//the list of file name
		this.saveResultFileByCsv(outputDir, fileName+".csv");//1. Gantt chart data by csv format.
		this.saveResultAsLog(outputDir, "log.txt",no); //2. Event log by txt. 
	}
	
	private String getResultFileName(String no){
		return no;
	}

	public void saveResultFileByCsv(String outputDirName, String resultFileName){
		File resultFile = new File(outputDirName, resultFileName);
		String separator = ",";
		try {
			// BOM
			FileOutputStream os = new FileOutputStream(resultFile);
			os.write(0xef);
			os.write(0xbb);
			os.write(0xbf);
			
			PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
			
			// header
			pw.println(String.join(separator, new String[]{"Duration", String.valueOf(project.getDuration()+1), "Total Work Amount", String.valueOf(project.getTotalActualWorkAmount())}));
			
			// workflow
			pw.println();
			pw.println("Gantt chart of each Task");
			pw.println(String.join(separator , new String[]{"Workflow", "Task", "Resource Name", "Start Time", "Finish Time", "Start Time", "Finish Time","Start Time", "Finish Time"}));
			this.workflowList.forEach(w -> {
				String workflowName = "workflow ID:"+w.getId();
				w.getTaskList().forEach(t ->{
					List<String> baseInfo = new ArrayList<String>();
					baseInfo.add(workflowName);
					baseInfo.add(t.getName());
					baseInfo.add(t.getAllocatedResourceList().stream().map(Resource::getName).collect(Collectors.joining("+")));
					IntStream.range(0, t.getFinishTimeList().size()).forEach(i -> {
//						baseInfo.add(String.valueOf(t.getReadyTimeList().get(i)));
						baseInfo.add(String.valueOf(t.getStartTimeList().get(i)+1));
						baseInfo.add(String.valueOf(t.getFinishTimeList().get(i)+1));
					});
					pw.println(String.join(separator ,baseInfo.stream().toArray(String[]::new)));
				});
			});

			// resource
			pw.println();
			pw.println("Gantt chart of each Resource");
			pw.println(String.join(separator , new String[]{"Resource Name", "Start Time", "Finish Time"}));
			this.resourceList.forEach(r ->{
				List<String> baseInfo = new ArrayList<String>();
				baseInfo.add(r.getName());
				IntStream.range(0, r.getAssignedTaskList().size()).forEach(i -> {
					baseInfo.add(String.valueOf(r.getStartTimeList().get(i)+1));
					baseInfo.add(String.valueOf(r.getFinishTimeList().get(i)+1));
				});
				pw.println(String.join(separator, baseInfo.stream().toArray(String[]::new)));
			});
			
			pw.close();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveResultAsLog(String outputDirName, String resultFileName, String no){
		try {
			FileWriter fw = new FileWriter(new File(outputDirName, resultFileName),true);
			BufferedWriter bw = new BufferedWriter(fw);
			this.workflowList.forEach(w -> {
				String projectName = w.getId();
				w.getTaskList().forEach(t ->{
					String taskName = t.getName();
					for(int i =0;i<t.getStartTimeList().size();i++){
						count++;
						String st = Integer.toString(t.getStartTimeList().get(i)+1);
						String et = Integer.toString(t.getFinishTimeList().get(i)+1);
						Double rc = t.getResourceCapacityLog().get(i);
						String log = no+"_"+Integer.toString(count)+projectName+ ","+projectName+","+taskName+","+st+","+et+",rn,"+Double.toString(rc)+",null,None";
						try {
							bw.write(log);
							bw.newLine();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
			});
			bw.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
