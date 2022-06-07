package com.softtech;

import com.softtech.smpp.InvalidResponseException;
import com.softtech.smpp.PDUException;
import com.softtech.smpp.SMPPConstant;
import com.softtech.smpp.bean.*;
import com.softtech.smpp.extra.NegativeResponseException;
import com.softtech.smpp.extra.ProcessRequestException;
import com.softtech.smpp.extra.ResponseTimeoutException;
import com.softtech.smpp.extra.SessionState;
import com.softtech.smpp.session.*;
import com.softtech.smpp.util.AbsoluteTimeFormatter;
import com.softtech.smpp.util.DeliveryReceiptState;
import com.softtech.smpp.util.InvalidDeliveryReceiptException;
import com.softtech.smpp.util.ValidityPeriodFormatter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author SUTHAR
 */
public class SendSMS_BKP extends Thread {

    private String SMPP = "";
    private String SMPP_IP_ADDRESS = "";
    //private final static String SMPP_IP_ADDRESS = "203.145.131.84";
    private int SMPP_PORT = 8888;
    private String SMPP_USER_ID = "";
    private String SMPP_PASSWORD = "";
    private String SMPP_MODE = "TRX";
    private static int ROUTE_ID = 4 ;

    private int SMPP_TPS = 5;
    private int SESSION = 0;
    private int SELECT_LIMIT = 10;
    private int RETRY_ATTEMPT = 1;
    private int VALIDITY_PERIOD = 0;
    private final long RECONNECT_INTERVAL = 10000L; // 5 seconds
    private final Long SMPP_TRANSACTIONTIMER = 10000L; // 10 seconds
    private final int SMPP_ENQUIRELINKTIMEOUT = 10000; // 10 seconds
    private final String WRITE_LOG = "Y";

    private int MAX_MULTIPART_MSG_SEGMENT_SIZE_UCS2 = 64;
    private int MAX_SINGLE_MSG_SEGMENT_SIZE_UCS2 = 70;
    private int MAX_MULTIPART_MSG_SEGMENT_SIZE_7BIT = 154;
    private int MAX_SINGLE_MSG_SEGMENT_SIZE_7BIT = 160;

    private final String ERROR_LOG_PATH = "/opt/SMPP/logs/";
    private final String STATUS_LOG_PATH = "/opt/SMPP/logs/";

    private final SetBlockingQueue sms_queue = new SetBlockingQueue();
    private final SetBlockingQueueDlr sms_dlr = new SetBlockingQueueDlr();
    List<SMS> SmsDrainList = new ArrayList<>();
    List<DLR> DlrDrainList = new ArrayList<>();

    private AtomicInteger requestCounter = new AtomicInteger();
    private AtomicInteger responseCounter = new AtomicInteger();
    private AtomicLong totalRequestCounter = new AtomicLong();
    private AtomicLong totalResponseCounter = new AtomicLong();
    private AtomicLong maxDelay = new AtomicLong();

    private final AbsoluteTimeFormatter absoluteTimeFormatter = new AbsoluteTimeFormatter();
    private final ValidityPeriodFormatter validityPeriodFormatter = new ValidityPeriodFormatter();

    static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
    static SimpleDateFormat stringRelativeTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static SimpleDateFormat TimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");

    private boolean lb_run = false;
    private Connection conn = null;

    private boolean[] threadIsAlive;
    private SMSSession[] smsSession;
    private DeliveryReceiptThread[] DlrSession;
    private BindType bindType = BindType.BIND_TRX;

    public SendSMS_BKP(int route_id) {
        ROUTE_ID = route_id;
        System.out.print(TimeFormatter.format(new Date()) + " --:  Initialising...\n");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        stringRelativeTimeFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public SendSMS_BKP() {
        System.out.print(TimeFormatter.format(new Date()) + " --:  Initialising...\n");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        stringRelativeTimeFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public void run() {
        String ls_sql = "";
        ResultSet rs = null;
        Statement sql_stmt = null;
        Connection con = null;

        while (!dbconfig.isDbConnected(con)) {
            try {
                con = dbconfig.GetConncetion();
            } catch (Exception e) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                }
            }
        }

        try {
            sql_stmt = con.createStatement();
            ls_sql = "SELECT ROUTE_NM,USER_NM,PASSWORD,MODE,TPS,SESSION,IP_ADDRESS,"
                    + "PORT,RETRY_ATTEMPT,ACTIVE,RUN_STATUS,SELECT_LIMIT,VALIDITY_PERIOD,"
                    + "7BIT_SINGLE_MSG_SIZE,7BIT_MULTIPART_MSG_SIZE,UCS2_SINGLE_MSG_SIZE,UCS2_MULTIPART_MSG_SIZE "
                    + " FROM sms_route_mst WHERE ACTIVE ='Y' AND ID =" + ROUTE_ID;
            rs = sql_stmt.executeQuery(ls_sql);
            while (rs.next()) {
                this.SESSION = rs.getInt("SESSION");
                this.SMPP_TPS = rs.getInt("TPS");
                this.SMPP_PORT = rs.getInt("PORT");
                this.SMPP = rs.getString("ROUTE_NM").trim();
                this.SMPP_MODE = rs.getString("MODE").trim();
                this.SMPP_USER_ID = rs.getString("USER_NM").trim();
                this.SMPP_PASSWORD = rs.getString("PASSWORD").trim();
                this.SMPP_IP_ADDRESS = rs.getString("IP_ADDRESS").trim();
                this.RETRY_ATTEMPT = rs.getInt("RETRY_ATTEMPT");
                this.SELECT_LIMIT = rs.getInt("SELECT_LIMIT");
                this.VALIDITY_PERIOD = rs.getInt("VALIDITY_PERIOD");

                this.MAX_SINGLE_MSG_SEGMENT_SIZE_7BIT = rs.getInt("7BIT_SINGLE_MSG_SIZE");
                this.MAX_MULTIPART_MSG_SEGMENT_SIZE_7BIT = rs.getInt("7BIT_MULTIPART_MSG_SIZE");
                this.MAX_SINGLE_MSG_SEGMENT_SIZE_UCS2 = rs.getInt("UCS2_SINGLE_MSG_SIZE");
                this.MAX_MULTIPART_MSG_SEGMENT_SIZE_UCS2 = rs.getInt("UCS2_MULTIPART_MSG_SIZE");

                lb_run = true;

                if (this.RETRY_ATTEMPT <= 0) {
                    this.RETRY_ATTEMPT = 1;
                }

                if (this.SELECT_LIMIT <= 0) {
                    this.SELECT_LIMIT = 1;
                }
                this.SMPP_TPS = Math.round(this.SMPP_TPS / this.SESSION);
                if (this.SMPP_TPS <= 0) {
                    this.SMPP_TPS = 10;
                }

                switch (this.SMPP_MODE) {
                    case "TX":
                        this.bindType = BindType.BIND_TX;
                        break;
                    case "RX":
                        this.bindType = BindType.BIND_RX;
                        break;
                    default:
                        this.bindType = BindType.BIND_TRX;
                        break;
                }
            }
            rs.close();
            sql_stmt.close();
            con.close();
        } catch (SQLException ex) {
            System.out.print(TimeFormatter.format(new Date()) + " --:  SQLException " + ex.getMessage() + "\n");
            dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "SQLException " + ex.getMessage());
        }

