package org.pdes.rcp.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.pdes.rcp.actions.base.AbstractSimulationAction;
import org.pdes.rcp.model.ProjectDiagram;
import org.pdes.simulator.PDES_Simulator;
import org.pdes.simulator.model.ProjectInfo;

public class MultiRunPDES_Simulator extends AbstractSimulationAction {
	
	private final String text = "TSLACK+SSP+Uncertainty model";
	
	public MultiRunPDES_Simulator(){
		this.setToolTipText(text);
		this.setText(text);
	}
	
	protected List<Future<String>> doSimulation(ProjectDiagram diagram, int workflowCount) {
		//Set the number of simulation
		int numOfSimulation = this.setNumOfSimulation();
		if(numOfSimulation <= 0) {
			this.aggregateMode = false;
			return null;
		}else{
			this.aggregateMode = true;
		}
		
		long start = System.currentTimeMillis();
		ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Future<String>> resultList = new ArrayList<Future<String>>();
		IntStream.range(0,numOfSimulation).forEach(i ->{
			BasicSimulationTask basicSimulationTask = new BasicSimulationTask(i, diagram, workflowCount, outputDir);
			resultList.add(service.submit(basicSimulationTask));
		});
		service.shutdown();
		long end = System.currentTimeMillis();
		msgStream.println("Processing time: " + ((end - start)) + " [millisec]");
		return resultList;
	}
	
	private class BasicSimulationTask implements Callable<String>{
		
		private final int no;
		private final ProjectDiagram diagram;
		private final int numOfWorkflow;
		private final String outputDirectoryPath;
		
		/**
		 * This is the constructor.
		 * @param no
		 * @param diagram
		 * @param numOfWorkflow
		 */
		public BasicSimulationTask(int no, ProjectDiagram diagram, int numOfWorkflow, String outputDirectoryPath) {
			this.no = no;
			this.diagram = diagram;
			this.numOfWorkflow = numOfWorkflow;
			this.outputDirectoryPath = outputDirectoryPath;
		}

		/* (non-Javadoc)
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public String call() throws Exception {
			ProjectInfo project = new ProjectInfo(diagram, numOfWorkflow);
			PDES_Simulator sim = new PDES_Simulator(project);
			sim.execute(no);
			sim.saveResultFilesInDirectory(outputDirectoryPath, String.valueOf(no));
			return String.format("%d,%f,%d,%f", no, project.getTotalCost(), project.getDuration(),project.getTotalActualWorkAmount());
		}
	}
	
	
}
