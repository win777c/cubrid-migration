package com.cubrid.cubridmigration.ui.wizard.editor.controller;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;

import com.cubrid.cubridmigration.core.ctc.CTCLoader;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.engine.IMigrationMonitor;

public class CTCProgressUIController extends MigrationProgressUIController {

//	private static final int INDEX_TABLE_NAME      = 0;
//	private static final int INDEX_EXPORTED_COLUMN = 1;
//	private static final int INDEX_IMPORTED_COLUMN = 2;
	
	enum INDEX {
		INDEX_TABLE_NAME, INDEX_EXPORTED_COLUMN, INDEX_IMPORTED_COLUMN
	}

	@Override
	public void startMigration(IMigrationMonitor monitor, int startMode) {
	    super.startMigration(monitor, startMode);
	}
	
	@Override
	public void stopMigrationNow() {
		super.stopMigrationNow();
		config.setFetchingEnd(true);
//		CTCLoader.closeConnection(config.getCtcHandleId());
	}
	
	@Override
	public int getProgressBarStyle() {
		// 언제 끝인지 알 수 없기 때문에 아래의 옵션으로 설정
		return SWT.INDETERMINATE;
	}
	
	@Override
	public String[][] getProgressTableInput() {
		// 최초 선택된 Table의 progress status 초기화한다.

		List<Table> tableList = new ArrayList<Table>();

		tableList.addAll(config.getSelectedTableList());

		int index = 0;

		tableItems = new String[tableList.size()][4];
		for (Table table : tableList) {
			String tableName = table.getName();
			Table tbl = config.getSrcTableSchema(table.getOwner(), tableName);

			if (config.isImplicitEstimate()) {
				tableItems[index] = new String[] { tableName, NA_STRING, NA_STRING, NA_STRING };
			} else if (tbl == null || tbl.getTableRowCount() == 0) {
				tableItems[index] = new String[] { tableName, NA_STRING, NA_STRING, NA_STRING };
			} else {
				tableItems[index] = new String[] { tableName, String.valueOf(tbl.getTableRowCount()), "0", "0" };
			}
			
			index++;
		}
		return tableItems;
	}
	
	@Override
	public String[] updateTableExpData(String tableName, long exp) {
		if (exp <= 0) {
			return new String[] {};
		}

		for (String[] item : tableItems) {
			if (item[0].equals(tableName)) {
				long newExp = getCellValue(item[1]) + exp;
				item[1] = String.valueOf(newExp);

				return item;
			}
		}

		return new String[] {};
	}
	
	@Override
	public String[] updateTableImpData(String tableName, long imp) {
		for (String[] item : tableItems) {
			if (item[0].equals(tableName)) {
				long newImp = getCellValue(item[2]) + imp;
				item[2] = String.valueOf(newImp);

				return item;
			}
		}
		return new String[] {};
	}
}
