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
package org.pdes.rcp.actions;

import java.io.File;

import org.eclipse.gef.ui.parts.GraphicalEditorWithPalette;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.pdes.rcp.model.ProjectDiagram;
import org.pdes.rcp.view.editor.ProjectEditor;

/**
 * This is the Action class for saving project.<br>
 * @author Taiga Mitsuyuki <mitsuyuki@sys.t.u-tokyo.ac.jp>
 */
public class SaveFileAction extends Action {
	
	public SaveFileAction() {
		//setImageDescriptor(ImageDescriptor.createFromFile(OpenFileAction.class, "save.png"));
		setToolTipText("Save");
		this.setText("Save");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		IWorkbench ib= PlatformUI.getWorkbench();
		GraphicalEditorWithPalette editor = (GraphicalEditorWithPalette)(ib.getActiveWorkbenchWindow().getActivePage().getActiveEditor());
		if(editor==null) return;//before editing
		if(editor instanceof ProjectEditor){
			ProjectEditor pe = (ProjectEditor)editor;
			String filePath = pe.getFilePath();
			if(filePath.equals("New Project")){
				FileDialog dialog = new FileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),SWT.SAVE);
				dialog.setFilterExtensions(new String[]{"*.pdm","*.xml"});
				dialog.setText("Save file");
				filePath = dialog.open();
			}
			if(filePath != null){
				if(((ProjectDiagram)pe.getDiagram()).saveProjectFile(filePath)){
					pe.setTabTitle(new File(filePath).getName());
					pe.setFilePath(filePath);
					pe.setSaveLocation();
					MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Save", "Project is saved to ["+filePath+"]");
				}else{
					MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error", "Project cannot be saved.");
				}
			}
		}
	}
	
	
}
