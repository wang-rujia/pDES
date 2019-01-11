package org.pdes.simulator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.pdes.simulator.model.ProjectInfo;
import org.pdes.simulator.model.Task;
import org.pdes.simulator.model.Workflow;

public class ExistingModel_Simulator {

	protected ProjectInfo project;
	protected List<Workflow> workflowList;
	protected List<Task> allTaskList;
	protected int time = 0;
	protected int timestep=0;
	private int count=0;
	private int simNo;
	private Random rand;
	
	public ExistingModel_Simulator(ProjectInfo project,int no, Random rand) {
		this.project=project;
		this.workflowList=project.getWorkflowList_child();
		this.allTaskList = workflowList.stream()
						   	.map(w -> w.getTaskList())
							.collect(() -> new ArrayList<>(),
									(l, t) -> l.addAll(t),
									(l1, l2) -> l1.addAll(l2)
									);
		this.rand=rand;
		this.simNo=no;
	}
	
	public void execute() {
		this.initialize(simNo,rand);
		sortTasks();
		List<Task> workingTaskList = new ArrayList<Task>();
		List<Task> finishedTaskList = new ArrayList<Task>();
		while(true){
			//A.set active tasks and add start time
			for(Task t:allTaskList) t.setWnFalse();
			workingTaskList.clear();
			for(int i=0;i<allTaskList.size();i++){
				if(allTaskList.get(i).getRemainingWorkAmount()>0){
					workingTaskList.add(allTaskList.get(i));
					for(int j=i+1;j<allTaskList.size();j++){
						if(allTaskList.get(j).getRemainingWorkAmount()>0 && allTaskList.get(j).checkInputForExistingModel()){
							workingTaskList.add(allTaskList.get(j));
							break;
						}else{
							if(allTaskList.get(j).getRemainingWorkAmount() > 0){
								break;
							}
						}
					}
					break;
				}
			}
			for(Task t:workingTaskList){
				t.setWnTrue();
				if(!t.getStartTimeAdded()){
					t.setStartTimeAddedTrue();
					t.addStartTime(time);
				}
			}
			
			//B.calculate time step
			timestep=100000;
			for(Task t:workingTaskList) if(t.getRemainingWorkAmount()+1 < timestep) timestep = (int)t.getRemainingWorkAmount()+1;
			
			//C. work on active tasks; add finish time; make finished task list
			time += timestep;
			workingTaskList.forEach(t -> t.performForExistingModel(timestep));
			finishedTaskList.clear();
			for(Task t:workingTaskList){
				if(t.getRemainingWorkAmount() <=0 ){
					t.setRemainingWorkAmountZero();
					finishedTaskList.add(t);
				}
			}
			
			//D. check for rework for completed tasks
			setRework(finishedTaskList);
			for(Task t:finishedTaskList){
				if(t.getReworkFrom().equals("none")){
					t.addReworkFromLog("None");
				}else{
					t.addReworkFromLog(t.getReworkFrom());
				}
				if(t.getStartTimeAdded()){
					t.addResourceCapacityLog(1);
					t.setStartTimeAddedFalse();
					t.setReworkFrom("none");
					t.addFinishTime(time);
				}
			}
			for(Task t: workingTaskList){
				if(t.getRemainingWorkAmount()>0 && !t.checkInputForExistingModel()){
					if(t.getStartTimeAdded()) {
						t.addReworkFromLog("Stop");
						t.addFinishTime(time);
						t.addResourceCapacityLog(1);
						t.setStartTimeAddedFalse();
					}
				}
			}

			if(checkFinishedForExistingModel()) return;
		}
	}
	
	public boolean checkFinishedForExistingModel(){
		for(Task t: allTaskList){
			if(t.getRemainingWorkAmount()>0) return false;
		}
		return true;
	}
	
	public void setRework(List<Task> finishedTaskList){
		finishedTaskList.forEach(t -> t.checkReworkForExistingModel(allTaskList));
	}

	
	public void sortTasks(){
		allTaskList.sort((t1, t2) -> {
			int c1 = (int)t1.getName().charAt(0);
			int c2 = (int)t2.getName().charAt(0);
			return Integer.compare(c1, c2);
		});
	}
	
	public void initialize(int simNo,Random rand){
		time = 0;
		allTaskList.forEach(t -> t.initializeForExistingModel(simNo, rand));
	}

	public boolean checkAllTasksAreFinished(){
		return allTaskList.stream().allMatch(w -> w.isFinished());
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
			pw.println(String.join(separator , new String[]{"Workflow", "Task", "Start Time", "Finish Time", "Start Time", "Finish Time","Start Time", "Finish Time"}));
			this.workflowList.forEach(w -> {
				String workflowName = "workflow ID:"+w.getId();
				w.getTaskList().forEach(t ->{
					List<String> baseInfo = new ArrayList<String>();
					baseInfo.add(workflowName);
					baseInfo.add(t.getName());
					IntStream.range(0, t.getFinishTimeList().size()).forEach(i -> {
						baseInfo.add(String.valueOf("st: "+Math.round(t.getStartTimeList().get(i)/100.0)));
						baseInfo.add(String.valueOf("et: "+Math.round(t.getFinishTimeList().get(i)/100.0)));
					});
					pw.println(String.join(separator ,baseInfo.stream().toArray(String[]::new)));
				});
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
					for(int i =0;i<t.getFinishTimeList().size();i++){
						count++;
						String st = Long.toString(Math.round(t.getStartTimeList().get(i)/10.0));
						String et = Long.toString(Math.round(t.getFinishTimeList().get(i)/10.0));
						Double rc = t.getResourceCapacityLog().get(i);
						String rw = t.getReworkFromLog().get(i);
						String log = no+"_"+Integer.toString(count)+projectName+ ","+projectName+","+taskName+","+st+","+et+",rn,"+Double.toString(rc)+",null,"+rw;
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
