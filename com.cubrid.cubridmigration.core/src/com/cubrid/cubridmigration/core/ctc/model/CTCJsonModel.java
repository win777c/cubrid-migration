package com.cubrid.cubridmigration.core.ctc.model;

/*
 ************************************* 
 * JSON Example                      *
 *************************************
 *                                   *
 * [{                                *
 *      "Transaction ID": 777,       *
 *      "Table": "tbl1",             *
 *      "Statement type": "insert",  *
 *      "Columns": {                 *
 *          "c1": 1,                 *
 *          "c2": "UNISQL"           *
 *      }                            *
 *  },                               *
 *  {                                *
 *      "Transaction ID": 777,       *
 *      "Table": "tbl1",             *
 *      "Statement type": "update",  *
 *      "Columns": {                 *
 *        "c1": 2,                   *
 *        "c2": "CUBRID"             *
 *      },                           *
 *      "key columns": {             *
 *          "c1": 1                  *
 *      }                            *
 *  },                               *
 *  {                                *
 *      "Transaction ID": 777,       *
 *      "Table": "tbl1",             *
 *      "Statement type": "delete",  *
 *      "key columns": {             *
 *          "c1": 1                  *
 *      }                            *
 * 	}                                *
 * ]                                 *
 *                                   *
 ************************************* 
 */

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class CTCJsonModel implements Serializable {

	private static final long   serialVersionUID = 5161230132071083186L;

	@SerializedName("Transaction ID")
	private String              transactionId    = "";

	@SerializedName("Statement type")
	private String              statementType    = "";

	@SerializedName("Table")
	private String              tableName        = "";

	@SerializedName("Columns")
	private Map<String, Object> columns          = new HashMap<String, Object>();

	@SerializedName("key columns")
	private Map<String, Object> keyColumns       = new HashMap<String, Object>();
	
	/**
	 * getTransactionId
	 * @return
	 */
	public String getTransactionId() {
		return transactionId;
	}

	/**
	 * setTransactionId
	 * @param transactionId
	 */
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	/**
	 * getStatementType
	 * @return
	 */
	public String getStatementType() {
		return statementType;
	}

	/**
	 * setStatementType
	 * @param statementType
	 */
	public void setStatementType(String statementType) {
		this.statementType = statementType;
	}

	/**
	 * getTableName
	 * @return
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * setTableName
	 * @param tableName
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * getColumns
	 * @return
	 */
	public Map<String, Object> getColumns() {
		return columns;
	}

	/**
	 * setColumns
	 * @param columns
	 */
	public void setColumns(Map<String, Object> columns) {
		this.columns = columns;
	}
	
	/**
	 * getKeyColumns
	 * @return
	 */
	public Map<String, Object> getKeyColumns() {
    	return keyColumns;
    }

	/**
	 * setKeyColumns
	 * @param keyColumns
	 */
	public void setKeyColumns(Map<String, Object> keyColumns) {
    	this.keyColumns = keyColumns;
    }

	@Override
	public String toString() {
		return "CTCJsonModel [" +
				"transactionId=" + transactionId 
				+ ", statementType=" + statementType 
				+ ", tableName=" + tableName 
				+ ", columns=" + columns
				+ ", keyColumns=" + keyColumns
				+ "]";
	}
}