/*
 * Copyright (C) 2009 Search Solution Corporation. All rights reserved by Search Solution. 
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met: 
 *
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer. 
 *
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution. 
 *
 * - Neither the name of the <ORGANIZATION> nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software without 
 *   specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY 
 * OF SUCH DAMAGE. 
 *
 */
package com.cubrid.cubridmigration.core.engine.scheduler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;

import com.cubrid.cubridmigration.core.common.PathUtils;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.engine.MigrationContext;
import com.cubrid.cubridmigration.core.engine.ThreadUtils;
import com.cubrid.cubridmigration.core.engine.UserDefinedDataHandlerManager;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.core.engine.config.SourceCSVConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceColumnConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceEntryTableConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceSQLTableConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceSequenceConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceTableConfig;
import com.cubrid.cubridmigration.core.engine.config.SourceViewConfig;
import com.cubrid.cubridmigration.core.engine.exception.BreakMigrationException;
import com.cubrid.cubridmigration.core.engine.task.IMigrationTask;
import com.cubrid.cubridmigration.core.engine.task.MigrationTaskFactory;

/**
 * MigrationTasksScheduler responses to schedule migration tasks.
 * 
 * @author Kevin Cao
 * @version 1.0 - 2011-8-30 created by Kevin Cao
 */
public class MigrationTasksScheduler {

	protected MigrationTaskFactory taskFactory;
	protected MigrationContext context;

	public MigrationTasksScheduler() {

	}

	/**
	 * Schedule migration tasks
	 * 
	 */
	public void schedule() {
		//Execute SQL tasks
		MigrationConfiguration config = context.getConfig();
		if (config.sourceIsSQL()) {
			List<String> files = config.getSqlFiles();
			for (String file : files) {
				executeTask2(taskFactory.createExportSQLTask(file));
			}
			await();
			return;
		}
		//Clean all no used objects
		config.cleanNoUsedConfigForStart();
		initUserDefinedHandlers();

		clearTargetDB();
		createTables();
		createViews();
		createSerials();

		executeUserSQLs();
		boolean constrainsCreated = false;
		//If HA mode, the constraints should be created firstly.
		if (config.targetIsOnline() && config.isCreateConstrainsBeforeData()) {
			constrainsCreated = true;
			createPKs();
		}
		createRecords();

		if (!constrainsCreated) {
			createPKs();
		}
		createIndexes();
		createFKs();
		executeUserSQLs2();
		updateAutoIncColumnsCurrentValue();
		//Export functions/procedures/triggers to a txt file
		if (config.isExportNoSupportObjects()) {
			createFunctions();
			createProcedures();
			createTriggers();
		}
		updateIndexStatistics();
	}

	/**
	 * Update auto_increment columns current values
	 * 
	 */
	private void updateAutoIncColumnsCurrentValue() {
		if (!context.getConfig().targetIsOnline()) {
			return;
		}
		executeTask(taskFactory.createUpdateAiColumnsCurValTask(context.getConfig()));
	}

	/**
	 * Initialize the user defined data handlers.
	 * 
	 */
	private void initUserDefinedHandlers() {
		MigrationConfiguration config = context.getConfig();
		UserDefinedDataHandlerManager udf = UserDefinedDataHandlerManager.getInstance();
		List<SourceEntryTableConfig> setcs = config.getExpEntryTableCfg();
		for (SourceEntryTableConfig setc : setcs) {
			for (SourceColumnConfig scc : setc.getColumnConfigList()) {
				if (StringUtils.isBlank(scc.getUserDataHandler())) {
					continue;
				}
				if (!udf.putColumnDataHandler(scc.getUserDataHandler(), false)) {
					throw new BreakMigrationException("Data handler '" + scc.getUserDataHandler()
							+ "' was not found.");
				}
			}
		}
		List<SourceSQLTableConfig> sstcs = config.getExpSQLCfg();
		for (SourceSQLTableConfig sstc : sstcs) {
			for (SourceColumnConfig scc : sstc.getColumnConfigList()) {
				if (StringUtils.isBlank(scc.getUserDataHandler())) {
					continue;
				}
				if (!udf.putColumnDataHandler(scc.getUserDataHandler(), false)) {
					throw new BreakMigrationException("Data handler '" + scc.getUserDataHandler()
							+ "' was not found.");
				}
			}
		}
	}

	/**
	 * Execute the SQLs which were set to be executed before data migration
	 * 
	 */
	private void executeUserSQLs() {
		MigrationConfiguration config = context.getConfig();
		List<SourceEntryTableConfig> setcs = config.getExpEntryTableCfg();
		for (SourceEntryTableConfig setc : setcs) {
			if (StringUtils.isBlank(setc.getSqlBefore())) {
				continue;
			}
			executeTask(taskFactory.createExecuteSQLTask(setc, setc.getSqlBefore()));
		}
		await();
	}

