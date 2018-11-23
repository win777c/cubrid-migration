package com.cubrid.cubridmigration.core.ctc;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * CTCLoader
 * 
 * @author win
 */
public final class CTCLoader {

	private CTCLoader() {}

	/**
	 * checkServerStatus
	 * 
	 * @param ctcHandleId
	 * @param serverStatus
	 * @return
	 */
	public static int checkServerStatus(int ctcHandleId, Integer serverStatus) {
		IntByReference _serverStatus = new IntByReference(serverStatus);
		return CTCLibrary.INSTANCE.ctc_check_server_status(ctcHandleId, _serverStatus);
	}

	/**
	 * checkJobStatus
	 * 
	 * @param ctcHandleId
	 * @param jobDescriptor
	 * @param jobStatus
	 * @return
	 */
	public static int checkJobStatus(int ctcHandleId, int jobDescriptor, Integer jobStatus) {
		IntByReference _jobStatus = new IntByReference(jobStatus);
		return CTCLibrary.INSTANCE.ctc_check_job_status(ctcHandleId, jobDescriptor, _jobStatus);
	}

	/**
	 * connectToServer 
	 * 
	 * Example Format for URL - ctc:cubrid:<host>:<port>
	 * 
	 * @param connectionType
	 * @param url
	 * @param ctcHandleId
	 * @return
	 */
	public static int openConnection(int connectionType, String url) { 
		return CTCLibrary.INSTANCE.ctc_open_connection(connectionType, url);
	}

	/**
	 * disconnectToServer
	 * 
	 * @param ctcHandleId
	 * @return
	 */
	public static int closeConnection(int ctcHandleId) {
		return CTCLibrary.INSTANCE.ctc_close_connection(ctcHandleId);
	}

	/**
	 * addJob
	 * 
	 * @param ctcHandleId
	 * @return
	 */
	public static int addJob(int ctcHandleId) {
		return CTCLibrary.INSTANCE.ctc_add_job(ctcHandleId);
	}

	/**
	 * deleteJob
	 * 
	 * @param ctcHandleId
	 * @param jobDescriptor
	 * @return
	 */
	public static int deleteJob(int ctcHandleId, int jobDescriptor) {
		return CTCLibrary.INSTANCE.ctc_delete_job(ctcHandleId, jobDescriptor);
	}

	/**
	 * registerTable
	 * 
	 * @param ctcHandleId
	 * @param jobDescriptor
	 * @param dbUser
	 * @param tableName
	 * @return
	 */
	public static int registerTable(int ctcHandleId, int jobDescriptor, String dbUser, String tableName) {
		return CTCLibrary.INSTANCE.ctc_register_table(ctcHandleId, jobDescriptor, dbUser, tableName);
	}

	/**
	 * unregisterTable
	 * 
	 * @param ctcHandleId
	 * @param jobDescriptor
	 * @param dbUser
	 * @param tableName
	 * @return
	 */
	public static int unregisterTable(int ctcHandleId, int jobDescriptor, String dbUser, String tableName) {
		return CTCLibrary.INSTANCE.ctc_unregister_table(ctcHandleId, jobDescriptor, dbUser, tableName);
	}
	
	/**
	 * fetchCapturedTransaction
	 * 
	 * @param ctcHandleId
	 * @param jobDescriptor
	 * @param resultBuffer
	 * @param resultBufferSize
	 * @param resultDataSize
	 * @return
	 */
	public static int fetchCapturedTransaction(int ctcHandleId, int jobDescriptor, Pointer resultBuffer, int resultBufferSize, IntByReference resultDataSize) {
		return CTCLibrary.INSTANCE.ctc_fetch_capture_transaction(ctcHandleId, jobDescriptor, resultBuffer, resultBufferSize, resultDataSize);
	}

