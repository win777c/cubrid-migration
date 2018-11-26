package com.cubrid.cubridmigration.core.engine.task.exp;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;

import com.cubrid.cubridmigration.core.common.DBUtils;
import com.cubrid.cubridmigration.core.ctc.CTCLoader;
import com.cubrid.cubridmigration.core.ctc.ICTCConstants;
import com.cubrid.cubridmigration.core.ctc.model.CTCJsonModel;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.engine.IMigrationEventHandler;
import com.cubrid.cubridmigration.core.engine.JDBCConManager;
import com.cubrid.cubridmigration.core.engine.MigrationContext;
import com.cubrid.cubridmigration.core.engine.ThreadUtils;
import com.cubrid.cubridmigration.core.engine.config.MigrationConfiguration;
import com.cubrid.cubridmigration.core.engine.config.SourceTableConfig;
import com.cubrid.cubridmigration.core.engine.event.ExportRecordsEvent;
import com.cubrid.cubridmigration.core.engine.event.ImportRecordsEvent;
import com.cubrid.cubridmigration.core.engine.task.ExportTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public class CTCExportTask extends ExportTask {
	
	public static final String        CTC_BROKER_URL = "vm://localhost";
	
	private static final int          FETCH_SIZE     = 4096;
	private static final Gson         gson           = new GsonBuilder().create();

	private MigrationConfiguration    migrationConfig;

	private MigrationContext          context;

	private ActiveMQConnectionFactory connectionFactory;
	private Connection                connection;
	private Session                   session;
	
	private JDBCConManager            connManager;
	
	public CTCExportTask(MigrationContext context) {
		this.context = context;
		this.migrationConfig = context.getConfig();
		this.connManager = context.getConnManager();
	}
	
	@Override
	protected void executeExportTask() {
		try {
			run("queueName");
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * run
	 * @param queueName
	 */
	public void run(String queueName) {
		Session session = null;
		try {
			session = connect(CTC_BROKER_URL);

			Destination destination = createQueue(session, queueName);

			MessageProducer messageProducer = session.createProducer(destination);
			MessageConsumer messageConsumer = session.createConsumer(destination);
			messageConsumer.setMessageListener(new MessageListenerAdapter() {

				@Override
				public void onMessage(Message message, Session session) throws JMSException {
					super.onMessage(message, session);
				}
				
				public void onMessage(Message message) {
					
					if ("".equals(message) || message == null) {
						return;
					}
					
					if (!(message instanceof TextMessage)) {
						return;
					}
					
					TextMessage textMesage = (TextMessage) message;
					String messageText = "";
					
					try {
						messageText = textMesage.getText();
                    } catch (JMSException e) {
	                    e.printStackTrace();
                    }

					try {
						textMesage.acknowledge();
                    } catch (JMSException e) {
	                    e.printStackTrace();
	                    return;
                    }

					CTCJsonModel[] ctcJsonModels = gson.fromJson(messageText, CTCJsonModel[].class);
					
					final List<String> sqlList = new ArrayList<String>();
					for (CTCJsonModel ctcJsonModel : ctcJsonModels) {
						createSQLStatement(sqlList, ctcJsonModel);
					}
					
					java.sql.Connection targetConnection = connManager.getTargetConnection();
					for (String sql : sqlList) {
						executeSQL(targetConnection, sql);
					}
					DBUtils.commit(targetConnection);

					Map<String, Integer> tableSummaryMap = getTableSummaryMap(ctcJsonModels);
					Iterator<String> iterator = tableSummaryMap.keySet().iterator();
					while (iterator.hasNext()) {
						String tableName = iterator.next();
						int statementCount = tableSummaryMap.get(tableName);

						final SourceTableConfig stc = new SourceTableConfig();
						stc.setName(tableName);
						
						final ImportRecordsEvent importRecordsEvent = new ImportRecordsEvent(stc, statementCount);
						IMigrationEventHandler eventsHandler = context.getEventsHandler();
						eventsHandler.handleEvent(importRecordsEvent);
					}
				}
			}); 

			sendTextMessages(messageProducer);
			
		} catch (Exception e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			try { session.close(); } catch (Exception e) {}
			try { connection.stop(); } catch (Exception e) {}
			try { connection.close(); } catch (Exception e) {}
		}
	}

	/**
	 * getTableSummaryMap
	 * @param ctcJsonModels
	 * @return
	 */
	public Map<String, Integer> getTableSummaryMap(CTCJsonModel[] ctcJsonModels) {
		Map<String, Integer> tableSummaryMap = new HashMap<String, Integer>();

		for (CTCJsonModel ctcJsonModel : ctcJsonModels) {
			String tableName = ctcJsonModel.getTableName();
			if (tableSummaryMap.get(tableName) == null) {
				tableSummaryMap.put(tableName, 1);
			} else {
				tableSummaryMap.put(tableName, tableSummaryMap.get(tableName) + 1);
			}
		}

		return tableSummaryMap;
	}
	
	/**
	 * createSQL
	 * @param model
	 */
	private void createSQLStatement(List<String> sqlList, CTCJsonModel model) {
		String statementType = model.getStatementType();
		
		String sqlStatement = "";
		
		if ("insert".equalsIgnoreCase(statementType)) {
			sqlStatement = createInsertDML(model);
		} else if ("update".equalsIgnoreCase(statementType)) {
			sqlStatement = createUpdateDML(model);
		} else if ("delete".equalsIgnoreCase(statementType)) {
			sqlStatement = createDeleteDML(model);
		}
		
		sqlList.add(sqlStatement);
	}
	
	/**
	 * createUpdateDML
	 * @param transactionModel
	 * @return
	 */
	private String createUpdateDML(CTCJsonModel transactionModel) {
		StringBuffer sb = new StringBuffer();

		String statementType = transactionModel.getStatementType();
		String tableName = transactionModel.getTableName();
		
		Map<String, Object> columns = transactionModel.getColumns();
		Map<String, Object> keyColumns = transactionModel.getKeyColumns();
		
		sb.append(statementType).append(" ").append(tableName).append(" set ");
		
		createColumnsStatement(sb, columns);
		createKeyColumnsStatement(sb, keyColumns);
		
		return sb.toString();
	}

	/**
	 * createColumnsStatement
	 * @param sb
	 * @param columns
	 */
	private void createColumnsStatement(StringBuffer sb, Map<String, Object> columns) {
	    Iterator<String> columnIterator = columns.keySet().iterator();
		while (columnIterator.hasNext()) {
			String columnName = columnIterator.next();
			Object columnValue = columns.get(columnName);
			
			sb.append(columnName).append(" = ");
			if (columnValue instanceof java.lang.String || columnValue instanceof java.lang.Character) {
				sb.append("'").append(columnValue).append("'");
			} else {
				sb.append(columnValue);
			}
			
			if (columnIterator.hasNext()) {
				sb.append(", ");
			}
		}
    }

	/**
	 * createKeyColumnsStatement
	 * @param sb
	 * @param keyColumns
	 */
	private void createKeyColumnsStatement(StringBuffer sb, Map<String, Object> keyColumns) {
		sb.append(" where ");
		Iterator<String> keyColumnIterator = keyColumns.keySet().iterator();
		while(keyColumnIterator.hasNext()) {
			String columnName = keyColumnIterator.next();
			Object columnValue = keyColumns.get(columnName);
			
			sb.append(columnName).append(" = ");
			if (columnValue instanceof java.lang.String || columnValue instanceof java.lang.Character) {
				sb.append("'").append(columnValue).append("'");
			} else {
				sb.append(columnValue);
			}
			
			if (keyColumnIterator.hasNext()) {
				sb.append(" and ");
			}
		}
    }
	
	/**	
	 * createDeleteDML
	 * @param transactionModel
	 * @return
	 */
	private String createDeleteDML(CTCJsonModel transactionModel) {
		StringBuffer sb = new StringBuffer();

		String statementType = transactionModel.getStatementType();
		String tableName = transactionModel.getTableName();
		Map<String, Object> keyColumns = transactionModel.getKeyColumns();

		sb.append(statementType).append(" from ").append(tableName);

		if (keyColumns.size() > 0) {
			createKeyColumnsStatement(sb, keyColumns);
		}

		return sb.toString();
	}
	
	/**
	 * createInsertDML
	 * @param transactionModel
	 * @return
	 */
	private String createInsertDML(CTCJsonModel transactionModel) {
		StringBuffer sb = new StringBuffer();
		
		Map<String, Object> columns = transactionModel.getColumns();
	    
	    sb.append(transactionModel.getStatementType())
	    	.append(" INTO ")
	    	.append(transactionModel.getTableName())
	    	.append(" (");
	    
	    Iterator<String> columnIterator = columns.keySet().iterator();
		while (columnIterator.hasNext()) {
			String columnName = columnIterator.next();
			sb.append(columnName);
			
			if (columnIterator.hasNext()) {
				sb.append(", ");
			}
		}
	    
	    sb.append(") ").append("values (");

		int columnSize = columns.size();
		int index = 0;
		for (Object s : columns.values()) {
			index++;
			sb.append("'").append(s).append("'");
			if (index != columnSize) {
				sb.append(", ");
			}
		}
	    
	    sb.append(");");
	    
	    return sb.toString();
    }
	
	/**
	 * executeSQL
	 * @param sql
	 */
	private void executeSQL(java.sql.Connection conn, String sql) {
		PreparedStatement pstmt = null;
		
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
	
	/**
	 * connect
	 * @return
	 * @throws JMSException
	 */
	private Session connect(String url) throws JMSException {
		connectionFactory = new ActiveMQConnectionFactory();
		connectionFactory.setBrokerURL(url);

		connection = connectionFactory.createConnection();
		connection.start();
		
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		
		setCtcConfiguration();
		
		return session;
	}

	/**
	 * setCtcConfiguration
	 */
	private void setCtcConfiguration() {
		migrationConfig.setConnectionFactory(connectionFactory);
		migrationConfig.setConnection(connection);
		migrationConfig.setSession(session);
		migrationConfig.setFetchingEnd(false);
	}
	
	/**
	 * createQueue
	 * @param session
	 * @param queueName
	 * @return
	 * @throws JMSException
	 */
	private Queue createQueue(Session session, String queueName) throws JMSException {
		return session.createQueue(queueName);
	}

	/**
	 * changeDeliveryMode
	 * @param messageProducer
	 */
	public void changeDeliveryMode(MessageProducer messageProducer) {
		try {
	        messageProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			messageProducer.setTimeToLive(5000);
        } catch (JMSException e) {
	        e.printStackTrace();
        }
	}

	/**
	 * sendTextMessages
	 * @param messageProducer
	 */
	private synchronized void sendTextMessages(MessageProducer messageProducer) {
		TextMessage message = new ActiveMQTextMessage();

		List<Table> selectedTableList = migrationConfig.getSelectedTableList();
		
		String dbUserName = migrationConfig.getSourceConParams().getConUser();
		int ctcHandleId = migrationConfig.getCtcHandleId();
		int jobDescriptor = CTCLoader.addJob(ctcHandleId);
		
		for (Table table : selectedTableList) {
			CTCLoader.registerTable(ctcHandleId, jobDescriptor, dbUserName, table.getName());
		}

		CTCLoader.startCapture(ctcHandleId, jobDescriptor);
		
		while (!migrationConfig.isFetchingEnd()) {
			try {
				// FETCH CAPTURED TRANSACTION
				CTCJsonModel[] ctcJsonModels = fetchCaptureTransaction(ctcHandleId, jobDescriptor, FETCH_SIZE);

				if (ctcJsonModels == null) {
					continue;
				}

				message.setText(gson.toJson(ctcJsonModels));
				
				messageProducer.send(message);

				Map<String, Integer> tableSummaryMap = getTableSummaryMap(ctcJsonModels);
				Iterator<String> iterator = tableSummaryMap.keySet().iterator();
				while (iterator.hasNext()) {
					String tableName = iterator.next();
					int statementCount = tableSummaryMap.get(tableName);
					
					final SourceTableConfig stc = new SourceTableConfig();  
					stc.setName(tableName);
					
					IMigrationEventHandler eventsHandler = context.getEventsHandler();
					final ExportRecordsEvent exportRecordsEvent = new ExportRecordsEvent(stc, statementCount);
					eventsHandler.handleEvent(exportRecordsEvent);
				}
				
				ctcJsonModels = null;
				
			} catch (Exception e) {
				e.printStackTrace();
				return;
			} 
		}
		
		for (Table table : selectedTableList) {
			CTCLoader.unregisterTable(ctcHandleId, jobDescriptor, dbUserName, table.getName());
		}

		CTCLoader.stopCapture(ctcHandleId, jobDescriptor, ICTCConstants.Job.CTC_QUIT_JOB_IMMEDIATELY);
		CTCLoader.closeConnection(ctcHandleId);
	}
	
	/**
	 * fetchCaptureTransaction
	 * @param ctcHandle
	 * @param jobDescriptor
	 * @param fetchSize
	 * @return
	 */
	private CTCJsonModel[] fetchCaptureTransaction(int ctcHandle, int jobDescriptor, int fetchSize) {
		int resultDateSize = 0;
		IntByReference _resultDataSize = new IntByReference(resultDateSize);

		String resultBuffer = "";
		Pointer resultBufferPointer = new Memory(fetchSize);
		resultBufferPointer.setString(0, resultBuffer);

		int noDataCount = 0; 

		CTCJsonModel[] ctcJsonModels = null;
		
		// CTC - FETCH
		while (!migrationConfig.isFetchingEnd()) {
			int returnValue = CTCLoader.fetchCapturedTransaction(ctcHandle, jobDescriptor, resultBufferPointer, fetchSize, _resultDataSize);
			if (returnValue == ICTCConstants.CTC_FAILED) {
				break;
			} else {
				String capturedTransactionJson = resultBufferPointer.getString(0).substring(0, _resultDataSize.getValue());
				
				if (returnValue == ICTCConstants.CTC_SUCCESS) {
					CTCJsonModel[] modelsFromJson = gson.fromJson(capturedTransactionJson, CTCJsonModel[].class);
					ctcJsonModels = (CTCJsonModel[]) ArrayUtils.addAll(ctcJsonModels, modelsFromJson);
					
					resultBufferPointer.clear(_resultDataSize.getValue());
					resultBufferPointer = null;
					_resultDataSize = null;
					
					return ctcJsonModels;
				} else if (returnValue == ICTCConstants.CTC_SUCCESS_FRAGMENTED) {
					CTCJsonModel[] modelsFromJson = gson.fromJson(capturedTransactionJson, CTCJsonModel[].class);
					ctcJsonModels = (CTCJsonModel[]) ArrayUtils.addAll(ctcJsonModels, modelsFromJson);
					continue;
				} else if (returnValue == ICTCConstants.CTC_SUCCESS_NO_DATA) {
					if (migrationConfig.isFetchingEnd() == true) {
						break;
					}
					
					noDataCount++;
					
					if (noDataCount == 2) {
						noDataCount = 0; // init
						ThreadUtils.threadSleep(1000, null);
					}
					
					continue;
				}
			}
		}
		
		return null;
	}
}