        System.out.print(TimeFormatter.format(new Date()) + " --:  ===========================================\n");
        System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP_IP_ADDRESS: " + SMPP_IP_ADDRESS + "\n");
        System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP_PORT: " + SMPP_PORT + "\n");
        System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP_USER_ID: " + SMPP_USER_ID + "\n");
        System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP_PASSWORD: " + SMPP_PASSWORD + "\n");
        System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP_MODE: " + SMPP_MODE + "\n");
        System.out.print(TimeFormatter.format(new Date()) + " --:  ROUTE_ID: " + ROUTE_ID + "\n");
        System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP_TPS: " + SMPP_TPS + "\n");
        System.out.print(TimeFormatter.format(new Date()) + " --:  RETRY_ATTEMPT: " + RETRY_ATTEMPT + "\n");
        System.out.print(TimeFormatter.format(new Date()) + " --:  ===========================================\n");

        smsSession = new SMSSession[this.SESSION];
        threadIsAlive = new boolean[this.SESSION];
        DlrSession = new DeliveryReceiptThread[1];

        //Start all session
        for (int i = 0; i < smsSession.length; i++) {
            smsSession[i] = new SMSSession(SMPP_IP_ADDRESS, SMPP_PORT, SMPP_USER_ID, SMPP_PASSWORD, i + 1, ROUTE_ID);
            smsSession[i].start();
            threadIsAlive[i] = true;
        }

        SetSMSRouteStatus(ROUTE_ID, "Y");
        System.out.print(TimeFormatter.format(new Date()) + " --:  Initialising complate\n");
        
