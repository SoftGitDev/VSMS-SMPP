package com.softtech;

import com.softtech.smpp.session.DataSmResult;
import com.softtech.smpp.session.Session;
import com.softtech.smpp.bean.DataSm;
import com.softtech.smpp.bean.AlertNotification;
import com.softtech.smpp.extra.ProcessRequestException;
import com.softtech.smpp.bean.DeliveryReceipt;
import com.softtech.smpp.util.InvalidDeliveryReceiptException;
import com.softtech.smpp.bean.DeliverSm;
import com.softtech.smpp.extra.SessionState;
import java.util.Random;
import com.softtech.smpp.InvalidResponseException;
import com.softtech.smpp.PDUException;
import com.softtech.smpp.bean.DataCoding;
import com.softtech.smpp.bean.OptionalParameter;
import com.softtech.smpp.bean.GeneralDataCoding;
import com.softtech.smpp.bean.RegisteredDelivery;
import com.softtech.smpp.bean.SMSCDeliveryReceipt;
import com.softtech.smpp.bean.ESMClass;
import com.softtech.smpp.bean.GSMSpecificFeature;
import com.softtech.smpp.bean.MessageType;
import com.softtech.smpp.bean.MessageMode;
import com.softtech.smpp.bean.Alphabet;
import com.softtech.smpp.bean.MessageClass;
import com.softtech.smpp.extra.NegativeResponseException;
import com.softtech.smpp.extra.ResponseTimeoutException;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import com.softtech.smpp.bean.NumberingPlanIndicator;
import com.softtech.smpp.bean.TypeOfNumber;
import com.softtech.smpp.session.MessageReceiverListener;
import com.softtech.smpp.session.SessionStateListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import com.softtech.smpp.session.BindParameter;
import com.softtech.smpp.session.SMPPSession;
import com.softtech.smpp.util.DeliveryReceiptState;
import java.text.ParseException;
import java.util.Iterator;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Collection;
import java.sql.SQLException;
import java.util.TimeZone;
import java.util.Date;
import java.util.ArrayList;
import com.softtech.smpp.bean.BindType;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import com.softtech.smpp.util.ValidityPeriodFormatter;
import com.softtech.smpp.util.AbsoluteTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

public class SendSMS1 extends Thread {

    private String SMPP = "";
    private String SMPP_IP_ADDRESS = "";
    //private final static String SMPP_IP_ADDRESS = "203.145.131.84";
    private int SMPP_PORT = 8888;
    private String SMPP_USER_ID = "";
    private String SMPP_PASSWORD = "";
    private String SMPP_MODE = "TRX";
    private static int ROUTE_ID = 4;

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
    private SendSMS1.SMSSession[] smsSession;
    private SendSMS1.DeliveryReceiptThread[] DlrSession;
    private BindType bindType = BindType.BIND_TRX;

