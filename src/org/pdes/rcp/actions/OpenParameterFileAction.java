package org.pdes.rcp.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.pdes.rcp.view.editor.ProjectEditorInput;

public class OpenParameterFileAction extends Action{
	
	public OpenParameterFileAction(){
		setToolTipText("Open extracted parameter file");
		this.setText("Open extracted parameter file");
	}
	
	public void run() {
		String filePath = openFileDialog();
		if(filePath!=null){
			String ext = filePath.substring(filePath.length()-3, filePath.length());
			if(ext.equals("xml")){
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IEditorInput input= new ProjectEditorInput(filePath);
				try{
					page.openEditor(input, "pDES.ParameterEditor");
				}catch(PartInitException e){
					e.printStackTrace();
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
