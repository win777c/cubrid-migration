package com.cubrid.cubridmigration.core.ctc;

public interface ICTCConstants {
	public static final int CTC_FAILED             = -1;
    public static final int CTC_SUCCESS            = 0;
    public static final int CTC_SUCCESS_FRAGMENTED = 1;
    public static final int CTC_SUCCESS_NO_DATA    = 2;

    /* CTC_CONNECTION_TYPE */
    interface Connection {
        public static final int CTC_CONN_TYPE_DEFAULT   = 0;
        public static final int CTC_CONN_TYPE_CTRL_ONLY = 1;
    }

    /* CTC_CHECK_SERVER_STATUS */
    interface Server {
        public static final int CTC_SERVER_NOT_READY = 0; // CTC서버가 job을 처리 가능한 상태가 아닌 경우
        public static final int CTC_SERVER_RUNNING   = 1; // CTC서버가 정상적으로 동작중인 경우의 상태
        public static final int CTC_SERVER_CLOSING   = 2; // CTC서버 프로세스를 종료중인 경우의 상태
    }
    
    /* CTC_CHECK_JOB_STATUS */
    interface Job {
        public static final int CTC_QUIT_JOB_IMMEDIATELY       = 0; // (default) 해당 job을 바로 종료한다
        public static final int CTC_QUIT_JOB_AFTER_TRANSACTION = 1; // CTC서버에서 어플리케이션으로 전송 대기중인 captured 트랜잭션을 모두 전송하고 job을 종료한다
        
        public static final int CTC_JOB_NONE                   = 0; // job description에 해당하는 job이 존재하지 않는 상태
        public static final int CTC_JOB_WAITING                = 1; // job에 수행되고 있는 트랜잭션이 존재하지 않는 상태
        public static final int CTC_JOB_PROCESSING             = 2; // job에 트랜잭션이 수행되고 있는 상태
        public static final int CTC_JOB_READY_TO_FETCH         = 3; // 트랜잭션 수행 후 fetch할 결과물이 대기중인 상태
        public static final int CTC_JOB_CLOSING                = 4; // job description에 해당하는 job을 closing하고 있는 상태
    }
}
