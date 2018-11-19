package com.cubrid.cubridmigration.core.engine.exporter.impl;

import com.cubrid.cubridmigration.core.engine.JDBCConManager;
import com.cubrid.cubridmigration.core.engine.MigrationStatusManager;
import com.cubrid.cubridmigration.core.engine.RecordExportedListener;
import com.cubrid.cubridmigration.core.engine.config.SourceTableConfig;
import com.cubrid.cubridmigration.core.engine.exporter.MigrationExporter;

public class CTCExporter extends MigrationExporter {

	protected MigrationStatusManager msm;
	
	protected JDBCConManager connManager;
	
	public void exportTableRecords(SourceTableConfig st, RecordExportedListener oneNewRecord) {
	}

	public void exportAllRecords(RecordExportedListener oneNewRecord) {
	}
	
	public void setConnManager(JDBCConManager connManager) {
		this.connManager = connManager;
	}
	
	public void setStatusManager(MigrationStatusManager msm) {
		this.msm = msm;
	}
}
