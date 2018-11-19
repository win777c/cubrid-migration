package com.cubrid.cubridmigration.core.ctc.model;

/*
 ************************************* 
 * JSON Example                      *
 *************************************
 *                                   *
 * {                                 *
 *      "Transaction ID": "12",      *
 *      "Statement type": "insert",  *
 *      "User": "dba",               *
 *      "Table": "tbl_01",           *
 *      "Columns": {                 *
 *          "C1": "1",               *
 *          "C2": "Man",             *
 *          "C3": "SEOUL",           *
 *          "C4": "010-0000-0000"    *
 *  	}                            *
 * }                                 *
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

	@SerializedName("User")
	private String              userName         = "";

	@SerializedName("Table")
	private String              tableName        = "";

	@SerializedName("Columns")
	private Map<String, String> columns          = new HashMap<String, String>();

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
	 * getUser
	 * @return
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * setUser
	 * @param userName
	 */
	public void setUserName(String userName) {
		this.userName = userName;
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
	public Map<String, String> getColumns() {
		return columns;
	}

	/**
	 * setColumns
	 * @param columns
	 */
	public void setColumns(Map<String, String> columns) {
		this.columns = columns;
	}
	
	@Override
	public String toString() {
		return "CTCJsonModel [" +
				"transactionId=" + transactionId 
				+ ", statementType=" + statementType 
				+ ", user=" + userName 
				+ ", tableName=" + tableName 
				+ ", columns=" + columns 
				+ "]";
	}
}