	/**
	 * Execute the SQLs which were set to be executed after migration
	 * 
	 */
	private void executeUserSQLs2() {
		MigrationConfiguration config = context.getConfig();
		List<SourceEntryTableConfig> setcs = config.getExpEntryTableCfg();
		for (SourceEntryTableConfig setc : setcs) {
			if (StringUtils.isBlank(setc.getSqlAfter())) {
				continue;
			}
			executeTask(taskFactory.createExecuteSQLTask(setc, setc.getSqlAfter()));
		}
		await();
	}

	/**
	 * Clear target database
	 * 
	 */
	private void clearTargetDB() {
		MigrationConfiguration config = context.getConfig();
		if (config.targetIsFile()) {
			PathUtils.deleteFile(new File(config.getTargetSchemaFileName()));
			PathUtils.deleteFile(new File(config.getTargetDataFileName()));
			PathUtils.deleteFile(new File(config.getTargetIndexFileName()));
		}
		executeTask(taskFactory.createCleanDBTask());
	}

	/**
	 * Waiting for step finished.
	 * 
	 */
	protected void await() {
		while (context.isExecutorsBusy()) {
			ThreadUtils.threadSleep(1000, null);
		}
	}

	/**
	 * Execute create DB object task
	 * 
	 * @param task IMigrationTask
	 */
	protected void executeTask(IMigrationTask task) {
		context.getDbObjectExe().execute((Runnable) task);
	}

	/**
	 * Execute migration records task
	 * 
	 * @param task Migration records task
	 */
	protected void executeTask2(IMigrationTask task) {
		context.getExportRecExe().execute((Runnable) task);
	}

	/**
	 * Schedule export table schema tasks.
	 * 
	 */
	protected void createTables() {
		MigrationConfiguration config = context.getConfig();
		List<SourceEntryTableConfig> sourceTables = config.getExpEntryTableCfg();
		List<SourceSQLTableConfig> sourceSQLTables = config.getExpSQLCfg();
		List<SourceCSVConfig> sourceCSVFiles = config.getCSVConfigs();
		List<String> tableCreated = new ArrayList<String>();
		if (config.isCtcMode()) {
			List<Table> selectedTableList = config.getSelectedTableList();
			for (Table table : selectedTableList) {
				executeTask(taskFactory.createImportTableSchemaTask(table, false, false));
			}
		}
		for (SourceEntryTableConfig st : sourceTables) {
			if (!st.isCreateNewTable()) {
				continue;
			}
			if (tableCreated.indexOf(st.getTarget()) >= 0) {
				continue;
			}
			tableCreated.add(st.getTarget());
			executeTask(taskFactory.createExportTableSchemaTask(st));
		}
		for (SourceSQLTableConfig st : sourceSQLTables) {
			if (!st.isCreateNewTable()) {
				continue;
			}
			if (tableCreated.indexOf(st.getTarget()) >= 0) {
				continue;
			}
			tableCreated.add(st.getTarget());
			executeTask(taskFactory.createExportTableSchemaTask(st));
		}
		//Create CSV file table
		for (SourceCSVConfig scc : sourceCSVFiles) {
			if (!scc.isCreate()) {
				continue;
			}
			if (tableCreated.indexOf(scc.getTarget()) >= 0) {
				continue;
			}
			tableCreated.add(scc.getTarget());
			executeTask(taskFactory.createExportCSVTableSchemaTask(scc));
		}
		await();
	}

	/**
	 * Schedule export view tasks.
	 * 
	 */
	protected void createViews() {
		MigrationConfiguration config = context.getConfig();
		List<SourceViewConfig> views = config.getExpViewCfg();
		for (SourceViewConfig vw : views) {
			executeTask(taskFactory.createExportViewTask(vw));
		}
		await();
	}

