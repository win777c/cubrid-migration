package com.cubrid.cubridmigration.ui.wizard.editor;

import java.io.File;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;

import com.cubrid.common.ui.swt.table.ObjectArrayRowTableLabelProvider;
import com.cubrid.common.ui.swt.table.TableViewerBuilder;
import com.cubrid.cubridmigration.core.common.PathUtils;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.ui.history.CTCReportEditorPart;
import com.cubrid.cubridmigration.ui.message.Messages;
import com.cubrid.cubridmigration.ui.wizard.editor.controller.CTCProgressUIController;

public class CTCProgressEditorPart extends MigrationProgressEditorPart {

	public static final String ID = CTCProgressEditorPart.class.getName();

	@Override
	public void createPartControl(org.eclipse.swt.widgets.Composite parent) {
		super.createPartControl(parent);

		txtProgress.setVisible(false);
		
		parent.redraw();
		parent.layout();
	}
	
	@Override
	protected void createProgressTableViewer(Composite pnlBackTop) {
	    TableViewerBuilder tvBuilder = new TableViewerBuilder();
		tvBuilder.setColumnNames(new String[] { 
				Messages.colTable, 
				Messages.colExportedCount,
		        Messages.colImportedCount, 
		        Messages.colFailed 
		});
		tvBuilder.setColumnWidths(new int[] { 200, 120, 120, 100 });
		tvBuilder.setColumnStyles(new int[] { SWT.LEFT, SWT.RIGHT, SWT.RIGHT, SWT.CENTER });
		tvBuilder.setContentProvider(new ArrayContentProvider());
		tvBuilder.setLabelProvider(new ObjectArrayRowTableLabelProvider());
		
		tvProgress = tvBuilder.buildTableViewer(pnlBackTop, SWT.BORDER | SWT.FULL_SELECTION);
	}
	
	@Override
	protected void createPart1(SashForm sf) {
		super.createPart1(sf);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
	    super.init(site, input);
	    setPartName("CTC Progress");
	    setTitleToolTip("CUBRID Transaction Capture");
	    initializeActiveMQ();
	}

	/**
	 * initializeActiveMQ
	 */
	private void initializeActiveMQ() {
	    // 시작 전 activemq-data 폴더 초기화 부분
		File activeMessageQueueStorageDir = new File(getActiveMessageStoragePath());
		deleteFile(activeMessageQueueStorageDir);
    }

	/**
	 * getActiveMessageStoragePath
	 * @return
	 */
	private String getActiveMessageStoragePath() {
	    String activemqMessageStroragePath = PathUtils.getInstallPath() + File.separatorChar + "activemq-data";
	    activemqMessageStroragePath.replace("\\", "/");
	    
	    return activemqMessageStroragePath;
    }
	
	/**
	 * deleteFile
	 * @param file
	 */
	private void deleteFile(File file) {
		if (file.isDirectory()) {
			for (File sub : file.listFiles()) {
				deleteFile(sub);
			}
		}
		file.delete();
	}
	
	@Override
	protected void initUIController(MigrationConfiguration cf) {
	    controller = new CTCProgressUIController();
	    controller.setConfig(cf);
	    controller.setReportEditorPartId(CTCReportEditorPart.ID);
	}
}