	/**
	 * startTransactionCapture
	 * 
	 * @param ctcHandleId
	 * @param jobDescriptor
	 * @return
	 */
	public static int startCapture(int ctcHandleId, int jobDescriptor) {
		return CTCLibrary.INSTANCE.ctc_start_capture(ctcHandleId, jobDescriptor);
	}

	/**
	 * stopTransactionCapture
	 * 
	 * @param ctcHandleId
	 * @param jobDescriptor
	 * @param closeCondition
	 * @return
	 */
	public static int stopCapture(int ctcHandleId, int jobDescriptor, int closeCondition) {
		return CTCLibrary.INSTANCE.ctc_stop_capture(ctcHandleId, jobDescriptor, closeCondition);
	}
}

/**
 * CTCLibrary
 * 
 * @author win
 */
interface CTCLibrary extends Library {

	public static final String	SHARED_OBJECT_CTC_API_FILENAME = "libctcapi.so.1.0.0";
	public static final String	SHARED_OBJECT_JANSSON_FILENAME = "libjansson.so.4.11.0";
	
	public static CTCLibrary	INSTANCE = (CTCLibrary) Native.loadLibrary(SHARED_OBJECT_CTC_API_FILENAME, CTCLibrary.class);
	public static CTCLibrary	LIB_INSTANCE = (CTCLibrary) Native.loadLibrary(SHARED_OBJECT_JANSSON_FILENAME, CTCLibrary.class);
	
	/**
	 * ctc_open_connection
	 * 
	 * @param connection_type
	 * @param connection_string
	 * @return
	 */
	int ctc_open_connection(int connection_type, String connection_string);

	/**
	 * ctc_close_connection
	 * 
	 * @param ctc_handle
	 * @return
	 */
	int ctc_close_connection(int ctc_handle);

	/**
	 * check_server_status
	 * 
	 * @param ctc_handle
	 * @param server_status
	 * @return
	 */
	int ctc_check_server_status(int ctc_handle, IntByReference server_status);

	/**
	 * ctc_check_job_status
	 * 
	 * @param ctc_handle
	 * @param job_descriptor
	 * @param job_status
	 * @return
	 */
	int ctc_check_job_status(int ctc_handle, int job_descriptor, IntByReference job_status);

	/**
	 * add_job
	 * 
	 * @param ctc_handle_id
	 * @return
	 */
	int ctc_add_job(int ctc_handle_id);

	/**
	 * ctc_delete_job
	 * 
	 * @param ctc_handle_id
	 * @param job_handle_id
	 * @return
	 */
	int ctc_delete_job(int ctc_handle_id, int job_handle_id);

	/**
	 * ctc_register_table
	 * 
	 * @param ctc_handle
	 * @param job_descriptor
	 * @param db_user_name
	 * @param table_name
	 * @return
	 */
	int ctc_register_table(int ctc_handle, int job_descriptor, String db_user_name, String table_name);

	/**
	 * ctc_unregister_table
	 * 
	 * @param ctc_handle
	 * @param job_descriptor
	 * @param db_user_name
	 * @param table_name
	 * @return
	 */
	int ctc_unregister_table(int ctc_handle, int job_descriptor, String db_user_name, String table_name);

	/**
	 * ctc_start_capture
	 * 
	 * @param ctc_handle
	 * @param job_descriptor
	 * @return
	 */
	int ctc_start_capture(int ctc_handle, int job_descriptor);

	/**
	 * ctc_stop_capture
	 * 
	 * @param ctc_handle
	 * @param job_descriptor
	 * @param close_condition
	 * @return
	 */
	int ctc_stop_capture(int ctc_handle, int job_descriptor, int close_condition);

	/**
	 * ctc_fetch_capture_transaction
	 * 
	 * @param ctc_handle
	 * @param job_descriptor
	 * @param result_buffer
	 * @param result_buffer_size
	 * @param result_date_size
	 * @return
	 */
	int ctc_fetch_capture_transaction(int ctc_handle, int job_descriptor, Pointer result_buffer, int result_buffer_size, IntByReference result_date_size);
}
