package org.pdes.rcp.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsoleStream;
import org.pdes.rcp.core.Activator;
import org.pdes.rcp.model.ProjectDiagram;
import org.pdes.rcp.view.editor.ProjectEditor;

public class OpenParameterFileAction extends Action{
	
	protected final MessageConsoleStream msgStream = Activator.getDefault().getMsgStream();
	
	public OpenParameterFileAction(){
		setToolTipText("Open extracted parameter file");
		this.setText("Open extracted parameter file");
	}
	
	public void run() {
		String filePath = openFileDialog();
		if(filePath!=null){			
			//Check whether Project is opened or not.
			IWorkbench ib = PlatformUI.getWorkbench();
			ProjectEditor pe = (ProjectEditor) ib.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			if(pe == null){
				MessageDialog.openError(ib.getActiveWorkbenchWindow().getShell(), "Error", "Project is not opened.");
				return;
			}
			//if opened file is xml, then read parameter file
			String ext = filePath.substring(filePath.length()-3, filePath.length());
			if(ext.equals("xml")){
				if(((ProjectDiagram)pe.getDiagram()).readParameterFile(filePath)){
					msgStream.println("parameter file has been read");
				}else{
					msgStream.println("error when read parameter file");
				}
			}
		}
	}
	
	private String openFileDialog() {
		FileDialog dialog = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),SWT.OPEN);
		dialog.setText("Select xml file.");
		dialog.setFilterExtensions(new String[]{"*.xml"});
		return dialog.open();
	}
	
	
}
