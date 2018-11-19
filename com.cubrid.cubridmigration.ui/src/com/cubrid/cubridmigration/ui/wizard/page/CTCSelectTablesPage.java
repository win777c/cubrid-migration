package com.cubrid.cubridmigration.ui.wizard.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.cubrid.common.ui.swt.table.TableViewerBuilder;
import com.cubrid.cubridmigration.core.dbobject.Catalog;
import com.cubrid.cubridmigration.core.dbobject.Column;
import com.cubrid.cubridmigration.core.dbobject.Schema;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.ui.MigrationUIPlugin;
import com.cubrid.cubridmigration.ui.wizard.MigrationWizard;

/**
 * Select Tables Wizard Page for CUBRID Transaction Capture
 * 
 * Selects tables to capture
 * 
 * CTCSelectTablesPage
 * @author win
 */
public class CTCSelectTablesPage extends MigrationWizardPage {

	private static final int[] SASH_FORM_WEIGHTS = { 50, 50 };

	private Composite          mainComposite;

	private TableViewer        selectTableViewer;
	private TableViewer        columnInformationTableViewer;

	private Text               txtSelectedTableName;
	
	public CTCSelectTablesPage(String pageName) {
		super(pageName);
	}
	
	@Override
	public void createControl(Composite parent) {
		mainComposite = new Composite(parent, SWT.None);  
		mainComposite.setLayout(new GridLayout());
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
	    SashForm sashForm = createSashForm();
	    
	    createSelectTableGroup(sashForm);
	    createColumnInformationTableGroup(sashForm);
	    
	    sashForm.setWeights(SASH_FORM_WEIGHTS);
	    
	    setControl(mainComposite);
	    
	    afterShowCurrentPage(null);
	}

	/**
	 * createSashForm
	 * @return
	 */
	private SashForm createSashForm() {
	    SashForm sashForm = new SashForm(mainComposite, SWT.None);
	    sashForm.setLayout(new GridLayout());
	    sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
	    sashForm.setFocus();
	    
	    return sashForm;
    }

	/**
	 * createSelectTreeGroup
	 * @param sashForm
	 */
	private void createSelectTableGroup(SashForm sashForm) {
		Group selectedTableGroup = new Group(sashForm, SWT.None);
		selectedTableGroup.setText("Select Tables");
		selectedTableGroup.setLayout(new GridLayout());
		selectedTableGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		
	    createSelectTableViewer(selectedTableGroup);
	    createButtonsComposite(selectedTableGroup);
    }

	/**
	 * createButtonsComposite
	 * @param selectedTableGroup
	 */
	private void createButtonsComposite(Group selectedTableGroup) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginWidth = 0;

		Composite buttonsComposite = new Composite(selectedTableGroup, SWT.None | SWT.RIGHT);
		buttonsComposite.setLayout(gridLayout);
		buttonsComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

		GridData btnGridData = new GridData(GridData.FILL);
		btnGridData.widthHint = 90;
		