    public SendSMS1(int route_id) {
        ROUTE_ID = route_id;
        System.out.print(TimeFormatter.format(new Date()) + " --:  Initialising...\n");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        stringRelativeTimeFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public SendSMS1() {
        System.out.print(TimeFormatter.format(new Date()) + " --:  Initialising...\n");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        stringRelativeTimeFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Override
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
                    Thread.sleep(2000L);
                } catch (InterruptedException ex2) {
                }
            }
        }
        try {
            sql_stmt = con.createStatement();
            ls_sql = "SELECT ROUTE_NM,USER_NM,PASSWORD,MODE,TPS,SESSION,IP_ADDRESS,PORT,RETRY_ATTEMPT,ACTIVE,RUN_STATUS,SELECT_LIMIT,VALIDITY_PERIOD,7BIT_SINGLE_MSG_SIZE,7BIT_MULTIPART_MSG_SIZE,UCS2_SINGLE_MSG_SIZE,UCS2_MULTIPART_MSG_SIZE  FROM sms_route_mst WHERE ACTIVE ='Y' AND ID =" + SendSMS1.ROUTE_ID;
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
                this.lb_run = true;
                if (this.RETRY_ATTEMPT <= 0) {
                    this.RETRY_ATTEMPT = 1;
                }
                if (this.SELECT_LIMIT <= 0) {
                    this.SELECT_LIMIT = 1;
                }
                this.SMPP_TPS = Math.round((float) (this.SMPP_TPS / this.SESSION));
                if (this.SMPP_TPS <= 0) {
                    this.SMPP_TPS = 10;
                }
                final String smpp_MODE = this.SMPP_MODE;
                switch (smpp_MODE) {
                    case "TX": {
                        this.bindType = BindType.BIND_TX;
                        continue;
                    }
                    case "RX": {
                        this.bindType = BindType.BIND_RX;
                        continue;
                    }
                    default: {
                        this.bindType = BindType.BIND_TRX;
                        continue;
                    }
                }
            }
            rs.close();
            sql_stmt.close();
            con.close();
        } catch (SQLException ex) {
            System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SQLException " + ex.getMessage() + "\n");
            dbconfig.WriteLog("Y", ERROR_LOG_PATH, this.SMPP, "SQLException " + ex.getMessage());
        }
        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  ===========================================\n");
        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SMPP_IP_ADDRESS: " + this.SMPP_IP_ADDRESS + "\n");
        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SMPP_PORT: " + this.SMPP_PORT + "\n");
        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SMPP_USER_ID: " + this.SMPP_USER_ID + "\n");
        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SMPP_PASSWORD: " + this.SMPP_PASSWORD + "\n");
        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SMPP_MODE: " + this.SMPP_MODE + "\n");
        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  ROUTE_ID: " + SendSMS1.ROUTE_ID + "\n");
        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SMPP_TPS: " + this.SMPP_TPS + "\n");
        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  RETRY_ATTEMPT: " + this.RETRY_ATTEMPT + "\n");
        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  ===========================================\n");
        this.smsSession = new SMSSession[this.SESSION];
        this.threadIsAlive = new boolean[this.SESSION];
        this.DlrSession = new DeliveryReceiptThread[5];
        
        for (int i = 0; i < this.smsSession.length; ++i) {
            (this.smsSession[i] = new SMSSession(this.SMPP_IP_ADDRESS, this.SMPP_PORT, this.SMPP_USER_ID, this.SMPP_PASSWORD, i + 1, SendSMS1.ROUTE_ID)).start();
            this.threadIsAlive[i] = true;
        }
        
        this.SetSMSRouteStatus(SendSMS1.ROUTE_ID, "Y");
        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  Initialising complate\n");
        
        new ThreadWatcherThread().start();
        
        if (this.bindType.IsTransmittable() && !this.bindType.IsReceivable()) {
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
        } else if (this.bindType.IsTransmittable()&& this.bindType.IsReceivable()) {
            new TrafficWatcherThread().start();
            for (int i = 0; i < DlrSession.length; i++) {
                DlrSession[i] = new DeliveryReceiptThread();
                DlrSession[i].start();
            }
            this.sendProcess();
        }
        
        this.sms_dlr.drainTo(this.DlrDrainList);
        for (final DLR dlr : this.DlrDrainList) {
            System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  DlrDrainList " + dlr.getMESSAGE_ID() + "\n");
            this.InsertDeliverSm(dlr.getMOBILE_NO(), dlr.getSENDER_ID(), dlr.getMESSAGE_ID(), dlr.getSUB(), dlr.getDELIVER(), dlr.getSUBMIT_DT(), dlr.getDONE_DT(), dlr.getDELIVERY_STATUS(), dlr.getERROR(), dlr.getTEXT());
        }
        this.sms_queue.drainTo(this.SmsDrainList);
        for (final SMS sms : this.SmsDrainList) {
            System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SmsDrainList " + sms.getTran_cd() + "\n");
            this.SetMessagePending(sms.getTran_cd(), sms.getTrntype());
        }
        for (int i = 0; i < this.smsSession.length; ++i) {
            if (this.threadIsAlive[i] && this.smsSession[i].isAlive()) {
                this.smsSession[i].unbindAndClose();
                this.threadIsAlive[i] = false;
            }
        }
        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SMPP ALL SESSION DEAD\n");
        dbconfig.WriteLog("Y", ERROR_LOG_PATH, this.SMPP, "SMPP STOP");
        this.SetSMSRouteStatus(SendSMS1.ROUTE_ID, "N");
        System.exit(0);
    }

    private void sendProcess() {
        ResultSet rs = null;
        Statement sql_stmt = null;
        String ls_sql = "";
        ls_sql = "SELECT SELLER_ID,USER_ID,SENDER_ID,TRN_TYPE, COUNT(NO_OF_SMS) AS NO_OF_SMS FROM sms_trn WHERE SMS_STATUS = 'P' AND SCHEDULER_DT <= SYSDATE() AND ROUTE_ID = " + SendSMS1.ROUTE_ID + " GROUP BY SELLER_ID,USER_ID,SENDER_ID,TRN_TYPE ORDER BY NO_OF_SMS ASC,TRN_TYPE ASC";
        while (this.lb_run) {
            boolean lb_row_found = false;
            try {
                if (!dbconfig.isDbConnected(this.conn)) {
                    try {
                        this.conn = dbconfig.GetConncetion();
                    } catch (Exception ex) {
                        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  CreateStatement Exception " + ex.getMessage() + "\n");
                        dbconfig.WriteLog("Y", ERROR_LOG_PATH, this.SMPP, "CreateStatement Exception " + ex.getMessage());
                    }
                } else {
                    try {
                        sql_stmt = this.conn.createStatement();
                        rs = sql_stmt.executeQuery(ls_sql);
                        while (rs.next()) {
                            lb_row_found = true;
                            this.SendSms(rs.getString("SELLER_ID"), rs.getString("USER_ID"), rs.getString("SENDER_ID"), rs.getString("TRN_TYPE"));
                        }
                        rs.close();
                        sql_stmt.close();
                    } catch (SQLException ex2) {
                        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SendProcess SQLException " + ex2.getMessage() + "\n");
                        dbconfig.WriteLog("Y", ERROR_LOG_PATH, this.SMPP, " SendProcess SQLException " + ex2.getMessage());
                    } catch (Exception ex) {
                        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SendProcess Exception " + ex.getMessage() + "\n");
                        dbconfig.WriteLog("Y", ERROR_LOG_PATH, this.SMPP, " SendProcess Exception " + ex.getMessage());
                    }
                    if (!lb_row_found) {
                        try {
                            Thread.sleep(2000L);
                        } catch (InterruptedException ex3) {
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  Exception " + ex.getMessage() + "\n");
                dbconfig.WriteLog("Y", ERROR_LOG_PATH, this.SMPP, "Exception " + ex.getMessage());
            }
            System.gc();
        }
    }

    private void SendSms(final String SELLER_ID, final String USER_ID, final String SENDER_ID, final String TRN_TYPE) {
        String ls_sql_sms = "";
        Statement sms_stmt = null;
        ResultSet rs_sms = null;
        ls_sql_sms = "SELECT TRAN_ID,TRAN_DT,SENDER_ID,MOBILE_NO,MESSAGE,UNICODE,MESSAGE_TYPE,SCHEDULER,SCHEDULER_DT,SEND_PART,REF_NO,NO_OF_SMS FROM sms_trn WHERE SELLER_ID = '" + SELLER_ID + "' AND USER_ID = '" + USER_ID + "' AND SENDER_ID = '" + SENDER_ID + "' AND SMS_STATUS = 'P'  AND TRN_TYPE ='" + TRN_TYPE + "' AND SCHEDULER_DT <= SYSDATE()  AND ROUTE_ID = " + SendSMS1.ROUTE_ID + " ORDER BY TRAN_DT ASC LIMIT " + this.SELECT_LIMIT;
        try {
            sms_stmt = this.conn.createStatement();
            rs_sms = sms_stmt.executeQuery(ls_sql_sms);
            while (rs_sms.next()) {
                //this.sms_queue.add(new SMS(rs_sms.getLong("TRAN_ID"), rs_sms.getString("TRAN_DT"), rs_sms.getString("SENDER_ID").trim(), rs_sms.getString("MOBILE_NO").trim(), rs_sms.getString("MESSAGE").trim(), rs_sms.getString("UNICODE").trim().equals("Y"), rs_sms.getString("MESSAGE_TYPE").trim().equals("F"), rs_sms.getString("SCHEDULER").trim().equals("Y"), rs_sms.getString("SCHEDULER_DT"), rs_sms.getInt("SEND_PART"), rs_sms.getInt("REF_NO"), rs_sms.getInt("NO_OF_SMS"), TRN_TYPE,"A"));
            }
            rs_sms.close();
            sms_stmt.close();
        } catch (SQLException ex) {
            System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SEND SendSms SQLException " + ex.getMessage() + "\n");
            dbconfig.WriteLog("Y", ERROR_LOG_PATH, this.SMPP, "SEND SendSms SQLException " + ex.getMessage());
        } catch (Exception ex2) {
            System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SEND SendSms Exception " + ex2.getMessage() + "\n");
            dbconfig.WriteLog("Y", ERROR_LOG_PATH, this.SMPP, "SEND SendSms Exception " + ex2.getMessage());
        }
    }

    private void SetSMSRouteStatus(final int ROUTE_ID, final String as_status) {
        String sql = "";
        Statement stmt = null;
        Connection con = null;
        try {
            con = dbconfig.GetConncetion();
            stmt = con.createStatement();
            sql = "UPDATE sms_route_mst SET RUN_STATUS = '" + as_status + "' WHERE ID=" + ROUTE_ID;
            final int rows = stmt.executeUpdate(sql);
            stmt.close();
            con.close();
        } catch (SQLException ex) {
            System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SetSMSRouteStatus SQLException " + ex.getMessage() + "\n");
            dbconfig.WriteLog("Y", ERROR_LOG_PATH, this.SMPP, "UPADATE RUN_STATUS " + ex.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex2) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex3) {
                }
            }
        }
    }

    private void SetMessagePending(final long al_tran_cd, final String as_trn_type) {
        Connection con = null;
        Statement upd_stmt = null;
        try {
            con = dbconfig.GetConncetion();
            upd_stmt = con.createStatement();
            final String sql = "UPDATE sms_trn SET SMS_STATUS = 'P' WHERE TRAN_ID=" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
            final int rows = upd_stmt.executeUpdate(sql);
            upd_stmt.close();
            con.close();
        } catch (SQLException ex) {
            dbconfig.WriteLog("Y", ERROR_LOG_PATH, this.SMPP, "SetMessagePending SQLException " + ex.getMessage());
        } finally {
            if (upd_stmt != null) {
                try {
                    upd_stmt.close();
                } catch (SQLException ex2) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException ex3) {
                }
            }
        }
    }

    private void DeleteSendSMS1() {
        String sql = "";
        Statement del_stmt = null;
        Connection db_conn = null;
        try {
            final SimpleDateFormat parser = new SimpleDateFormat("HH:mm");
            final String date = parser.format(new Date());
            final Date d1 = parser.parse("23:57");
            final Date d2 = parser.parse("23:59");
            final Date userDate = parser.parse(date);
            if (userDate.after(d1) && userDate.before(d2)) {
                this.totalRequestCounter.set(0L);
                this.totalResponseCounter.set(0L);
                try {
                    db_conn = dbconfig.GetConncetion();
                    del_stmt = db_conn.createStatement();
                    sql = "DELETE FROM sms_trn WHERE SMS_STATUS = 'Y'";
                    final int rows = del_stmt.executeUpdate(sql);
                    del_stmt.close();
                    db_conn.close();
                } catch (SQLException ex) {
                    System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  DELETE SEND SMS SQLException " + ex.getMessage() + "\n");
                    dbconfig.WriteLog("Y", ERROR_LOG_PATH, this.SMPP, "DELETE SEND SMS SQLException " + ex.getMessage());
                }
            } else {
                try {
                    db_conn = dbconfig.GetConncetion();
                    del_stmt = db_conn.createStatement();
                    sql = "DELETE FROM sms_trn WHERE SMS_STATUS = 'Y' and DELIVERY_STATUS <> 'P'";
                    final int rows = del_stmt.executeUpdate(sql);
                    del_stmt.close();
                    db_conn.close();
                } catch (SQLException ex) {
                    System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  DELETE SEND SMS SQLException " + ex.getMessage() + "\n");
                    dbconfig.WriteLog("Y", ERROR_LOG_PATH, this.SMPP, "DELETE SEND SMS SQLException " + ex.getMessage());
                }
            }
        } catch (ParseException ex2) {
            System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  DELETE SEND SMS ParseException " + ex2.getMessage() + "\n");
            dbconfig.WriteLog("Y", ERROR_LOG_PATH, this.SMPP, " DELETE SEND SMS ParseException " + ex2.getMessage());
        } finally {
            if (del_stmt != null) {
                try {
                    del_stmt.close();
                } catch (SQLException ex3) {
                }
            }
            if (db_conn != null) {
                try {
                    db_conn.close();
                } catch (SQLException ex4) {
                }
            }
        }
    }

    private void GetSetting() {
        Connection db_conn = null;
        Statement sql_stmt = null;
        ResultSet rs = null;
        try {
            db_conn = dbconfig.GetConncetion();
            sql_stmt = db_conn.createStatement();
            final String ls_sql = "SELECT PORT,USER_NM,PASSWORD,IP_ADDRESS,RETRY_ATTEMPT,TPS,ACTIVE,SELECT_LIMIT,VALIDITY_PERIOD,7BIT_SINGLE_MSG_SIZE,7BIT_MULTIPART_MSG_SIZE,UCS2_SINGLE_MSG_SIZE,UCS2_MULTIPART_MSG_SIZE  FROM sms_route_mst WHERE ID =" + SendSMS1.ROUTE_ID;
            rs = sql_stmt.executeQuery(ls_sql);
            while (rs.next()) {
                this.RETRY_ATTEMPT = rs.getInt("RETRY_ATTEMPT");
                final int tps = rs.getInt("TPS");
                this.lb_run = rs.getString("ACTIVE").trim().equalsIgnoreCase("Y");
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
                this.SMPP_TPS = Math.round((float) (tps / this.SESSION));
                if (this.SMPP_TPS <= 0) {
                    this.SMPP_TPS = 10;
                }
            }
            rs.close();
            sql_stmt.close();
            db_conn.close();
        } catch (SQLException ex) {
            dbconfig.WriteLog("Y", ERROR_LOG_PATH, "GetSetting", "SQLException " + ex.getMessage());
        } catch (Exception ex2) {
            dbconfig.WriteLog("Y", ERROR_LOG_PATH, "GetSetting", "Exception " + ex2.getMessage());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ex3) {
                }
            }
            if (sql_stmt != null) {
                try {
                    sql_stmt.close();
                } catch (SQLException ex4) {
                }
            }
            if (db_conn != null) {
                try {
                    db_conn.close();
                } catch (SQLException ex5) {
                }
            }
        }
        dbconfig.WriteStatus(ERROR_LOG_PATH, "SMPP IS RUNNING...");
    }

    private void InsertDeliverSm(final String MOBILE_NO, final String SENDER_ID, final String MESSAGE_ID, final String SUB, final String DELIVER, final String SUBMIT_DT, final String DONE_DT, final DeliveryReceiptState DELIVERY_STATUS, final String ERROR, final String TEXT) {
        Connection db_conn = null;
        Statement ins_stmt = null;
        try {
            final String ls_sql = "INSERT INTO sms_response_mst (`MOBILE_NO`,`SENDER_ID`,`MESSAGE_ID`,`SUB`,`DELIVER`,`SUBMIT_DT`,`DONE_DT`,`DELIVERY_STATUS`,`ERROR`,`TEXT`,`ROUTE_ID`) VALUES ('" + MOBILE_NO + "','" + SENDER_ID + "','" + MESSAGE_ID + "','" + SUB + "','" + DELIVER + "','" + SUBMIT_DT + "','" + DONE_DT + "','" + DELIVERY_STATUS + "','" + ERROR + "','" + TEXT + "'," + SendSMS1.ROUTE_ID + ")";
            db_conn = dbconfig.GetConncetion();
            ins_stmt = db_conn.createStatement();
            ins_stmt.executeUpdate(ls_sql);
            ins_stmt.close();
            db_conn.close();
        } catch (SQLException ex) {
            dbconfig.WriteLog("Y", ERROR_LOG_PATH, this.SMPP, "SQLException " + ex.getMessage());
            try {
                ins_stmt.close();
            } catch (SQLException ex2) {
            }
            try {
                db_conn.close();
            } catch (SQLException ex3) {
            }
        } finally {
            if (ins_stmt != null) {
                try {
                    ins_stmt.close();
                } catch (SQLException ex4) {
                }
            }
            if (db_conn != null) {
                try {
                    db_conn.close();
                } catch (SQLException ex5) {
                }
            }
        }
        System.gc();
    }

    private class ThreadWatcherThread extends Thread {

        @Override
        public void run() {
            System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  Starting ThreadWatcherThread\n");
            while (SendSMS1.this.lb_run) {
                SendSMS1.this.GetSetting();
                for (int i = 0; i < SendSMS1.this.smsSession.length; ++i) {
                    if (SendSMS1.this.threadIsAlive[i] && !SendSMS1.this.smsSession[i].isAlive()) {
                        SendSMS1.this.threadIsAlive[i] = false;
                        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SMPP SESSION: " + i + " IS DEAD\n");
                        (SendSMS1.this.smsSession[i] = new SMSSession(SendSMS1.this.SMPP_IP_ADDRESS, SendSMS1.this.SMPP_PORT, SendSMS1.this.SMPP_USER_ID, SendSMS1.this.SMPP_PASSWORD, i + 1, SendSMS1.ROUTE_ID)).start();
                        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SMPP SESSION: " + i + " START\n");
                        SendSMS1.this.threadIsAlive[i] = true;
                    }
                }
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    private class TrafficWatcherThread extends Thread {

        @Override
        public void run() {
            if (SendSMS1.this.bindType.IsTransmittable()) {
                System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  Starting traffic watcher...\n");
                while (SendSMS1.this.lb_run) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ex) {
                    }
                    final int requestPerSecond = SendSMS1.this.requestCounter.getAndSet(0);
                    final int responsePerSecond = SendSMS1.this.responseCounter.getAndSet(0);
                    final long maxDelayPerSecond = SendSMS1.this.maxDelay.getAndSet(0L);
                    SendSMS1.this.totalRequestCounter.addAndGet(requestPerSecond);
                    final long total = SendSMS1.this.totalResponseCounter.addAndGet(responsePerSecond);
                    System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  Request/Response per second: " + requestPerSecond + "/" + responsePerSecond + " of " + total + " maxDelay=" + maxDelayPerSecond + "\n");
                }
            }
        }
    }

    private class DeliveryReceiptThread extends Thread {

        @Override
        public void run() {
            final String ls_Query = "INSERT INTO sms_response_mst (`MOBILE_NO`,`SENDER_ID`,`MESSAGE_ID`,`SUB`,`DELIVER`,`SUBMIT_DT`,`DONE_DT`,`DELIVERY_STATUS`,`ERROR`,`TEXT`,`ROUTE_ID`) VALUES ";
            String ls_sql = "";
            int count = 0;
            Connection db_conn = null;
            Statement ins_stmt = null;
            long startTime = System.currentTimeMillis();
            if (SendSMS1.this.bindType.IsReceivable()) {
                System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  Starting DeliveryReceiptThread\n");
                while (SendSMS1.this.lb_run) {
                    try {
                        DLR dlr = (DLR) sms_dlr.take();
                        final long delay = System.currentTimeMillis() - startTime;
                        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  DLR delay " + delay + "\n");
                        if (delay > 10000L) {
                            count = 100;
                        }
                        if (count > 99) {
                            startTime = System.currentTimeMillis();
                            ls_sql = ls_sql + "('" + dlr.getMOBILE_NO() + "','" + dlr.getSENDER_ID() + "','" + dlr.getMESSAGE_ID() + "','" + dlr.getSUB() + "','" + dlr.getDELIVER() + "','" + dlr.getSUBMIT_DT() + "','" + dlr.getDONE_DT() + "','" + dlr.getDELIVERY_STATUS() + "','" + dlr.getERROR() + "','" + dlr.getTEXT() + "'," + SendSMS1.ROUTE_ID + ")";
                            try {
                                db_conn = dbconfig.GetConncetion();
                                ins_stmt = db_conn.createStatement();
                                ins_stmt.executeUpdate(ls_Query + ls_sql);
                                ins_stmt.close();
                                db_conn.close();
                                System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  DLR INSERT " + count + "\n");
                            } catch (SQLException ex) {
                                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "SQLException " + ex.getMessage());
                                try {
                                    ins_stmt.close();
                                } catch (SQLException ex3) {
                                }
                                try {
                                    db_conn.close();
                                } catch (SQLException ex4) {
                                }
                            } finally {
                                if (ins_stmt != null) {
                                    try {
                                        ins_stmt.close();
                                    } catch (SQLException ex5) {
                                    }
                                }
                                if (db_conn != null) {
                                    try {
                                        db_conn.close();
                                    } catch (SQLException ex6) {
                                    }
                                }
                            }
                            count = 0;
                            ls_sql = "";
                            System.gc();
                        } else {
                            ls_sql = ls_sql + "('" + dlr.getMOBILE_NO() + "','" + dlr.getSENDER_ID() + "','" + dlr.getMESSAGE_ID() + "','" + dlr.getSUB() + "','" + dlr.getDELIVER() + "','" + dlr.getSUBMIT_DT() + "','" + dlr.getDONE_DT() + "','" + dlr.getDELIVERY_STATUS() + "','" + dlr.getERROR() + "','" + dlr.getTEXT() + "'," + SendSMS1.ROUTE_ID + "),";
                        }
                        ++count;
                    } catch (Exception ex2) {
                        dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "Try Exception " + ex2.getMessage());
                    }
                }
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException ex7) {
                }
            }
        }
    }

    private class SMSSession extends Thread {

        private SMPPSession session;
        private BindParameter bindParameter;
        private String SMPP_IP_ADDRESS;
        private int SMPP_PORT;
        private String SMPP_USER_ID;
        private String SMPP_PASSWORD;
        private int SESSION_ID;
        private int ROUTE_ID;
        final SimpleDateFormat stringRelativeTimeFormatter;
        private ExecutorService executorService;

        public SMSSession(final String SMPP_IP_ADDRESS, final int SMPP_PORT, final String SMPP_USER_ID, final String SMPP_PASSWORD, final int SESSION_ID, final int ROUTE_ID) {
            this.session = null;
            this.bindParameter = null;
            this.SMPP_IP_ADDRESS = "";
            this.SMPP_PORT = 0;
            this.SMPP_USER_ID = "";
            this.SMPP_PASSWORD = "";
            this.SESSION_ID = 0;
            this.ROUTE_ID = 1;
            this.stringRelativeTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            this.executorService = Executors.newFixedThreadPool(SendSMS1.this.SMPP_TPS);
            this.SMPP_IP_ADDRESS = SMPP_IP_ADDRESS;
            this.SMPP_PORT = SMPP_PORT;
            this.SMPP_USER_ID = SMPP_USER_ID;
            this.SMPP_PASSWORD = SMPP_PASSWORD;
            this.SESSION_ID = SESSION_ID;
            this.ROUTE_ID = ROUTE_ID;
        }

        @Override
        public void run() {
            long counter = 0L;
            long lStartTime = 0L;
            long delay = 0L;
            try {
                (this.session = new SMPPSession()).setPduProcessorDegree(SendSMS1.this.SMPP_TPS);
                this.session.setTransactionTimer(SendSMS1.this.SMPP_TRANSACTIONTIMER);
                this.session.addSessionStateListener(new SessionStateListenerImpl());
                if (SendSMS1.this.bindType.IsReceivable()) {
                    this.session.setMessageReceiverListener(new MessageReceiverListenerImpl());
                }
                this.bindParameter = new BindParameter(SendSMS1.this.bindType, this.SMPP_USER_ID, this.SMPP_PASSWORD, "TR", TypeOfNumber.ALPHANUMERIC, NumberingPlanIndicator.UNKNOWN, null);
                this.session.connectAndBind(this.SMPP_IP_ADDRESS, this.SMPP_PORT, this.bindParameter);
            } catch (IOException e) {
                System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + this.SESSION_ID + " Run Exception " + e.getMessage() + "\n");
                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "Run Exception " + e.getMessage());
            }
            System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + this.SESSION_ID + " CONNECT\n");
            System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + this.SESSION_ID + " SEND START...\n");
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

        private Runnable newSendTask(final long tran_cd, final String source, final String destination, final String message, final boolean ab_flash, final boolean ab_unicode, final boolean ab_scheduler, final String scheduleDeliveryDt, final String trntype, final int send_part, final int ref_no) {
            return new Runnable() {
                @Override
                public void run() {
                    String ls_messageId = null;
                    String scheduleDeliveryTime = "";
                    String validityPeriod = "";
                    try {
                        if (ab_scheduler) {
                            try {
                                final Date scheduler_dt = SMSSession.this.stringRelativeTimeFormatter.parse(scheduleDeliveryDt);
                                scheduleDeliveryTime = SendSMS1.this.absoluteTimeFormatter.format(scheduler_dt);
                                validityPeriod = SendSMS1.this.validityPeriodFormatter.format(scheduler_dt, SendSMS1.this.VALIDITY_PERIOD);
                                scheduleDeliveryTime = "";
                            } catch (ParseException ex4) {
                            }
                        } else if (SendSMS1.this.VALIDITY_PERIOD > 0) {
                            final Date targetTime = new Date();
                            validityPeriod = SendSMS1.this.validityPeriodFormatter.format(targetTime, SendSMS1.this.VALIDITY_PERIOD);
                        }
                        for (int li_loop = 1; li_loop <= SendSMS1.this.RETRY_ATTEMPT; ++li_loop) {
                            try {
                                ls_messageId = SMSSession.this.SendSms(tran_cd, trntype, source, destination, ab_flash, ab_unicode, message, scheduleDeliveryTime, validityPeriod, send_part, ref_no);
                            } catch (ResponseTimeoutException ex) {
                                //  In case of submit timeout, so when internet connection is lost after
                                //  submitting an SMS, you cannot tell whether the SMS was sent or not.
                                //  The service provider will not give back any response.
                                System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  ResponseTimeoutException " + ex.getMessage() + "\n");
                                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "ResponseTimeoutException " + ex.getMessage());
                            }
                            System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SEND SMS MOBILE NO: " + destination + " SENDER ID " + source + " SEND ATTEMPT " + li_loop + " MESSAGE ID " + ls_messageId + "\n");
                            SMSSession.this.UpdateSendAttempt(tran_cd, trntype);
                            if (ls_messageId != null) {
                                SMSSession.this.UpdateMessageId(tran_cd, trntype, ls_messageId.toUpperCase());
                                break;
                            }
                            if (li_loop == SendSMS1.this.RETRY_ATTEMPT) {
                                SMSSession.this.RejectMessage(tran_cd, trntype, 8);
                            }
                        }
                    } catch (IOException ex2) {
                        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  IOException Message Ref. No: " + tran_cd + " " + ex2.getMessage() + "\n");
                        SMSSession.this.SetMessagePending(tran_cd, trntype);
                        dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "IOException Message Ref. No: " + tran_cd + " " + ex2.getMessage());
                    } catch (NegativeResponseException ex3) {
                        if (ex3.getCommandStatus() == 88) { //Throttling error (ESME has exceeded allowed message limits)
                            SMSSession.this.SetMessagePending(tran_cd, trntype);
                        } else { // 168 DND Block, 1009 Black Listed Number, 11 Invalid destination address, 300 Incorrect destination address
                            SMSSession.this.RejectMessage(tran_cd, trntype, ex3.getCommandStatus());
                        }
                        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  NegativeResponseException " + ex3.getMessage() + "\n");
                        dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, " NegativeResponseException " + ex3.getMessage());
                    }
                }
            };
        }

        private String SendSms(final long tran_cd, final String trntype, final String source, final String destination, final boolean ab_flash, final boolean ab_unicode, final String message, final String scheduleDeliveryTime, final String validityPeriod, final int send_part, final int ref_no) throws IOException, NegativeResponseException, ResponseTimeoutException {
            String ls_message_Id = null;
            final MessageClass messageClass = MessageClass.CLASS1;
            Alphabet alphabet = null;
            int maximumSingleMessageSize = 0;
            int maximumMultipartMessageSegmentSize = 0;
            byte[] byteSingleMessage = null;
            if (ab_unicode) {
                alphabet = (ab_flash ? Alphabet.ALPHA_UCS2_FLASH : Alphabet.ALPHA_UCS2);
                byteSingleMessage = message.getBytes("UTF-16BE");
                maximumSingleMessageSize = SendSMS1.this.MAX_SINGLE_MSG_SEGMENT_SIZE_UCS2;
                maximumMultipartMessageSegmentSize = SendSMS1.this.MAX_MULTIPART_MSG_SEGMENT_SIZE_UCS2;
            } else {
                alphabet = (ab_flash ? Alphabet.ALPHA_FLASH : Alphabet.ALPHA_DEFAULT);
                byteSingleMessage = message.getBytes();
                maximumSingleMessageSize = SendSMS1.this.MAX_SINGLE_MSG_SEGMENT_SIZE_7BIT;
                maximumMultipartMessageSegmentSize = SendSMS1.this.MAX_MULTIPART_MSG_SEGMENT_SIZE_7BIT;
            }
            byte[][] byteMessagesArray = null;
            ESMClass esmClass = null;
            if (message.length() > maximumSingleMessageSize) {
                byteMessagesArray = this.splitUnicodeMessage(byteSingleMessage, maximumMultipartMessageSegmentSize, tran_cd, trntype, ref_no);
                esmClass = new ESMClass(MessageMode.DEFAULT, MessageType.DEFAULT, GSMSpecificFeature.UDHI);
            } else {
                byteMessagesArray = new byte[][]{byteSingleMessage};
                esmClass = new ESMClass();
            }
            for (int i = send_part; i <= byteMessagesArray.length; ++i) {
                final long startTime = System.currentTimeMillis();
                SendSMS1.this.requestCounter.incrementAndGet();
                ls_message_Id = this.submitMessage(byteMessagesArray[i - 1], source, destination, messageClass, alphabet, esmClass, scheduleDeliveryTime, validityPeriod);
                this.UpdateSendPart(tran_cd, trntype);
                final long delay = System.currentTimeMillis() - startTime;
                SendSMS1.this.responseCounter.incrementAndGet();
                if (SendSMS1.this.maxDelay.get() < delay) {
                    SendSMS1.this.maxDelay.set(delay);
                }
            }
            return ls_message_Id;
        }

        private String submitMessage(final byte[] message, final String source, final String destination, final MessageClass messageClass, final Alphabet alphabet, final ESMClass esmClass, final String scheduleDeliveryTime, final String validityPeriod) throws IOException, NegativeResponseException, ResponseTimeoutException {
            String messageId = null;
            try {
              messageId = session.submitShortMessage("CMT", TypeOfNumber.ALPHANUMERIC, NumberingPlanIndicator.UNKNOWN,
                        source, TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.ISDN, destination, esmClass,
                        (byte) 0, (byte) 1, scheduleDeliveryTime, validityPeriod, new RegisteredDelivery(SMSCDeliveryReceipt.SUCCESS_FAILURE),
                        (byte) 0, new GeneralDataCoding(alphabet, esmClass), (byte) 0, message);
             } catch (PDUException e) {
                System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  PDUException " + e.getMessage() + "\n");
            } catch (InvalidResponseException e2) {
                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "InvalidResponseException " + e2.getMessage());
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

        private void UpdateSendAttempt(final long al_tran_cd, final String as_trn_type) {
            Connection con = null;
            Statement upd_stmt = null;
            try {
                con = dbconfig.GetConncetion();
                upd_stmt = con.createStatement();
                final String sql = "UPDATE sms_trn SET SEND_ATTEMPT = SEND_ATTEMPT + 1 WHERE TRAN_ID =" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
                final int rows = upd_stmt.executeUpdate(sql);
                upd_stmt.close();
                con.close();
            } catch (SQLException ex) {
                System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  UpdateSendAttempt SQLException " + ex.getMessage() + "\n");
                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "UPADATE SQLException " + ex.getMessage());
            } finally {
                if (upd_stmt != null) {
                    try {
                        upd_stmt.close();
                    } catch (SQLException ex2) {
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex3) {
                    }
                }
            }
        }

        private void UpdateSendPart(final long al_tran_cd, final String as_trn_type) {
            Connection con = null;
            Statement upd_stmt = null;
            try {
                con = dbconfig.GetConncetion();
                upd_stmt = con.createStatement();
                final String sql = "UPDATE sms_trn SET SEND_PART = SEND_PART + 1 WHERE TRAN_ID =" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
                final int rows = upd_stmt.executeUpdate(sql);
                upd_stmt.close();
                con.close();
            } catch (SQLException ex) {
                System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  UpdateSendAttempt SQLException " + ex.getMessage() + "\n");
                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "UPADATE SQLException " + ex.getMessage());
            } finally {
                if (upd_stmt != null) {
                    try {
                        upd_stmt.close();
                    } catch (SQLException ex2) {
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex3) {
                    }
                }
            }
        }

        private void UpdateRefNo(final long al_tran_cd, final String as_trn_type, final int ref_no) {
            Connection con = null;
            Statement upd_stmt = null;
            try {
                con = dbconfig.GetConncetion();
                upd_stmt = con.createStatement();
                final String sql = "UPDATE sms_trn SET REF_NO ='" + ref_no + "' WHERE TRAN_ID=" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
                final int rows = upd_stmt.executeUpdate(sql);
                upd_stmt.close();
                con.close();
            } catch (SQLException ex) {
                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "UPADATE SQLException " + ex.getMessage());
            } finally {
                if (upd_stmt != null) {
                    try {
                        upd_stmt.close();
                    } catch (SQLException ex2) {
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex3) {
                    }
                }
            }
        }

        private void UpdateMessageId(final long al_tran_cd, final String as_trn_type, final String as_messageId) {
            Connection con = null;
            Statement upd_stmt = null;
            try {
                con = dbconfig.GetConncetion();
                upd_stmt = con.createStatement();
                final String sql = "UPDATE sms_trn SET SMS_STATUS='Y',SEND_DT = SYSDATE(), MESSAGE_ID = '" + as_messageId + "' WHERE TRAN_ID=" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
                final int rows = upd_stmt.executeUpdate(sql);
                upd_stmt.close();
                con.close();
            } catch (SQLException ex) {
                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "UPADATE SQLException " + ex.getMessage());
            } finally {
                if (upd_stmt != null) {
                    try {
                        upd_stmt.close();
                    } catch (SQLException ex2) {
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex3) {
                    }
                }
            }
        }

        private void RejectMessage(final long al_tran_cd, final String as_trn_type, final int error_cd) {
            Connection con = null;
            Statement upd_stmt = null;
            try {
                con = dbconfig.GetConncetion();
                upd_stmt = con.createStatement();
                final String sql = "UPDATE sms_trn SET SMS_STATUS = 'Y', ERROR_CD ='" + error_cd + "', SEND_DT = SYSDATE() WHERE DELIVERY_STATUS = 'P' AND TRAN_ID=" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
                final int rows = upd_stmt.executeUpdate(sql);
                upd_stmt.close();
                con.close();
            } catch (SQLException ex) {
                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "UPADATE SQLException " + ex.getMessage());
            } finally {
                if (upd_stmt != null) {
                    try {
                        upd_stmt.close();
                    } catch (SQLException ex2) {
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex3) {
                    }
                }
            }
        }

        private void SetMessagePending(final long al_tran_cd, final String as_trn_type) {
            Connection con = null;
            Statement upd_stmt = null;
            try {
                con = dbconfig.GetConncetion();
                upd_stmt = con.createStatement();
                final String sql = "UPDATE sms_trn SET SMS_STATUS = 'P' WHERE TRAN_ID=" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
                final int rows = upd_stmt.executeUpdate(sql);
                upd_stmt.close();
                con.close();
            } catch (SQLException ex) {
                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "SetMessagePending SQLException " + ex.getMessage());
            } finally {
                if (upd_stmt != null) {
                    try {
                        upd_stmt.close();
                    } catch (SQLException ex2) {
                    }
                }
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex3) {
                    }
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
            final SMPPSession tmpSession = new SMPPSession();
            tmpSession.setPduProcessorDegree(SMPP_TPS * SESSION);
            tmpSession.setTransactionTimer(SendSMS1.this.SMPP_TRANSACTIONTIMER);
            tmpSession.addSessionStateListener(new SessionStateListenerImpl());
            if (SendSMS1.this.bindType.IsReceivable()) {
                tmpSession.setMessageReceiverListener(new MessageReceiverListenerImpl());
            }
            this.bindParameter = new BindParameter(SendSMS1.this.bindType, this.SMPP_USER_ID, this.SMPP_PASSWORD, "TR", TypeOfNumber.ALPHANUMERIC, NumberingPlanIndicator.UNKNOWN, null);
            tmpSession.connectAndBind(this.SMPP_IP_ADDRESS, this.SMPP_PORT, this.bindParameter);
            return tmpSession;
        }

        public void unbindAndClose() {
            System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + this.SESSION_ID + " SEND STOP...\n");
            this.session.unbindAndClose();
        }

        private class MessageReceiverListenerImpl implements MessageReceiverListener {

            private static final String DATASM_NOT_IMPLEMENTED = "data_sm not implemented";

            @Override
            public void onAcceptDeliverSm(final DeliverSm deliverSm) throws ProcessRequestException {
                if (MessageType.SMSC_DEL_RECEIPT.containedIn(deliverSm.getEsmClass())) {
                    try {
                        final DeliveryReceipt delReceipt = deliverSm.getShortMessageAsDeliveryReceipt();
                        final String messageId = delReceipt.getId().toUpperCase();
                        SendSMS1.this.sms_dlr.add(new DLR(deliverSm.getDestAddress(), deliverSm.getSourceAddr(), messageId, delReceipt.getSubmitted(), delReceipt.getDelivered(), delReceipt.getSubmitDate(), delReceipt.getDoneDate(), delReceipt.getFinalStatus(), delReceipt.getError(), delReceipt.getText()));
                        System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  DLR RECEIVE " + deliverSm.getSourceAddr() + " " + deliverSm.getDestAddress() + " " + messageId + "\n");
                    } catch (InvalidDeliveryReceiptException e) {
                        dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "InvalidDeliveryReceiptException " + e.getMessage());
                    }
                } else {
                    dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "Receiving message " + new String(deliverSm.getShortMessage()));
                }
            }

            @Override
            public void onAcceptAlertNotification(final AlertNotification alertNotification) {
                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "onAcceptAlertNotification " + alertNotification.toString());
            }

            @Override
            public DataSmResult onAcceptDataSm(final DataSm dataSm, final Session source) throws ProcessRequestException {
                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "onAcceptDataSm " + dataSm.toString());
                throw new ProcessRequestException("data_sm not implemented", 3);
            }
        }

        private class SessionStateListenerImpl implements SessionStateListener {

            @Override
            public void onStateChange(final SessionState newState, final SessionState oldState, final Session source) {
                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "ON STATE CHANGE NEW STATE " + newState.toString() + " OLD STATE " + oldState.toString());
                System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  ON STATE CHANGE NEW STATE " + newState.toString() + " OLD STATE " + oldState.toString() + "\n");
                if (newState.equals(SessionState.CLOSED) && (oldState.equals(SessionState.BOUND_TX) || oldState.equals(SessionState.BOUND_RX) || oldState.equals(SessionState.BOUND_TRX))) {
                    dbconfig.WriteLog("Y", ERROR_LOG_PATH, SendSMS1.this.SMPP, "SMPP SESSION " + SMSSession.this.SESSION_ID + " DISCONNECT");
                    System.out.print(SendSMS1.TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SMSSession.this.SESSION_ID + " DISCONNECT\n");
                    SMSSession.this.reconnectAfter(10000L);
                }
            }
        }
    }
}
