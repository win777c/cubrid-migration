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
import javax.jms.Topic;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;

import com.cubrid.cubridmigration.core.common.DBUtils;
import com.cubrid.cubridmigration.core.ctc.CTCLoader;
import com.cubrid.cubridmigration.core.ctc.ICTCConstants;
import com.cubrid.cubridmigration.core.ctc.model.CTCJsonModel;
import com.cubrid.cubridmigration.core.dbobject.Table;
import com.cubrid.cubridmigration.core.engine.IMigrationEventHandler;
import com.cubrid.cubridmigration.core.engine.JDBCConManager;
import com.cubrid.cubridmigration.core.engine.MigrationContext;
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

	private JDBCConManager            connManager;
	
	public static boolean isFetchingEnd = false;

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
	 * createStatistics
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
		
		// insert table values
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
		return "";
	}
	
	/**
	 * createDeleteDML
	 * @param transactionModel
	 * @return
	 */
	private String createDeleteDML(CTCJsonModel transactionModel) {
		return "";
	}
	
	/**
	 * Example
	 * 
	 * 	 CREATE TABLE a_tbl1(
     *       id INT UNIQUE,
     *       name VARCHAR,
     *       phone VARCHAR DEFAULT '000-0000'
     *   );
     *   
     *   --insert default values with DEFAULT keyword before VALUES
     *   INSERT INTO a_tbl1 DEFAULT VALUES;
     *   
     *   --insert multiple rows
     *   INSERT INTO a_tbl1 VALUES (1,'aaa', DEFAULT),(2,'bbb', DEFAULT);
     *   
     *   --insert a single row specifying column values for all
     *   INSERT INTO a_tbl1 VALUES (3,'ccc', '333-3333');
     *   
     *   --insert two rows specifying column values for only
     *   INSERT INTO a_tbl1(id) VALUES (4), (5);
     *   
     *   --insert a single row with SET clauses
     *   INSERT INTO a_tbl1 SET id=6, name='eee';
     *   INSERT INTO a_tbl1 SET id=7, phone='777-7777';
     *   
     *   SELECT * FROM a_tbl1;
	 **/
	private String createInsertDML(CTCJsonModel transactionModel) {
		StringBuffer sb = new StringBuffer();
		
		Map<String, String> columns = transactionModel.getColumns();
	    
	    sb.append(transactionModel.getStatementType().toUpperCase())
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
		for (String s : columns.values()) {
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
		
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		return session;
	}
	
	/**
	 * createDestination
	 * @param session
	 * @param topicName
	 * @return
	 * @throws JMSException
	 */
	private Topic createTopic(Session session, String topicName) throws JMSException {
		return session.createTopic(topicName);
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
		int jobDescriptor = CTCLoader.addJobDescriptor(ctcHandleId);
		
		for (Table table : selectedTableList) {
			CTCLoader.registerTable(ctcHandleId, jobDescriptor, dbUserName, table.getName());
		}
		
		CTCLoader.startCapture(ctcHandleId, jobDescriptor);
		
		while (true) {
			try {
				// FETCH CAPTURED TRANSACTION
				String json = fetchCaptureTransaction(ctcHandleId, jobDescriptor, FETCH_SIZE);

				if ("".equals(json)) {
					continue;
				}

				message.setText(json);
				
				messageProducer.send(message);

				CTCJsonModel[] ctcJsonModels = gson.fromJson(json, CTCJsonModel[].class);
				
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
			} catch (Exception e) {
				e.printStackTrace();
				isFetchingEnd = true;
				CTCLoader.stopCapture(ctcHandleId, jobDescriptor, ICTCConstants.Job.CTC_QUIT_JOB_IMMEDIATELY);
				CTCLoader.closeConnection(ctcHandleId);
				return;
			} 
		}
	}
	
	/**
	 * fetchCaptureTransaction
	 * @param ctcHandle
	 * @param jobDescriptor
	 * @param fetchSize
	 * @return
	 */
	private String fetchCaptureTransaction(int ctcHandle, int jobDescriptor, int fetchSize) {
		Integer resultDateSize = new Integer(0);
		IntByReference _resultDataSize = new IntByReference(resultDateSize);

		String resultBuffer = "";
		Pointer resultBufferPointer = new Memory(fetchSize);
		resultBufferPointer.setString(0, resultBuffer);

		StringBuffer sb = new StringBuffer();

		// CTC - FETCH
		while (true) {
			int returnValue = CTCLoader.fetchCapturedTransaction(ctcHandle, jobDescriptor, resultBufferPointer, fetchSize, _resultDataSize);
			if (returnValue == ICTCConstants.CTC_FAILED) {
				break;
			} else {
				
				String capturedTransactionJson = resultBufferPointer.getString(0).substring(0, _resultDataSize.getValue());
				
				if (returnValue == ICTCConstants.CTC_SUCCESS) {
					sb.append(capturedTransactionJson);
					resultBufferPointer.clear(_resultDataSize.getValue());
					return sb.toString();
				} else if (returnValue == ICTCConstants.CTC_SUCCESS_FRAGMENTED) {
					sb.append(capturedTransactionJson);
					continue;
				} else if (returnValue == ICTCConstants.CTC_SUCCESS_NO_DATA) {
					if (isFetchingEnd == true) {
						break;
					}
					continue;
				}
			}
		}
		
		return "";
	}
}