		Button btnSelectAll = new Button(buttonsComposite, SWT.None);
		btnSelectAll.setLayoutData(btnGridData);
		btnSelectAll.setText("Select all");
		btnSelectAll.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doSelectAll();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		Button btnDeselectAll = new Button(buttonsComposite, SWT.None);
		btnDeselectAll.setLayoutData(btnGridData);
		btnDeselectAll.setText("Deselect all");
		btnDeselectAll.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doDeselectAll();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}

	/**
	 * setTableItemChecked
	 * @param checked
	 * @return
	 */
	private org.eclipse.swt.widgets.Table setTableItemChecked(boolean checked) {
		org.eclipse.swt.widgets.Table table = selectTableViewer.getTable();
		for (TableItem tableItem : table.getItems()) {
			tableItem.setChecked(checked);
		}
	    return table;
    }
	
	/**
	 * doSelectAll
	 */
	private void doSelectAll() {
		org.eclipse.swt.widgets.Table table = setTableItemChecked(true);
		table.selectAll();
		initTableViewer();
    }
	
	/**
	 * doDeselectAll
	 */
	private void doDeselectAll() {
		org.eclipse.swt.widgets.Table table = setTableItemChecked(false);
		table.deselectAll();
		initTableViewer();
    }

	/**
	 * initTableViewer
	 */
	private void initTableViewer() {
	    selectTableViewer.refresh();
		initColumnInformationTableViewer();
    }
	
	/**
	 * initColumnInformationTableViewer
	 */
	private void initColumnInformationTableViewer() {
		txtSelectedTableName.setText("");
		columnInformationTableViewer.setInput(new ArrayList());
    }
	
	/**
	 * createSelectTableViewer
	 * @param selectedTableGroup
	 */
	private void createSelectTableViewer(Group selectedTableGroup) {
		selectTableViewer = new TableViewer(selectedTableGroup, SWT.None | SWT.BORDER | SWT.CHECK | SWT.MULTI);
		selectTableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		selectTableViewer.setColumnProperties(new String[] { "Table name" });
		selectTableViewer.setContentProvider(new TableViewerContentProvider());
		selectTableViewer.setLabelProvider(new LeftTableLabelProvider());
		selectTableViewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
			    return super.compare(viewer, e1, e2);
			}
		});
		selectTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selectedObj = (IStructuredSelection) event.getSelection();
				if (selectedObj.getFirstElement() instanceof Table) {
					Table table = (Table) selectedObj.getFirstElement();
					columnInformationTableViewer.setInput(table.getColumns());
					columnInformationTableViewer.refresh();
					
					txtSelectedTableName.setText(table.getName());
				}
			}
		});
		
		org.eclipse.swt.widgets.Table table = selectTableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		TableColumn tableColumn = new TableColumn(table, SWT.None);
		tableColumn.setWidth(370);
		tableColumn.setText("Table name");

		selectTableViewer.refresh();
	}
	
	/**
	 * createSelectedTableGroup
	 * @param sashForm
	 */
	private void createColumnInformationTableGroup(SashForm sashForm) {
		Group selectTablesGroup = new Group(sashForm, SWT.None);
		selectTablesGroup.setText("Selected Table Information");
		selectTablesGroup.setLayout(new GridLayout());
		selectTablesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite tableNameComposite = new Composite(selectTablesGroup, SWT.None);
		tableNameComposite.setLayout(new GridLayout(2, false));
		tableNameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label lblSelectedTable = new Label(tableNameComposite, SWT.None);
		lblSelectedTable.setText("Table name : ");
		
		txtSelectedTableName = new Text(tableNameComposite, SWT.None | SWT.BORDER);
		txtSelectedTableName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		txtSelectedTableName.setEditable(false);
		
		TableViewerBuilder tvBuilder = new TableViewerBuilder();
		tvBuilder.setColumnNames(new String[] { "Column name", "Data Type" });
		tvBuilder.setColumnWidths(new int[] { 185, 185 });
		tvBuilder.setContentProvider(new TableViewerContentProvider());
		tvBuilder.setLabelProvider(new RightTableLabelProvider());
		columnInformationTableViewer = tvBuilder.buildTableViewer(selectTablesGroup, SWT.None | SWT.BORDER | SWT.FULL_SELECTION);
    }
	
	@Override
	protected void afterShowCurrentPage(PageChangedEvent event) {
		try {
			setTitle("Select Tables Page");
			setDescription("Please, Select Tables");
			loadSourceTables();
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		} finally {
			isFirstVisible = false;
		}
	}
	
	protected void handlePageLeaving(PageChangingEvent event) {
		// If page is not complete, it should be go to previous page.
		if (!isPageComplete()) {
			return;
		}
		if (!isGotoNextPage(event)) {
			return;
		}
		event.doit = updateMigrationConfig();
	}

	public void loadSourceTables() {
		MigrationConfiguration migrationConfig = getMigrationWizard().getMigrationConfig();
		
		if (!migrationConfig.isCtcMode()) {
			return;
		}
		
		MigrationWizard migrationWizard = getMigrationWizard();
		
		Catalog sourceCatalog = migrationWizard.getSourceCatalog();
		Catalog targetCatalog = migrationWizard.getTargetCatalog();
		
		List<Table> srcTableList = getTableList(sourceCatalog);
		List<Table> trgTableList = getTableList(targetCatalog);
		
		Map<String, Table> srcTableMap = createTableMap(srcTableList);
		Map<String, Table> trgTableMap = createTableMap(trgTableList);
		
		List<Table> capturableTableList = getCapturableTableList(srcTableMap, trgTableMap);
		
		selectTableViewer.setInput(capturableTableList);
	}
	
	private List<Table> getTableList(Catalog catalog) {
		
		List<Schema> schemaList = null;

		try {
			schemaList = catalog.getSchemas();
		} catch (Exception e) {
			return new ArrayList();
		}
		
		Schema schema = schemaList.get(0);
		
		return schema.getTables();
    }
	
	/**
	 * createTableMap
	 * @param tableList
	 * @return
	 */
	public Map<String, Table> createTableMap(List<Table> tableList) {
		Map<String, Table> map = new HashMap<String, Table>();
		for (Table table : tableList) {
			map.put(table.getName(), table);
		}
		return map;
	}
	
	/**
	 * createColumnMap
	 * @param table
	 * @return
	 */
	public Map<String, Column> createColumnMap(Table table) {
		Map<String, Column> map = new HashMap<String, Column>();
		for (Column column : table.getColumns()) {
			map.put(column.getName(), column);
		}
		return map;
	}
	
	/**
	 * getCapturableTableList
	 * @param srcTableMap
	 * @param trgTableMap
	 * @return
	 */
	private List<Table> getCapturableTableList(Map srcTableMap, Map trgTableMap) {
		List<Table> tableList = new ArrayList<Table>();
        
		Iterator<Table> iterator = srcTableMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry entry = (Entry) iterator.next();
			Table srcTable = (Table) entry.getValue();
			Table trgTable = (Table) trgTableMap.get(srcTable.getName());
			if (trgTable == null) { // 동일한 테이블이 타겟DB에 없다고 판단.
				tableList.add(srcTable);
			} else if (trgTable != null) {
				if (isSameColumnCount(srcTable, trgTable)) { // 컬럼 개수 비교
					if (isSameColumn(srcTable, trgTable)) { // 컬럼을 비교 후 모두 동일하다고 판단되면 Tablelist에 add함
						tableList.add(srcTable);
					}
				} else { // 컬럼 개수 비교해서 다를 경우, 스키마가 다르다고 판단
					continue;
				}
			}
		}
		
		return tableList;
	}

	private boolean isSameColumn(Table srcTable, Table trgTable) {
	    Map<String, Column> srcTableColumnMap = createColumnMap(srcTable);
	    Map<String, Column> trgTableColumnMap = createColumnMap(trgTable);
	    Iterator<String> iterator = srcTableColumnMap.keySet().iterator();
		while (iterator.hasNext()) {
			String srcColumnName = iterator.next();
			Column srcColumn = srcTableColumnMap.get(srcColumnName);
			Column trgColumn = trgTableColumnMap.get(srcColumnName);
			if (trgColumn == null) {
				return false;
			} else {
				if(!trgColumn.getShownDataType().equalsIgnoreCase(srcColumn.getShownDataType())) {
					return false;
				}
			}
	    }
		
		return true;
    }

	private boolean isSameColumnCount(Table srcTbl, Table trgTbl) {
		int srcTblColumnCount = srcTbl.getColumns().size();
		int trgTblColumnCount = trgTbl.getColumns().size();
		
		if (srcTblColumnCount == trgTblColumnCount) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	protected boolean updateMigrationConfig() {
		MigrationWizard migrationWizard = getMigrationWizard();
		MigrationConfiguration migrationConfig = migrationWizard.getMigrationConfig();
		
		migrationConfig.setSelectedTableList(getSelectedTableList());
		
	    return super.updateMigrationConfig();
	}
	
	private List<Table> getSelectedTableList() {
		List selectedTableList = new ArrayList();

		org.eclipse.swt.widgets.Table table = selectTableViewer.getTable();

		for (TableItem tableItem : table.getItems()) {
			if (tableItem.getChecked()) {
				selectedTableList.add(tableItem.getData());
			}
		}

		return selectedTableList;
	}
}