	/**
	 * Schedule export table record tasks.
	 * 
	 */
	protected void createRecords() {
		MigrationConfiguration config = context.getConfig();
		boolean isMigData = false;
		List<SourceEntryTableConfig> entryTables = config.getExpEntryTableCfg();
		for (SourceTableConfig table : entryTables) {
			isMigData = isMigData || table.isMigrateData();
		}
		List<SourceSQLTableConfig> sqlTables = config.getExpSQLCfg();
		for (SourceTableConfig table : sqlTables) {
			isMigData = isMigData || table.isMigrateData();
		}
		List<SourceCSVConfig> csvs = config.getCSVConfigs();
		isMigData = isMigData || !csvs.isEmpty();
		// CTC
		if (config.isCtcMode()) {
			executeTask2(taskFactory.createCTCExportTask(context));
			return;
		}
		//If no data to be migrated, return
		if (!isMigData) {
			return;
		}
		if (config.sourceIsOnline()) {
			//schedule exporting tasks
			for (SourceTableConfig table : entryTables) {
				if (!table.isMigrateData()) {
					continue;
				}
				executeTask2(taskFactory.createExportTableRecordsTask(table));
			}
			for (SourceTableConfig table : sqlTables) {
				if (!table.isMigrateData()) {
					continue;
				}
				executeTask2(taskFactory.createExportTableRecordsTask(table));
			}
		} else if (config.sourceIsXMLDump()) {
			executeTask(taskFactory.createExportAllRecordsTask());
		} else if (config.sourceIsCSV()) {
			for (SourceCSVConfig csv : csvs) {
				executeTask(taskFactory.createExportCSVTask(csv));
			}
		}
		await();
	}

	/**
	 * Schedule export Primary Key tasks.
	 * 
	 */
	protected void createPKs() {
		MigrationConfiguration config = context.getConfig();
		List<SourceEntryTableConfig> stables = config.getExpEntryTableCfg();
		List<String> names = new ArrayList<String>();
		for (SourceTableConfig tb : stables) {
			if (!tb.isCreateNewTable()) {
				continue;
			}
			if (!((SourceEntryTableConfig) tb).isCreatePK()) {
				continue;
			}
			final String name = tb.getTarget().trim().toLowerCase(Locale.US);
			if (names.indexOf(name) >= 0) {
				continue;
			}
			names.add(name);
			executeTask(taskFactory.createExportPKTask(tb));
		}
		await();
	}

	/**
	 * Schedule export foreign key tasks.
	 * 
	 */
	protected void createFKs() {
		MigrationConfiguration config = context.getConfig();
		List<SourceEntryTableConfig> stables = config.getExpEntryTableCfg();
		List<String> names = new ArrayList<String>();
		for (SourceTableConfig tb : stables) {
			if (!tb.isCreateNewTable()) {
				continue;
			}
			final String name = tb.getTarget().trim().toLowerCase(Locale.US);
			if (names.indexOf(name) >= 0) {
				continue;
			}
			names.add(name);
			executeTask(taskFactory.createExportFKTask(tb));
		}
		await();
	}

	/**
	 * Schedule export index tasks.
	 * 
	 */
	protected void createIndexes() {
		MigrationConfiguration config = context.getConfig();
		List<SourceEntryTableConfig> stables = config.getExpEntryTableCfg();
		List<String> names = new ArrayList<String>();
		for (SourceTableConfig tb : stables) {
			if (!tb.isCreateNewTable()) {
				continue;
			}
			final String name = tb.getTarget().trim().toLowerCase(Locale.US);
			if (names.indexOf(name) >= 0) {
				continue;
			}
			names.add(name);
			executeTask(taskFactory.createExportIndexTask(tb));
		}
		await();
	}

	/**
	 * Schedule export sequence tasks.
	 * 
	 */
	protected void createSerials() {
		MigrationConfiguration config = context.getConfig();
		List<SourceSequenceConfig> sequences = config.getExpSerialCfg();
		for (SourceSequenceConfig sq : sequences) {
			executeTask(taskFactory.createExportSequenceTask(sq));
		}
		await();
	}

	/**
	 * Schedule export function tasks.
	 * 
	 */
	protected void createFunctions() {
		MigrationConfiguration config = context.getConfig();
		List<String> functions = config.getExpFunctionCfg();
		for (String ft : functions) {
			executeTask(taskFactory.createExportFunctionTask(ft));
		}
		await();
	}

	/**
	 * Schedule export procedure tasks.
	 * 
	 */
	protected void createProcedures() {
		MigrationConfiguration config = context.getConfig();
		List<String> procedures = config.getExpProcedureCfg();
		for (String pd : procedures) {
			executeTask(taskFactory.createExportProcedureTask(pd));
		}
		await();
	}

	/**
	 * Schedule export trigger tasks.
	 * 
	 */
	protected void createTriggers() {
		MigrationConfiguration config = context.getConfig();
		List<String> triggers = config.getExpTriggerCfg();
		for (String tg : triggers) {
			executeTask(taskFactory.createExportTriggerTask(tg));
		}
		await();
	}

	/**
	 * Append update sql for updating statistics of all indexes
	 */
	private void updateIndexStatistics() {
		executeTask(taskFactory.createUpdateStatisticsTask());
	}

	public void setTaskFactory(MigrationTaskFactory taskFactory) {
		this.taskFactory = taskFactory;
	}

	public void setContext(MigrationContext context) {
		this.context = context;
	}
}
