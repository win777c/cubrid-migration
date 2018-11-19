package com.cubrid.cubridmigration.ui.history;

import java.text.NumberFormat;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.cubrid.common.ui.swt.table.TableViewerBuilder;
import com.cubrid.cubridmigration.core.common.TimeZoneUtils;
import com.cubrid.cubridmigration.core.engine.report.MigrationReport;
import com.cubrid.cubridmigration.core.engine.report.RecordMigrationResult;
import com.cubrid.cubridmigration.cubrid.CUBRIDTimeUtil;
import com.cubrid.cubridmigration.ui.SWTResourceConstents;
import com.cubrid.cubridmigration.ui.history.tableviewer.RecordMigrationResultTableLabelProvider;
import com.cubrid.cubridmigration.ui.message.Messages;

public class CTCReportEditorPart extends EditorPart {
	public static final String ID = CTCReportEditorPart.class.getName();

	public static final String[] TABLE_HEADER_DATA = new String[] {Messages.colTableName, Messages.colExpCount, Messages.colImpCount, "Failed Count"};

	private TabFolder tfOverview;
	private TableViewer tvRecords;
	
	@Override
    public void doSave(IProgressMonitor monitor) {
    }

	@Override
    public void doSaveAs() {
    }

	@Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName("CTC Report");
		setTitleToolTip("CTC Report");
		setTitleImage(SWTResourceConstents.IMAGE_EXPORT_REPORT);
    }

	@Override
    public boolean isDirty() {
	    return false;
    }

	@Override
    public boolean isSaveAsAllowed() {
	    return false;
    }
	
	@Override
    public void createPartControl(Composite parent) {
		Composite backGroundComposite = new Composite(parent, SWT.NONE);
		backGroundComposite.setLayout(new GridLayout());
		backGroundComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		tfOverview = new TabFolder(backGroundComposite, SWT.NONE);
		tfOverview.setLayout(new GridLayout());
		tfOverview.setLayoutData(new GridData(GridData.FILL_BOTH));

		TabItem tiOverview = new TabItem(tfOverview, SWT.NONE);
		tiOverview.setText("Overview");
		
		createOverview(tiOverview);
//		createTimeStatus(tiOverview);
		
		setContent2Tables();
    }

	/**
	 * createTimeStatus
	 * @param bgComp
	 */
	private void createTimeStatus(TabItem tabItem) {
		Composite comTime = new Composite(tabItem.getControl().getShell(), SWT.NONE);
		tabItem.setControl(comTime);
		comTime.setLayout(new GridLayout(8, false));
		comTime.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

		Label lblStartTime = new Label(comTime, SWT.NONE);
		lblStartTime.setLayoutData(new GridData(SWT.CENTER));
		lblStartTime.setText(Messages.lblStartTime);
		Label txtStartTime = new Label(comTime, SWT.NONE);
		txtStartTime.setLayoutData(new GridData(SWT.CENTER));
		MigrationReport report = getReporter().getReport();
		txtStartTime.setText(CUBRIDTimeUtil.defaultFormatMilin(new Date(report.getTotalStartTime())));
		final Color clrBlue = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
		txtStartTime.setForeground(clrBlue);

		Label lblEndTime = new Label(comTime, SWT.NONE);
		lblEndTime.setLayoutData(new GridData(SWT.CENTER));
		lblEndTime.setText(Messages.lblEndTime);
		Label txtEndTime = new Label(comTime, SWT.NONE);
		txtEndTime.setLayoutData(new GridData(SWT.CENTER));
		txtEndTime.setText(CUBRIDTimeUtil.defaultFormatMilin(new Date(report.getTotalEndTime())));
		txtEndTime.setForeground(clrBlue);

		Label lblTotalTime = new Label(comTime, SWT.NONE);
		lblTotalTime.setLayoutData(new GridData(SWT.CENTER));
		lblTotalTime.setText(Messages.lblTotalTimeSpend);
		Label txtTotalTime = new Label(comTime, SWT.NONE);
		txtTotalTime.setLayoutData(new GridData(SWT.CENTER));
		txtTotalTime.setText(TimeZoneUtils.format(report.getTotalEndTime() - report.getTotalStartTime()));
		txtTotalTime.setForeground(clrBlue);
	}
	
	/**
	 * createOverview
	 * @param tiTables TabItem
	 */
	private void createOverview(TabItem tiTables) {
		TableViewerBuilder tvBuilder = new TableViewerBuilder();
		tvBuilder.setColumnNames(TABLE_HEADER_DATA);
		tvBuilder.setColumnWidths(new int[] {150, 150, 150, 130});
		tvBuilder.setColumnStyles(new int[] {SWT.LEFT, SWT.RIGHT, SWT.RIGHT, SWT.RIGHT});
		tvBuilder.setContentProvider(new ArrayContentProvider());
		tvBuilder.setLabelProvider(new OverviewResultTableLabelProvider());
		tvRecords = tvBuilder.buildTableViewer(tfOverview, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);

		tiTables.setControl(tvRecords.getTable());
	}
	
	/**
	 * @return MigrationReporter
	 */
	private MigrationReporter getReporter() {
		return (MigrationReporter) getEditorInput();
	}
	
	/**
	 * Fill the data to tables.
	 */
	private void setContent2Tables() {
		MigrationReporter reporter = getReporter();
		MigrationReport report = reporter.getReport();
		
		tvRecords.setInput(report.getRecMigResults());
	}
	
	@Override
    public void setFocus() {
    }
}

class OverviewResultTableLabelProvider extends RecordMigrationResultTableLabelProvider {
	
	@Override
	public String getColumnText(Object element, int columnIndex) {
		RecordMigrationResult rs = (RecordMigrationResult) element;
		switch (columnIndex) {
		case 0:
			return rs.getSource();
		case 1:
			return rs.getTotalCount() == 0 ? 
					MigrationReportEditorPart.EMPTY_CELL_VALUE : NumberFormat.getIntegerInstance().format(rs.getExpCount());
		case 2:
			return rs.getTotalCount() == 0 ? 
					MigrationReportEditorPart.EMPTY_CELL_VALUE : NumberFormat.getIntegerInstance().format(rs.getImpCount());
		case 3:
			return "-";
		default:
			return null;
		}
	}
	
	@Override
	public Color getBackground(Object element, int columnIndex) {
	    return null;
	}
}