class RightTableLabelProvider implements ITableLabelProvider {

	private static final String ICON_COLUMN_PATH = "icon/db/table_column_item.png";

	private static final int    INDEX_COLUMN_NAME     = 0;
	private static final int    INDEX_COLUMN_DATATYPE = 1;

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (!(element instanceof Column)) {
			return null;
		}

		switch (columnIndex) {
		case INDEX_COLUMN_NAME:
			return MigrationUIPlugin.getImage(ICON_COLUMN_PATH);
		default:
			return null;
		}
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof Column)) {
			return "";
		}

		Column column = (Column) element;
		switch (columnIndex) {
		case INDEX_COLUMN_NAME:
			return column.getName();
		case INDEX_COLUMN_DATATYPE:
			return column.getShownDataType();
		default:
			return "";
		}
	}
}

class LeftTableLabelProvider implements ITableLabelProvider {

	private static final String ICON_TABLE_PATH = "icon/db/table.png";

	private static final int    INDEX_TABLE_NAME     = 0;

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (!(element instanceof Table)) {
			return null;
		}

		switch (columnIndex) {
		case INDEX_TABLE_NAME:
			return MigrationUIPlugin.getImage(ICON_TABLE_PATH);
		default:
			return null;
		}
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof Table)) {
			return "";
		}

		Table table = (Table) element;
		switch (columnIndex) {
		case INDEX_TABLE_NAME:
			return table.getName();
		default:
			return "";
		}
	}
}

/**
 * TableViewerContentProvider
 * @author win
 */
class TableViewerContentProvider implements IStructuredContentProvider {

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof List) {
			return ((List) inputElement).toArray();
		}
		return new Object[0];
	}
}