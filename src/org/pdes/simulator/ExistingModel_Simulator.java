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
import java.util.stream.IntStream;
import org.pdes.simulator.model.ProjectInfo;
import org.pdes.simulator.model.Task;
import org.pdes.simulator.model.Workflow;

public class ExistingModel_Simulator {

	protected ProjectInfo project;
	protected List<Workflow> workflowList;
	protected List<Task> allTaskList;
	protected int time = 0;
	private int count=0;
	private int simNo;
	
	public ExistingModel_Simulator(ProjectInfo project,int no) {
		this.project=project;
		this.workflowList=project.getWorkflowList_child();
		this.allTaskList = workflowList.stream()
						   	.map(w -> w.getTaskList())
							.collect(() -> new ArrayList<>(),
									(l, t) -> l.addAll(t),
									(l1, l2) -> l1.addAll(l2)
									);
		this.simNo=no;
	}
	
	public void execute() {
		this.initialize(simNo);
		sortTasks();
		while(true){
			allTaskList.forEach(t -> t.checkPermissionForExistingModel(time));
			allTaskList.forEach(t -> t.performForExistingModel(time));
			allTaskList.forEach(t -> t.checkFinishedForExistingModel(time, allTaskList, simNo));
			if(checkAllTasksAreFinished()) return;
			time++;
		}
	}
	
	public void sortTasks(){
		allTaskList.sort((t1, t2) -> {
			int c1 = (int)t1.getName().charAt(0);
			int c2 = (int)t2.getName().charAt(0);
			return Integer.compare(c1, c2);
		});
	}
	
	public void initialize(int simNo){
		this.time = 1;
		allTaskList.forEach(t -> t.initializeForExistingModel(simNo));
		this.sortTasks();
	}

	public boolean checkAllTasksAreFinished(){
		return allTaskList.stream().allMatch(w -> w.isFinished());
	}

	public void saveResultFilesInDirectory(String outputDir, String no){
		String fileName = this.getResultFileName(no);//the list of file name
		this.saveResultFileByCsv(outputDir, fileName+".csv");//1. Gantt chart data by csv format.
		this.saveResultAsLog(outputDir, "log.txt"); //2. Event log by txt. 
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
						baseInfo.add(String.valueOf("st: "+t.getStartTimeList().get(i)));
						baseInfo.add(String.valueOf("et: "+t.getFinishTimeList().get(i)));
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
	
	public void saveResultAsLog(String outputDirName, String resultFileName){
		try {
			FileWriter fw = new FileWriter(new File(outputDirName, resultFileName),true);
			BufferedWriter bw = new BufferedWriter(fw);
			this.workflowList.forEach(w -> {
				String projectName = w.getId();
				w.getTaskList().forEach(t ->{
					String taskName = t.getName();
					for(int i =0;i<t.getFinishTimeList().size();i++){
						count++;
						String st = Integer.toString(t.getStartTimeList().get(i)/100);
						String et = Integer.toString(t.getFinishTimeList().get(i)/100);
						Double rc = t.getResourceCapacityLog().get(i);
						String log = Integer.toString(count)+projectName+ ","+projectName+","+taskName+","+st+","+et+",rn,"+Double.toString(rc)+",null,None";
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