        //SMS Send Process
        if (this.bindType.IsTransmittable()&& !this.bindType.IsReceivable()) {
            // run TrafficWatcher
            new TrafficWatcherThread().start();
            this.sendProcess();
        } else if (!this.bindType.IsTransmittable() && this.bindType.IsReceivable()) {
            for (int i = 0; i < DlrSession.length; i++) {
                DlrSession[i] = new DeliveryReceiptThread();
                DlrSession[i].start();
            }
            while (lb_run) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            }
        } else if (this.bindType.IsTransmittable() && this.bindType.IsReceivable()) {
            new TrafficWatcherThread().start();
            for (int i = 0; i < DlrSession.length; i++) {
                DlrSession[i] = new DeliveryReceiptThread();
                DlrSession[i].start();
            }
            this.sendProcess();
        }
        
        sms_dlr.drainTo(DlrDrainList);
        for (DLR dlr : DlrDrainList) {
            System.out.print(TimeFormatter.format(new Date()) + " --:  DlrDrainList " + dlr.getMESSAGE_ID() + "\n");
            InsertDeliverSm(dlr.getMOBILE_NO(), dlr.getSENDER_ID(), dlr.getMESSAGE_ID(), dlr.getSUB(), dlr.getDELIVER(), dlr.getSUBMIT_DT(), dlr.getDONE_DT(), dlr.getDELIVERY_STATUS(), dlr.getERROR(), dlr.getTEXT());
        }

        sms_queue.drainTo(SmsDrainList);
        for (SMS sms : SmsDrainList) {
            System.out.print(TimeFormatter.format(new Date()) + " --:  SmsDrainList " + sms.getTran_cd() + "\n");
            SetMessagePending(sms.getTran_cd(), sms.getTrntype());
        }

        for (int i = 0; i < smsSession.length; i++) {
            if (threadIsAlive[i] && smsSession[i].isAlive()) {
                smsSession[i].unbindAndClose();
                threadIsAlive[i] = false;
            }
        }
        System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP ALL SESSION DEAD\n");
        dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "SMPP STOP");
        SetSMSRouteStatus(ROUTE_ID, "N");
        System.exit(0);
    }

    private void sendProcess() {
        ResultSet rs = null;
        Statement sql_stmt = null;
        String ls_sql = null;
        boolean lb_row_found;

        ls_sql = "SELECT SELLER_ID,USER_ID,SENDER_ID,TRN_TYPE, COUNT(NO_OF_SMS) AS NO_OF_SMS FROM sms_trn "
                + "WHERE SMS_STATUS = 'P' AND SCHEDULER_DT <= SYSDATE() AND ROUTE_ID = " + ROUTE_ID
                + " GROUP BY SELLER_ID,USER_ID,SENDER_ID,TRN_TYPE ORDER BY NO_OF_SMS ASC,TRN_TYPE ASC";

        while (lb_run) {
            lb_row_found = false;
            try {
                if (!dbconfig.isDbConnected(conn)) {
                    try {
                        conn = dbconfig.GetConncetion();
                    } catch (Exception ex) {
                        System.out.print(TimeFormatter.format(new Date()) + " --:  CreateStatement Exception " + ex.getMessage() + "\n");
                        dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "CreateStatement Exception " + ex.getMessage());
                    }
                } else {
                    try {
                        sql_stmt = conn.createStatement();
                        rs = sql_stmt.executeQuery(ls_sql);

                        while (rs.next()) { //Send SMS
                            lb_row_found = true;
                            SendSms(rs.getString("SELLER_ID"), rs.getString("USER_ID"), rs.getString("SENDER_ID"), rs.getString("TRN_TYPE"));
                        }
                        rs.close();
                        sql_stmt.close();
                    } catch (SQLException ex) {
                        System.out.print(TimeFormatter.format(new Date()) + " --:  SendProcess SQLException " + ex.getMessage() + "\n");
                        dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, " SendProcess SQLException " + ex.getMessage());
                    } catch (Exception ex) {
                        System.out.print(TimeFormatter.format(new Date()) + " --:  SendProcess Exception " + ex.getMessage() + "\n");
                        dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, " SendProcess Exception " + ex.getMessage());
                    }finally{                        
                        rs = null;
                        sql_stmt = null;
                    }
                    if (!lb_row_found) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.print(TimeFormatter.format(new Date()) + " --:  Exception " + ex.getMessage() + "\n");
                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "Exception " + ex.getMessage());
            }
            System.gc();
        }
    }

    private void SendSms(String SELLER_ID, String USER_ID, String SENDER_ID, String TRN_TYPE) {
        String ls_sql_sms = null;
        Statement sms_stmt = null;
        ResultSet rs_sms = null;

        ls_sql_sms = "SELECT TRAN_ID,TRAN_DT,SENDER_ID,MOBILE_NO,MESSAGE,UNICODE,MESSAGE_TYPE,SCHEDULER,SCHEDULER_DT,SEND_PART,REF_NO,NO_OF_SMS FROM sms_trn "
                + "WHERE SELLER_ID = '" + SELLER_ID + "' AND USER_ID = '" + USER_ID + "' AND SENDER_ID = '" + SENDER_ID + "' AND SMS_STATUS = 'P' "
                + " AND TRN_TYPE ='" + TRN_TYPE + "' AND SCHEDULER_DT <= SYSDATE() "
                + " AND ROUTE_ID = " + ROUTE_ID + " ORDER BY TRAN_DT ASC LIMIT " + SELECT_LIMIT;

        try {
            sms_stmt = conn.createStatement();
            rs_sms = sms_stmt.executeQuery(ls_sql_sms);

            while (rs_sms.next()) {
                /*sms_queue.add(new SMS(rs_sms.getLong("TRAN_ID"),
                        rs_sms.getString("TRAN_DT"),
                        rs_sms.getString("SENDER_ID").trim(),
                        rs_sms.getString("MOBILE_NO").trim(),
                        rs_sms.getString("MESSAGE").trim(),
                        rs_sms.getString("UNICODE").trim().equals("Y"),
                        rs_sms.getString("MESSAGE_TYPE").trim().equals("F"),
                        rs_sms.getString("SCHEDULER").trim().equals("Y"),
                        rs_sms.getString("SCHEDULER_DT"),
                        rs_sms.getInt("SEND_PART"),
                        rs_sms.getInt("REF_NO"),
                        rs_sms.getInt("NO_OF_SMS"),
                        TRN_TYPE,""));*/
            }
            rs_sms.close();
            sms_stmt.close();
        } catch (SQLException ex) {
            System.out.print(TimeFormatter.format(new Date()) + " --:  SEND SendSms SQLException " + ex.getMessage() + "\n");
            dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "SEND SendSms SQLException " + ex.getMessage());
        } catch (Exception ex) {
            System.out.print(TimeFormatter.format(new Date()) + " --:  SEND SendSms Exception " + ex.getMessage() + "\n");
            dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "SEND SendSms Exception " + ex.getMessage());
        }finally{                        
            rs_sms = null;
            sms_stmt = null;
            ls_sql_sms = null;
        }
        System.gc();
    }

    private void SetSMSRouteStatus(int ROUTE_ID, String as_status) {
        String sql = null;
        Statement stmt = null;
        Connection con = null;

        try {
            con = dbconfig.GetConncetion();
            stmt = con.createStatement();
            sql = "UPDATE sms_route_mst SET RUN_STATUS = '" + as_status + "' WHERE ID=" + ROUTE_ID;
            int rows = stmt.executeUpdate(sql);
            stmt.close();
            con.close();
        } catch (SQLException ex) {
            System.out.print(TimeFormatter.format(new Date()) + " --:  SetSMSRouteStatus SQLException " + ex.getMessage() + "\n");
            dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "UPADATE RUN_STATUS " + ex.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                    stmt = null;
                } catch (SQLException ex) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                    con = null;
                } catch (SQLException ex) {
                }
            }
        }
        System.gc();
    }

    private void SetMessagePending(long al_tran_cd, String as_trn_type) {
        Connection con = null;
        Statement upd_stmt = null;
        try {
            con = dbconfig.GetConncetion();
            upd_stmt = con.createStatement();
            String sql = "UPDATE sms_trn SET SMS_STATUS = 'P' WHERE TRAN_ID=" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
            int rows = upd_stmt.executeUpdate(sql);
            upd_stmt.close();
            con.close();
        } catch (SQLException ex) {
            dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "SetMessagePending SQLException " + ex.getMessage());
        } finally {
            if (upd_stmt != null) {
                try {
                    upd_stmt.close();
                    upd_stmt = null;
                } catch (SQLException ex) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                    con = null;
                } catch (SQLException ex) {
                }
            }
        }
        System.gc();
    }

    private void GetSetting() {
        Connection db_conn = null;
        Statement sql_stmt = null;
        ResultSet rs = null;
        int tps;
        try {
            db_conn = dbconfig.GetConncetion();
            sql_stmt = db_conn.createStatement();
            String ls_sql = "SELECT PORT,USER_NM,PASSWORD,IP_ADDRESS,RETRY_ATTEMPT,TPS,ACTIVE,"
                    + "SELECT_LIMIT,VALIDITY_PERIOD,7BIT_SINGLE_MSG_SIZE,7BIT_MULTIPART_MSG_SIZE,"
                    + "UCS2_SINGLE_MSG_SIZE,UCS2_MULTIPART_MSG_SIZE "
                    + " FROM sms_route_mst WHERE ID =" + ROUTE_ID;
            rs = sql_stmt.executeQuery(ls_sql);
            while (rs.next()) {
                this.RETRY_ATTEMPT = rs.getInt("RETRY_ATTEMPT");
                tps = rs.getInt("TPS");
                lb_run = rs.getString("ACTIVE").trim().equalsIgnoreCase("Y");
                this.SELECT_LIMIT = rs.getInt("SELECT_LIMIT");
                this.VALIDITY_PERIOD = rs.getInt("VALIDITY_PERIOD");

                this.SMPP_PORT = rs.getInt("PORT");
                this.SMPP_USER_ID = rs.getString("USER_NM").trim();
                this.SMPP_PASSWORD = rs.getString("PASSWORD").trim();
                this.SMPP_IP_ADDRESS = rs.getString("IP_ADDRESS").trim();

                this.MAX_SINGLE_MSG_SEGMENT_SIZE_7BIT = rs.getInt("7BIT_SINGLE_MSG_SIZE");
                this.MAX_MULTIPART_MSG_SEGMENT_SIZE_7BIT = rs.getInt("7BIT_MULTIPART_MSG_SIZE");
                this.MAX_SINGLE_MSG_SEGMENT_SIZE_UCS2 = rs.getInt("UCS2_SINGLE_MSG_SIZE");
                this.MAX_MULTIPART_MSG_SEGMENT_SIZE_UCS2 = rs.getInt("UCS2_MULTIPART_MSG_SIZE");

                if (this.RETRY_ATTEMPT <= 0) {
                    this.RETRY_ATTEMPT = 1;
                }

                if (this.SELECT_LIMIT <= 0) {
                    this.SELECT_LIMIT = 1;
                }
                this.SMPP_TPS = Math.round(tps / this.SESSION);
                if (this.SMPP_TPS <= 0) {
                    this.SMPP_TPS = 10;
                }
            }
            rs.close();
            sql_stmt.close();
            db_conn.close();
        } catch (SQLException ex) {
            dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, "GetSetting", "SQLException " + ex.getMessage());
        } catch (Exception ex) {
            dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, "GetSetting", "Exception " + ex.getMessage());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                    rs = null;
                } catch (SQLException ex) {
                }
            }
            if (sql_stmt != null) {
                try {
                    sql_stmt.close();
                    sql_stmt = null;
                } catch (SQLException ex) {
                }
            }
            if (db_conn != null) {
                try {
                    db_conn.close();
                    db_conn = null;
                } catch (SQLException ex) {
                }
            }
        }
        System.gc();
        dbconfig.WriteStatus(STATUS_LOG_PATH, "SMPP IS RUNNING...");
        //System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP_TPS: " + SMPP_TPS + " RETRY_ATTEMPT: " + RETRY_ATTEMPT + "\n");
    }

    private class ThreadWatcherThread extends Thread {

        public void run() {
            System.out.print(TimeFormatter.format(new Date()) + " --:  Starting ThreadWatcherThread\n");
            while (lb_run) {
                GetSetting();
                for (int i = 0; i < smsSession.length; i++) {
                    if (threadIsAlive[i] && !smsSession[i].isAlive()) {
                        threadIsAlive[i] = false;
                        System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION: " + i + " IS DEAD\n");
                        smsSession[i] = new SMSSession(SMPP_IP_ADDRESS, SMPP_PORT, SMPP_USER_ID, SMPP_PASSWORD, i + 1, ROUTE_ID);
                        smsSession[i].start();
                        System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION: " + i + " START\n");
                        threadIsAlive[i] = true;
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private class TrafficWatcherThread extends Thread {

        @Override
        public void run() {
            if (bindType.IsTransmittable()) {
                System.out.print(TimeFormatter.format(new Date()) + " --:  Starting traffic watcher...\n");
                while (lb_run) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                    int requestPerSecond = requestCounter.getAndSet(0);
                    int responsePerSecond = responseCounter.getAndSet(0);
                    long maxDelayPerSecond = maxDelay.getAndSet(0);
                    totalRequestCounter.addAndGet(requestPerSecond);
                    long total = totalResponseCounter.addAndGet(responsePerSecond);
                    System.out.print(TimeFormatter.format(new Date()) + " --:  Request/Response per 5 second: " + requestPerSecond + "/" + responsePerSecond + " of " + total + " maxDelay=" + maxDelayPerSecond + "\n");
                }
                System.gc();
            }
        }
    }

    private class DeliveryReceiptThread extends Thread {

        @Override
        public void run() {
            String ls_Query = "INSERT INTO sms_response_mst (`MOBILE_NO`,`SENDER_ID`,`MESSAGE_ID`,`SUB`,`DELIVER`,`SUBMIT_DT`,`DONE_DT`,`DELIVERY_STATUS`,`ERROR`,`TEXT`,`ROUTE_ID`) VALUES ";
            String ls_sql = "";
            int count = 0;
            Connection db_conn = null;
            Statement ins_stmt = null;
            long startTime = System.currentTimeMillis();
            if (bindType.IsReceivable()) {
                System.out.print(TimeFormatter.format(new Date()) + " --:  Starting DeliveryReceiptThread\n");
                while (lb_run) {
                    try {
                        DLR dlr = (DLR) sms_dlr.take();
                        long delay = System.currentTimeMillis() - startTime;
                        System.out.print(TimeFormatter.format(new Date()) + " --:  DLR delay " + delay + "\n");
                        if (delay > 10000) {
                            count = 100;
                        }
                        if (count > 99) {
                            startTime = System.currentTimeMillis();
                            ls_sql += "('" + dlr.getMOBILE_NO() + "','" + dlr.getSENDER_ID() + "','" + dlr.getMESSAGE_ID() + "','" + dlr.getSUB() + "','" + dlr.getDELIVER() + "',"
                                    + "'" + dlr.getSUBMIT_DT() + "','" + dlr.getDONE_DT() + "','" + dlr.getDELIVERY_STATUS() + "','" + dlr.getERROR() + "','" + dlr.getTEXT() + "'," + ROUTE_ID + ")";
                            try {
                                db_conn = dbconfig.GetConncetion();
                                ins_stmt = db_conn.createStatement();
                                ins_stmt.executeUpdate(ls_Query + ls_sql);
                                ins_stmt.close();
                                db_conn.close();
                                System.out.print(TimeFormatter.format(new Date()) + " --:  DLR INSERT " + count + "\n");
                            } catch (SQLException ex) {
                                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "SQLException " + ex.getMessage());
                                try {
                                    ins_stmt.close();
                                        ins_stmt = null;
                                } catch (SQLException e) {
                                }
                                try {
                                    db_conn.close();
                                        db_conn = null;
                                } catch (SQLException e) {
                                }
                            } finally {
                                if (ins_stmt != null) {
                                    try {
                                        ins_stmt.close();
                                        ins_stmt = null;
                                    } catch (SQLException ex) {
                                    }
                                }
                                if (db_conn != null) {
                                    try {
                                        db_conn.close();
                                        db_conn = null;
                                    } catch (SQLException ex) {
                                    }
                                }
                            }

                            count = 0;
                            ls_sql = "";
                            System.gc();
                        } else {
                            ls_sql += "('" + dlr.getMOBILE_NO() + "','" + dlr.getSENDER_ID() + "','" + dlr.getMESSAGE_ID() + "','" + dlr.getSUB() + "','" + dlr.getDELIVER() + "',"
                                    + "'" + dlr.getSUBMIT_DT() + "','" + dlr.getDONE_DT() + "','" + dlr.getDELIVERY_STATUS() + "','" + dlr.getERROR() + "','" + dlr.getTEXT() + "'," + ROUTE_ID + "),";
                        }
                        count++;
//                        InsertDeliverSm(dlr.getMOBILE_NO(), dlr.getSENDER_ID(), dlr.getMESSAGE_ID(), dlr.getSUB(), dlr.getDELIVER(), dlr.getSUBMIT_DT(), dlr.getDONE_DT(), dlr.getDELIVERY_STATUS(), dlr.getERROR(), dlr.getTEXT());                        
                    } catch (Exception ex) {
                        dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "Try Exception " + ex.getMessage());
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void InsertDeliverSm(String MOBILE_NO, String SENDER_ID, String MESSAGE_ID, String SUB, String DELIVER, String SUBMIT_DT, String DONE_DT, DeliveryReceiptState DELIVERY_STATUS, String ERROR, String TEXT) {
        Connection db_conn = null;
        Statement ins_stmt = null;
        try {
            String ls_sql = "INSERT INTO sms_response_mst (`MOBILE_NO`,`SENDER_ID`,`MESSAGE_ID`,`SUB`,`DELIVER`,`SUBMIT_DT`,`DONE_DT`,`DELIVERY_STATUS`,`ERROR`,`TEXT`,`ROUTE_ID`) VALUES "
                    + "('" + MOBILE_NO + "','" + SENDER_ID + "','" + MESSAGE_ID + "','" + SUB + "','" + DELIVER + "','" + SUBMIT_DT + "','" + DONE_DT + "','" + DELIVERY_STATUS + "','" + ERROR + "','" + TEXT + "'," + ROUTE_ID + ")";

            db_conn = dbconfig.GetConncetion();
            ins_stmt = db_conn.createStatement();
            ins_stmt.executeUpdate(ls_sql);
            ins_stmt.close();
            db_conn.close();
        } catch (SQLException ex) {
            dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "SQLException " + ex.getMessage());
            try {
                ins_stmt.close();
            } catch (SQLException e) {
            }
            try {
                db_conn.close();
            } catch (SQLException e) {
            }
        } finally {
            if (ins_stmt != null) {
                try {
                    ins_stmt.close();
                } catch (SQLException ex) {
                }
            }
            if (db_conn != null) {
                try {
                    db_conn.close();
                } catch (SQLException ex) {
                }
            }
        }
        System.gc();
    }

    private class SMSSession extends Thread {

        private SMPPSession session = null;
        private BindParameter bindParameter = null;

        private String SMPP_IP_ADDRESS = "";
        private int SMPP_PORT = 0;
        private String SMPP_USER_ID = "";
        private String SMPP_PASSWORD = "";
        private int SESSION_ID = 0;
        private int ROUTE_ID = 1;
        final SimpleDateFormat stringRelativeTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        private ExecutorService executorService = Executors.newFixedThreadPool(SMPP_TPS);
        
        
        public SMSSession(String SMPP_IP_ADDRESS, int SMPP_PORT, String SMPP_USER_ID, String SMPP_PASSWORD, int SESSION_ID, int ROUTE_ID) {
            this.SMPP_IP_ADDRESS = SMPP_IP_ADDRESS;
            this.SMPP_PORT = SMPP_PORT;
            this.SMPP_USER_ID = SMPP_USER_ID;
            this.SMPP_PASSWORD = SMPP_PASSWORD;
            this.SESSION_ID = SESSION_ID;
            this.ROUTE_ID = ROUTE_ID;
        }

        public void run() {
            long counter = 0;
            long lStartTime = 0;
            long delay = 0;
            try {
                this.session = new SMPPSession();
                this.session.setPduProcessorDegree(SMPP_TPS);
                this.session.setTransactionTimer(SMPP_TRANSACTIONTIMER);
                this.session.addSessionStateListener(new SessionStateListenerImpl());
                ((ThreadPoolExecutor) this.executorService).setCorePoolSize(SMPP_TPS);
                ((ThreadPoolExecutor) this.executorService).setMaximumPoolSize(SMPP_TPS);

                if (bindType.IsReceivable()) {
                    this.session.setMessageReceiverListener(new MessageReceiverListenerImpl());
                }
                this.bindParameter = new BindParameter(bindType,
                        this.SMPP_USER_ID, this.SMPP_PASSWORD, "TR", TypeOfNumber.ALPHANUMERIC, NumberingPlanIndicator.UNKNOWN, null);
                this.session.connectAndBind(this.SMPP_IP_ADDRESS, this.SMPP_PORT, this.bindParameter);
            } catch (IOException e) {
                System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " Run Exception " + e.getMessage() + "\n");
                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "Run Exception " + e.getMessage());
            }
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " CONNECT\n");
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " SEND START...\n");
            if (bindType.IsTransmittable()) {
                while (lb_run) {
                    try {
                        SMS sms = (SMS) sms_queue.take();
                        if (this.session != null && this.session.getSessionState().isTransmittable()) {
                            counter = counter + sms.getNo_of_sms();
                            if (counter > SMPP_TPS) {
                                delay = System.currentTimeMillis() - lStartTime;
                                if (delay > 0 && delay < 1000) {
                                    try {
                                        System.out.print(new SimpleDateFormat("dd-MM-yyyy hh:mm:ss.SSS").format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " SYSTEM SLEEP FOR: " + (1000 - delay) + "\n");
                                        dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "SYSTEM SLEEP FOR: " + (1000 - delay));
                                        Thread.sleep((1000 - delay));
                                    } catch (InterruptedException e) {
                                    }
                                }
                                counter = 0;
                                lStartTime = System.currentTimeMillis();
                            }
                            executorService.execute(newSendTask(sms.getTran_cd(), sms.getSource(), sms.getDestination(), sms.getMessage(),
                                    sms.isFlash(), sms.isUnicode(), sms.isScheduler(), sms.getScheduleDeliveryTime(), sms.getTrntype(), sms.getSend_part(), sms.getRef_no()));
                        } else {
                            SetMessagePending(sms.getTran_cd(), sms.getTrntype());
                        }
                        sms = null;
                        System.gc();
                    } catch (Exception ex) {
                        System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " Try Exception " + ex.getMessage() + "\n");
                        dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "Try Exception " + ex.getMessage());
                    }
                }
                executorService.shutdown();
                try {
                    executorService.awaitTermination(SMPP_TRANSACTIONTIMER, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted");
                }
            } else if (bindType.IsReceivable()) {
                while (lb_run) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            this.unbindAndClose();
        }

        public Runnable newSendTask(final long tran_cd, final String source, final String destination, final String message, final boolean ab_flash, final boolean ab_unicode, final boolean ab_scheduler, final String scheduleDeliveryDt, final String trntype, final int send_part, final int ref_no) {
            return new Runnable() {
                public void run() {
                    String ls_messageId = null;
                    String scheduleDeliveryTime = "";
                    String validityPeriod = "";

                    try {
                        if (ab_scheduler) {
                            try {
                                Date scheduler_dt = stringRelativeTimeFormatter.parse(scheduleDeliveryDt);
                                scheduleDeliveryTime = absoluteTimeFormatter.format(scheduler_dt);
                                validityPeriod = validityPeriodFormatter.format(scheduler_dt, VALIDITY_PERIOD);
                                scheduleDeliveryTime = "";
                            } catch (ParseException e) {
                            }
                        } else {
                            if (VALIDITY_PERIOD > 0) {
                                Date targetTime = new Date();
                                validityPeriod = validityPeriodFormatter.format(targetTime, VALIDITY_PERIOD);
                            }
                        }

                        for (int li_loop = 1; li_loop <= RETRY_ATTEMPT; li_loop++) {
                            try {
                                ls_messageId = SendSms(tran_cd, trntype, source, destination, ab_flash, ab_unicode, message, scheduleDeliveryTime, validityPeriod, send_part, ref_no);

                            } catch (ResponseTimeoutException ex) {
                                //  In case of submit timeout, so when internet connection is lost after
                                //  submitting an SMS, you cannot tell whether the SMS was sent or not.
                                //  The service provider will not give back any response.
                                System.out.print(TimeFormatter.format(new Date()) + " --:  ResponseTimeoutException " + ex.getMessage() + "\n");
                                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "ResponseTimeoutException " + ex.getMessage());
                            }
                            System.out.print(TimeFormatter.format(new Date()) + " --:  SEND SMS MOBILE NO: " + destination + " SENDER ID " + source + " SEND ATTEMPT " + li_loop + " MESSAGE ID " + ls_messageId + "\n");
                            UpdateSendAttempt(tran_cd, trntype);
                            if (ls_messageId != null) {
                                UpdateMessageId(tran_cd, trntype, ls_messageId.toUpperCase());
                            //    DeleteSendSMS(tran_cd, trntype);
                                break;
                            } else if (li_loop == RETRY_ATTEMPT) {
                                RejectMessage(tran_cd, trntype, 8);
                            }
                        }
                    } catch (IOException ex) {
                        System.out.print(TimeFormatter.format(new Date()) + " --:  IOException Message Ref. No: " + tran_cd + " " + ex.getMessage() + "\n");
                        SetMessagePending(tran_cd, trntype);
                        dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "IOException Message Ref. No: " + tran_cd + " " + ex.getMessage());

                    } catch (NegativeResponseException ex) {
                        if (ex.getCommandStatus() == 88) { //Throttling error (ESME has exceeded allowed message limits)
                            SetMessagePending(tran_cd, trntype);
                        } else {// 168 DND Block, 1009 Black Listed Number, 11 Invalid destination address, 300 Incorrect destination address
                            RejectMessage(tran_cd, trntype, ex.getCommandStatus());
                        }
                        System.out.print(TimeFormatter.format(new Date()) + " --:  NegativeResponseException " + ex.getMessage() + "\n");
                        dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, " NegativeResponseException " + ex.getMessage());
                    }
                }
            };
        }

        private String SendSms(long tran_cd, String trntype, String source, String destination, boolean ab_flash, boolean ab_unicode, String message, String scheduleDeliveryTime, String validityPeriod, int send_part, int ref_no) throws IOException, NegativeResponseException, ResponseTimeoutException {
            String ls_message_Id = null;
            MessageClass messageClass = MessageClass.CLASS1;
            Alphabet alphabet = null;
            int maximumSingleMessageSize = 0;
            int maximumMultipartMessageSegmentSize = 0;
            byte[] byteSingleMessage = null;

            if (ab_unicode) {
                alphabet = (ab_flash) ? Alphabet.ALPHA_UCS2_FLASH : Alphabet.ALPHA_UCS2;
                byteSingleMessage = message.getBytes("UTF-16BE");
                maximumSingleMessageSize = MAX_SINGLE_MSG_SEGMENT_SIZE_UCS2;
                maximumMultipartMessageSegmentSize = MAX_MULTIPART_MSG_SEGMENT_SIZE_UCS2;
            } else {
                alphabet = (ab_flash) ? Alphabet.ALPHA_FLASH : Alphabet.ALPHA_DEFAULT;
                byteSingleMessage = message.getBytes();
                maximumSingleMessageSize = MAX_SINGLE_MSG_SEGMENT_SIZE_7BIT;
                maximumMultipartMessageSegmentSize = MAX_MULTIPART_MSG_SEGMENT_SIZE_7BIT;
            }

            byte[][] byteMessagesArray = null;
            ESMClass esmClass = null;

            if (message.length() > maximumSingleMessageSize) {
                byteMessagesArray = splitUnicodeMessage(byteSingleMessage, maximumMultipartMessageSegmentSize, tran_cd, trntype, ref_no);
                esmClass = new ESMClass(MessageMode.DEFAULT, MessageType.DEFAULT, GSMSpecificFeature.UDHI);
            } else {
                byteMessagesArray = new byte[][]{byteSingleMessage};
                esmClass = new ESMClass();
            }

            for (int i = send_part; i <= byteMessagesArray.length; i++) {
                long startTime = System.currentTimeMillis();
                requestCounter.incrementAndGet();
                ls_message_Id = submitMessage(byteMessagesArray[i - 1], source, destination, messageClass, alphabet, esmClass, scheduleDeliveryTime, validityPeriod);
                UpdateSendPart(tran_cd, trntype);
                long delay = System.currentTimeMillis() - startTime;
                responseCounter.incrementAndGet();
                if (maxDelay.get() < delay) {
                    maxDelay.set(delay);
                }
            }
            return ls_message_Id;
        }

        private String submitMessage(byte[] message, String source, String destination,
                MessageClass messageClass, Alphabet alphabet, ESMClass esmClass, String scheduleDeliveryTime, String validityPeriod) throws IOException, NegativeResponseException, ResponseTimeoutException {

            String messageId = null;
            try {
                messageId = session.submitShortMessage("CMT", TypeOfNumber.ALPHANUMERIC, NumberingPlanIndicator.UNKNOWN,
                        source, TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.ISDN, destination, esmClass,
                        (byte) 0, (byte) 1, scheduleDeliveryTime, validityPeriod, new RegisteredDelivery(SMSCDeliveryReceipt.SUCCESS_FAILURE),
                        (byte) 0, new GeneralDataCoding(alphabet, esmClass), (byte) 0, message);
            } catch (PDUException e) {
                System.out.print(TimeFormatter.format(new Date()) + " --:  PDUException " + e.getMessage() + "\n");
                // Invalid PDU parameter
            } catch (InvalidResponseException e) {
                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "InvalidResponseException " + e.getMessage());
                // Invalid response
            }
            return messageId;
        }

        private byte[][] splitUnicodeMessage(byte[] aMessage, Integer maximumMultipartMessageSegmentSize, long tran_cd, String trntype, int ref_no) {

            final byte UDHIE_HEADER_LENGTH = 0x05;
            final byte UDHIE_IDENTIFIER_SAR = 0x00;
            final byte UDHIE_SAR_LENGTH = 0x03;

            // determine how many messages have to be sent
            int numberOfSegments = aMessage.length / maximumMultipartMessageSegmentSize;
            int messageLength = aMessage.length;
            if (numberOfSegments > 255) {
                numberOfSegments = 255;
                messageLength = numberOfSegments * maximumMultipartMessageSegmentSize;
            }
            if ((messageLength % maximumMultipartMessageSegmentSize) > 0) {
                numberOfSegments++;
            }

            // prepare array for all of the msg segments
            byte[][] segments = new byte[numberOfSegments][];
            byte[] referenceNumber = new byte[1];
            int lengthOfData;

            if (ref_no == 0) {
                // generate new reference number
                new Random().nextBytes(referenceNumber);
                UpdateRefNo(tran_cd, trntype, (int) referenceNumber[0]);
            } else {
                referenceNumber[0] = (byte) ref_no;
            }

            // split the message adding required headers
            for (int i = 0; i < numberOfSegments; i++) {
                if (numberOfSegments - i == 1) {
                    lengthOfData = messageLength - i * maximumMultipartMessageSegmentSize;
                } else {
                    lengthOfData = maximumMultipartMessageSegmentSize;
                }

                // new array to store the header
                segments[i] = new byte[6 + lengthOfData];

                // UDH header
                // doesn't include itself, its header length
                segments[i][0] = UDHIE_HEADER_LENGTH;
                // SAR identifier
                segments[i][1] = UDHIE_IDENTIFIER_SAR;
                // SAR length
                segments[i][2] = UDHIE_SAR_LENGTH;
                // reference number (same for all messages)
                segments[i][3] = referenceNumber[0];
                // total number of segments
                segments[i][4] = (byte) numberOfSegments;
                // segment number
                segments[i][5] = (byte) (i + 1);
                // copy the data into the array
                System.arraycopy(aMessage, (i * maximumMultipartMessageSegmentSize), segments[i], 6, lengthOfData);

            }
            return segments;
        }

        private void UpdateSendAttempt(long al_tran_cd, String as_trn_type) {
            Connection con = null;
            Statement upd_stmt = null;
            try {
                con = dbconfig.GetConncetion();
                upd_stmt = con.createStatement();
                String sql = "UPDATE sms_trn SET SEND_ATTEMPT = SEND_ATTEMPT + 1 WHERE TRAN_ID =" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
                int rows = upd_stmt.executeUpdate(sql);
                upd_stmt.close();
                con.close();
            } catch (SQLException ex) {
                System.out.print(TimeFormatter.format(new Date()) + " --:  UpdateSendAttempt SQLException " + ex.getMessage() + "\n");
                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "UPADATE SQLException " + ex.getMessage());
            } finally {
                if (upd_stmt != null) {
                    try {
                        upd_stmt.close();
                    } catch (SQLException ex) {
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex) {
                    }
                }
            }
        }

        private void UpdateSendPart(long al_tran_cd, String as_trn_type) {
            Connection con = null;
            Statement upd_stmt = null;
            try {
                con = dbconfig.GetConncetion();
                upd_stmt = con.createStatement();
                String sql = "UPDATE sms_trn SET SEND_PART = SEND_PART + 1 WHERE TRAN_ID =" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
                int rows = upd_stmt.executeUpdate(sql);
                upd_stmt.close();
                con.close();
            } catch (SQLException ex) {
                System.out.print(TimeFormatter.format(new Date()) + " --:  UpdateSendAttempt SQLException " + ex.getMessage() + "\n");
                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "UPADATE SQLException " + ex.getMessage());
            } finally {
                if (upd_stmt != null) {
                    try {
                        upd_stmt.close();
                    } catch (SQLException ex) {
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex) {
                    }
                }
            }
        }

        private void UpdateRefNo(long al_tran_cd, String as_trn_type, int ref_no) {
            Connection con = null;
            Statement upd_stmt = null;
            try {
                con = dbconfig.GetConncetion();
                upd_stmt = con.createStatement();
                String sql = "UPDATE sms_trn SET REF_NO ='" + ref_no + "' WHERE TRAN_ID=" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
                int rows = upd_stmt.executeUpdate(sql);
                upd_stmt.close();
                con.close();
            } catch (SQLException ex) {
                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "UPADATE SQLException " + ex.getMessage());
            } finally {
                if (upd_stmt != null) {
                    try {
                        upd_stmt.close();
                    } catch (SQLException ex) {
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex) {
                    }
                }
            }
        }

        private void UpdateMessageId(long al_tran_cd, String as_trn_type, String as_messageId) {
            Connection con = null;
            Statement upd_stmt = null;
            try {
                con = dbconfig.GetConncetion();
                upd_stmt = con.createStatement();
                String sql = "UPDATE sms_trn SET SMS_STATUS='Y',SEND_DT = SYSDATE(), MESSAGE_ID = '" + as_messageId + "' WHERE TRAN_ID=" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
                int rows = upd_stmt.executeUpdate(sql);
                upd_stmt.close();
                con.close();
            } catch (SQLException ex) {
                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "UPADATE SQLException " + ex.getMessage());
            } finally {
                if (upd_stmt != null) {
                    try {
                        upd_stmt.close();
                    } catch (SQLException ex) {
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex) {
                    }
                }
            }
        }
        
        private void DeleteSendSMS(long al_tran_cd, String as_trn_type) {
            Connection con = null;
            Statement del_stmt = null;
            try {
                con = dbconfig.GetConncetion();
                del_stmt = con.createStatement();
                String sql = "DELETE FROM sms_trn WHERE TRAN_ID=" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
                int rows = del_stmt.executeUpdate(sql);
                del_stmt.close();
                con.close();
            } catch (SQLException ex) {
                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "DeleteSMS SQLException " + ex.getMessage());
            } finally {
                if (del_stmt != null) {
                    try {
                        del_stmt.close();
                    } catch (SQLException ex) {
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex) {
                    }
                }
            }
        }

        private void RejectMessage(long al_tran_cd, String as_trn_type, int error_cd) {
            Connection con = null;
            Statement upd_stmt = null;
            try {
                con = dbconfig.GetConncetion();
                upd_stmt = con.createStatement();
                String sql = "UPDATE sms_trn SET SMS_STATUS = 'Y', ERROR_CD ='" + error_cd + "', SEND_DT = SYSDATE() WHERE DELIVERY_STATUS = 'P' AND TRAN_ID=" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
                int rows = upd_stmt.executeUpdate(sql);
                upd_stmt.close();
                con.close();
            } catch (SQLException ex) {
                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "UPADATE SQLException " + ex.getMessage());
            } finally {
                if (upd_stmt != null) {
                    try {
                        upd_stmt.close();
                    } catch (SQLException ex) {
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex) {
                    }
                }
            }
        }

        private void SetMessagePending(long al_tran_cd, String as_trn_type) {
            Connection con = null;
            Statement upd_stmt = null;
            try {
                con = dbconfig.GetConncetion();
                upd_stmt = con.createStatement();
                String sql = "UPDATE sms_trn SET SMS_STATUS = 'P' WHERE TRAN_ID=" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
                int rows = upd_stmt.executeUpdate(sql);
                upd_stmt.close();
                con.close();
            } catch (SQLException ex) {
                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "SetMessagePending SQLException " + ex.getMessage());
            } finally {
                if (upd_stmt != null) {
                    try {
                        upd_stmt.close();
                    } catch (SQLException ex) {
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex) {
                    }
                }
            }
        }

        private class MessageReceiverListenerImpl implements MessageReceiverListener {

            private static final String DATASM_NOT_IMPLEMENTED = "data_sm not implemented";

            @Override
            public void onAcceptDeliverSm(DeliverSm deliverSm) throws ProcessRequestException {
                if (MessageType.SMSC_DEL_RECEIPT.containedIn(deliverSm.getEsmClass())) {
                    // this message is delivery receipt
                    try {
                        DeliveryReceipt delReceipt = deliverSm.getShortMessageAsDeliveryReceipt();
                        String messageId = delReceipt.getId().toUpperCase();
                        sms_dlr.add(new DLR(deliverSm.getDestAddress(),
                                deliverSm.getSourceAddr(),
                                messageId, delReceipt.getSubmitted(),
                                delReceipt.getDelivered(),
                                delReceipt.getSubmitDate(),
                                delReceipt.getDoneDate(),
                                delReceipt.getFinalStatus(),
                                delReceipt.getError(),
                                delReceipt.getText()));
                        System.out.print(TimeFormatter.format(new Date()) + " --:  DLR RECEIVE " + deliverSm.getSourceAddr() + " " + deliverSm.getDestAddress() + " " + messageId + "\n");
                    } catch (InvalidDeliveryReceiptException e) {
                        dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "InvalidDeliveryReceiptException " + e.getMessage());
                    }
                } else {
                    dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "Receiving message " + new String(deliverSm.getShortMessage()));
                }
            }

            @Override
            public void onAcceptAlertNotification(AlertNotification alertNotification) {
                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "onAcceptAlertNotification " + alertNotification.toString());
            }

            @Override
            public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "onAcceptDataSm " + dataSm.toString());
                throw new ProcessRequestException(DATASM_NOT_IMPLEMENTED, SMPPConstant.STAT_ESME_RINVCMDID);
            }
        }

        private class SessionStateListenerImpl implements SessionStateListener {

            @Override
            public void onStateChange(SessionState newState, SessionState oldState, Session source) {
                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "ON STATE CHANGE NEW STATE " + newState.toString() + " OLD STATE " + oldState.toString());
                System.out.print(TimeFormatter.format(new Date()) + " --:  ON STATE CHANGE NEW STATE " + newState.toString() + " OLD STATE " + oldState.toString() + "\n");
                if (newState.equals(SessionState.CLOSED) && (oldState.equals(SessionState.BOUND_TX) || oldState.equals(SessionState.BOUND_RX) || oldState.equals(SessionState.BOUND_TRX))) {
                    dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "SMPP SESSION " + SESSION_ID + " DISCONNECT");
                    System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " DISCONNECT\n");
                    //session = null;
                    reconnectAfter(RECONNECT_INTERVAL);
                }
            }
        }

        private void reconnectAfter(final long timeInMillis) {
            try {
                Thread.sleep(timeInMillis);
            } catch (InterruptedException ignored) {
            }
            dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "RECONNECT AFTER SMPP SESSION " + SESSION_ID);

            try {
                int attempt = 0;
                while (lb_run && (this.session == null || this.session.getSessionState().equals(SessionState.CLOSED))) {
                    attempt++;
                    try {
                        this.session = null;
                        System.gc();
                        System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " RECONNECT ATTEMPT " + attempt + "\n");
                        this.session = newSession();
                        try {///Sleep for session init.
                            Thread.sleep(5000);
                        } catch (InterruptedException ignored) {
                        }
                    } catch (IOException e) {
                        System.out.print(TimeFormatter.format(new Date()) + " --:  Reconnect IOException: " + e.getMessage() + "\n");
                        dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "Reconnect IOException: " + e.getMessage());
                        this.session = null;
                        System.gc();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            } catch (Exception e) {
                dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "RECONNECT WHILE LOOP " + e.getMessage());
            }
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " RECONNECT\n");
        }

        private SMPPSession newSession() throws IOException {
            SMPPSession tmpSession = new SMPPSession();
            tmpSession.setPduProcessorDegree(SMPP_TPS);
            tmpSession.setTransactionTimer(SMPP_TRANSACTIONTIMER);
            tmpSession.addSessionStateListener(new SessionStateListenerImpl());

            if (bindType.IsReceivable()) {
                tmpSession.setMessageReceiverListener(new MessageReceiverListenerImpl());
            }
            this.bindParameter = new BindParameter(bindType,
                    this.SMPP_USER_ID, this.SMPP_PASSWORD, "TR", TypeOfNumber.ALPHANUMERIC, NumberingPlanIndicator.UNKNOWN, null);
            tmpSession.connectAndBind(this.SMPP_IP_ADDRESS, this.SMPP_PORT, this.bindParameter);
            return tmpSession;
        }

        public void unbindAndClose() {
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " SEND STOP...\n");
            this.session.unbindAndClose();
        }
    }
}
