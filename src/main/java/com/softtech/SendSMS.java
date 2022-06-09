package com.softtech;

import com.google.communication.businessmessaging.verifiedsms.v1.RecipientAndMessage;
import com.google.communication.businessmessaging.verifiedsms.v1.VerifiedSmsCompletionCallbackWithResult;
import com.google.communication.businessmessaging.verifiedsms.v1.VerifiedSmsServiceClient;
import com.softtech.smpp.InvalidResponseException;
import com.softtech.smpp.PDUException;
import com.softtech.smpp.SMPPConstant;
import com.softtech.smpp.bean.*;
import com.softtech.smpp.extra.NegativeResponseException;
import com.softtech.smpp.extra.ProcessRequestException;
import com.softtech.smpp.extra.ResponseTimeoutException;
import com.softtech.smpp.extra.SessionState;
import com.softtech.smpp.session.*;
import com.softtech.smpp.util.AESEncryption;
import com.softtech.smpp.util.AbsoluteTimeFormatter;
import com.softtech.smpp.util.InvalidDeliveryReceiptException;
import com.softtech.smpp.util.ValidityPeriodFormatter;
import java.io.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author SUTHAR
 */
public class SendSMS extends Thread {

    private static int ROUTE_ID = 0;

    private final SetBlockingQueue sms_queue = new SetBlockingQueue();
    private final SetBlockingQueueDlr sms_dlr = new SetBlockingQueueDlr();
    private List<SMS> SmsDrainList;
    private List<DLR> DlrDrainList;

    private final AtomicInteger requestCounter = new AtomicInteger();
    private final AtomicInteger responseCounter = new AtomicInteger();
    private final AtomicLong totalRequestCounter = new AtomicLong();
    private final AtomicLong totalResponseCounter = new AtomicLong();
    private final AtomicLong maxDelay = new AtomicLong();

    private final AbsoluteTimeFormatter absoluteTimeFormatter = new AbsoluteTimeFormatter();
    private final ValidityPeriodFormatter validityPeriodFormatter = new ValidityPeriodFormatter();

    private final MessageReceiverListenerImpl MessageReceiverListener = new MessageReceiverListenerImpl();

    static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
    static SimpleDateFormat stringRelativeTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static SimpleDateFormat TimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");

    private Connection conn = null;
    private boolean[] threadIsAlive;
    private SMSSession[] smsSession;
    private DeliveryReceiptThread[] DlrSession;

    public SendSMS(int route_id) {
        ROUTE_ID = route_id;
        System.out.print(TimeFormatter.format(new Date()) + " --:  Initialising...\n");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        stringRelativeTimeFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public SendSMS() {
        System.out.print(TimeFormatter.format(new Date()) + " --:  Initialising...\n");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        stringRelativeTimeFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Override
    public void run() {

        if (Common.LoadSetUpValues(ROUTE_ID)) {
            System.out.print(TimeFormatter.format(new Date()) + " --:  ===========================================\n");
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP_IP_ADDRESS: " + Common.SMPP_IP_ADDRESS + "\n");
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP_PORT: " + Common.SMPP_PORT + "\n");
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP_USER_ID: " + Common.SMPP_USER_ID + "\n");
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP_PASSWORD: " + Common.SMPP_PASSWORD + "\n");
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP_MODE: " + Common.SMPP_MODE + "\n");
            System.out.print(TimeFormatter.format(new Date()) + " --:  ROUTE_ID: " + ROUTE_ID + "\n");
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP_TPS: " + Common.SMPP_TPS + "\n");
            System.out.print(TimeFormatter.format(new Date()) + " --:  RETRY_ATTEMPT: " + Common.RETRY_ATTEMPT + "\n");
            System.out.print(TimeFormatter.format(new Date()) + " --:  ===========================================\n");

            File privateKeyPath = new File(Common.VSMS_PRIVATE_KEY_PATH);
            File serviceAccountJson = new File(Common.VSMS_SERVICE_ACCOUNT_JSON);
            if (privateKeyPath.exists() && serviceAccountJson.exists()) {
                try {
                    Common.VSMS_SERVICE_CLIENT = new VerifiedSmsServiceClient.Builder()
                            .setServiceAccountKeyStream(new FileInputStream(Common.VSMS_SERVICE_ACCOUNT_JSON))
                            .build();
                    Common.VSMS_PUBLIC_KEY = Files.readAllBytes(Paths.get(Common.VSMS_PRIVATE_KEY_PATH));
                    System.out.println(TimeFormatter.format(new Date()) + " --:  VSMS ENABLED \n");
                } catch (Exception ex) {
                    Common.VSMS_SERVICE_CLIENT = null;
                    Common.VSMS_PUBLIC_KEY = null;
                }
            }

            this.smsSession = new SMSSession[Common.SESSION];
            this.threadIsAlive = new boolean[Common.SESSION];
            this.DlrSession = new DeliveryReceiptThread[1];

            for (int i = 0; i < this.smsSession.length; i++) {
                this.smsSession[i] = new SMSSession(Common.SMPP_IP_ADDRESS, Common.SMPP_PORT, Common.SMPP_USER_ID, Common.SMPP_PASSWORD, i + 1, SendSMS.ROUTE_ID);
            }

            for (int j = 0; j < this.smsSession.length; j++) {
                this.smsSession[j].start();
                try {
                    // On my workstation, anything over about 500ms gap between
                    // starting threads masks the problem (gives the PDU
                    // init code enough time to initialise):
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                this.threadIsAlive[j] = true;
            }

            System.out.print(SendSMS.TimeFormatter.format(new Date()) + " --:  Initialising complete\n");
            new ThreadWatcherThread().start();

            if (Common.bindType.IsTransmittable() && Common.bindType.IsReceivable()) {
                for (int i = 0; i < this.DlrSession.length; i++) {
                    (this.DlrSession[i] = new DeliveryReceiptThread()).start();
                }
                new TrafficWatcherThread().start();
                this.sendProcess();
            } else if (!Common.bindType.IsTransmittable() && Common.bindType.IsReceivable()) {
                for (int i = 0; i < this.DlrSession.length; i++) {
                    (this.DlrSession[i] = new DeliveryReceiptThread()).start();
                }
                while (Common.IS_RUN) {
                    try {
                        Thread.sleep(5000L);
                    } catch (InterruptedException e) {
                    }
                }
            } else if (Common.bindType.IsTransmittable() && !Common.bindType.IsReceivable()) {
                new TrafficWatcherThread().start();
                this.sendProcess();
            }

            DlrDrainList = new ArrayList<>();
            sms_dlr.drainTo(DlrDrainList);
            for (DLR dlr : DlrDrainList) {
                System.out.print(TimeFormatter.format(new Date()) + " --:  DlrDrainList " + dlr.getMESSAGE_ID() + "\n");
                Common.InsertDeliverSm(dlr.getMOBILE_NO(), dlr.getSENDER_ID(), dlr.getMESSAGE_ID(), dlr.getSUB(), dlr.getDELIVER(), dlr.getSUBMIT_DT(), dlr.getDONE_DT(), dlr.getDELIVERY_STATUS(), dlr.getERROR(), dlr.getTEXT(), ROUTE_ID);
            }

            SmsDrainList = new ArrayList<>();
            sms_queue.drainTo(SmsDrainList);
            for (SMS sms : SmsDrainList) {
                System.out.print(TimeFormatter.format(new Date()) + " --:  SmsDrainList " + sms.getTran_cd() + "\n");
                Common.SetMessagePending(sms.getTran_cd(), sms.getTrntype());
            }

            for (int i = 0; i < smsSession.length; i++) {
                if (threadIsAlive[i] && smsSession[i].isAlive()) {
                    smsSession[i].unbindAndClose();
                    threadIsAlive[i] = false;
                }
            }
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP ALL SESSION DEAD\n");
            dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "SMPP STOP");
            System.exit(0);
        }
    }

    private void sendProcess() {
        ResultSet rs;
        Statement sql_stmt;
        String ls_sql;
        boolean lb_row_found;
        boolean lb_next;
        AtomicLong ll_cnt = new AtomicLong();
        AtomicLong smsCounter = new AtomicLong();
        ArrayList<String> SELLER_IDs = new ArrayList<>();
        ArrayList<String> USER_IDs = new ArrayList<>();
        ArrayList<String> TRN_TYPEs = new ArrayList<>();
        long noOfsms;

        while (Common.IS_RUN) {

            lb_row_found = false;

            ls_sql = "SELECT SELLER_ID,USER_ID,TRN_TYPE,NO_OF_SMS FROM (SELECT SELLER_ID,USER_ID,TRN_TYPE, COUNT(NO_OF_SMS) AS NO_OF_SMS FROM sms_trn "
                    + "WHERE SMS_STATUS = 'P' AND SCHEDULER_DT <= SYSDATE() AND ROUTE_ID = " + ROUTE_ID
                    + " AND SMS_TYPE = '" + Common.SMS_TYPE + "' ";
            ls_sql += Common.TRN_TYPE.equals("") ? "" : " AND TRN_TYPE IN (" + Common.TRN_TYPE + ") ";
            ls_sql += " GROUP BY SELLER_ID,USER_ID,TRN_TYPE) tbl ORDER BY TRN_TYPE ASC,NO_OF_SMS ASC";

            try {
                if (!dbconfig.isDbConnected(conn)) {
                    try {
                        conn = dbconfig.GetConncetion();
                    } catch (Exception ex) {
                        System.out.print(TimeFormatter.format(new Date()) + " --:  CreateStatement Exception " + ex.getMessage() + "\n");
                        dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "CreateStatement Exception " + ex.getMessage());
                    }
                    ll_cnt.set(0);
                } else if (ll_cnt.get() > 500) {
                    try {
                        conn = dbconfig.GetConncetion();
                    } catch (Exception ex) {
                        System.out.print(TimeFormatter.format(new Date()) + " --:  CreateStatement Exception " + ex.getMessage() + "\n");
                        dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "CreateStatement Exception " + ex.getMessage());
                    }
                    ll_cnt.set(0);
                } else {
                    try {
                        sql_stmt = conn.createStatement();
                        rs = sql_stmt.executeQuery(ls_sql);

                        while (rs.next()) { //Send SMS

                            noOfsms = smsCounter.addAndGet(rs.getLong("NO_OF_SMS"));

                            USER_IDs.add("'" + rs.getString("USER_ID") + "'");
                            if (!SELLER_IDs.contains("'" + rs.getString("SELLER_ID") + "'")) {
                                SELLER_IDs.add("'" + rs.getString("SELLER_ID") + "'");
                            }
                            if (!TRN_TYPEs.contains("'" + rs.getString("TRN_TYPE") + "'")) {
                                TRN_TYPEs.add("'" + rs.getString("TRN_TYPE") + "'");
                            }

                            lb_row_found = true;
                            lb_next = rs.next();

                            if (!lb_next || noOfsms >= Common.SELECT_LIMIT) {
                                SendSms(String.join(",", SELLER_IDs), String.join(",", USER_IDs), String.join(",", TRN_TYPEs));
                                smsCounter.set(0);
                                SELLER_IDs.clear();
                                USER_IDs.clear();
                                TRN_TYPEs.clear();
                            }
                            if (lb_next) {
                                rs.previous();
                            }
                        }
                        rs.close();
                        sql_stmt.close();
                    } catch (SQLException ex) {
                        System.out.print(TimeFormatter.format(new Date()) + " --:  SendProcess SQLException " + ex.getMessage() + "\n");
                        dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, " SendProcess SQLException " + ex.getMessage());
                    } catch (Exception ex) {
                        System.out.print(TimeFormatter.format(new Date()) + " --:  SendProcess Exception " + ex.getMessage() + "\n");
                        dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, " SendProcess Exception " + ex.getMessage());
                    }
                    if (!lb_row_found) {
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    ll_cnt.getAndIncrement();
                }
            } catch (Exception ex) {
                System.out.print(TimeFormatter.format(new Date()) + " --:  Exception " + ex.getMessage() + "\n");
                dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "Exception " + ex.getMessage());
            }
            System.gc();
        }
    }

    private void SendSms(String SELLER_ID, String USER_ID, String TRN_TYPE) {
        String ls_sql_sms;
        String ls_postbackData = null;
        String ls_message = null;
        Statement sms_stmt;
        ResultSet rs_sms;
        ArrayList<String> TRAN_IDs = new ArrayList<>();
        ArrayList<String> TRN_TYPEs = new ArrayList<>();
        List<RecipientAndMessage> recipientsAndMessages = new ArrayList<RecipientAndMessage>();

//        ls_sql_sms = "SELECT TRAN_ID,TRAN_DT,SELLER_ID,USER_ID,SENDER_ID,MOBILE_NO,MESSAGE,UNICODE,MESSAGE_TYPE,SCHEDULER,SCHEDULER_DT,SEND_PART,REF_NO,NO_OF_SMS,TRN_TYPE,IFNULL(TEMPLATE_ID,'') AS TEMPLATE_ID,IFNULL(PE_ID,'') AS PE_ID,IFNULL(TELEMARKETER_ID,'') AS TELEMARKETER_ID, '' AS VSMS_AGENT_ID FROM sms_trn "
//                + " WHERE SELLER_ID IN (" + SELLER_ID + ") AND USER_ID IN (" + USER_ID + ") AND SMS_STATUS = 'P' "
//                + " AND TRN_TYPE IN (" + TRN_TYPE + ") AND SCHEDULER_DT <= SYSDATE() "
//                + " AND ROUTE_ID = " + ROUTE_ID + " AND SMS_TYPE = '" + Common.SMS_TYPE + "' ORDER BY TRN_TYPE ASC,TRAN_DT ASC LIMIT " + Common.SELECT_LIMIT;
        ls_sql_sms = "SELECT S.TRAN_ID,S.TRAN_DT,S.SELLER_ID,S.USER_ID,S.SENDER_ID,S.MOBILE_NO,S.MESSAGE,S.UNICODE,S.MESSAGE_TYPE,S.SCHEDULER,S.SCHEDULER_DT,S.SEND_PART,S.REF_NO,S.NO_OF_SMS,S.TRN_TYPE,IFNULL(S.TEMPLATE_ID,'') AS TEMPLATE_ID,IFNULL(S.PE_ID,'') AS PE_ID,IFNULL(S.TELEMARKETER_ID,'') AS TELEMARKETER_ID, IFNULL(U.VSMS_AGENT_ID,'') AS VSMS_AGENT_ID "
                + " FROM sms_trn S ,user_mst U WHERE S.SELLER_ID = U.SELLER_ID AND S.USER_ID = U.USER_ID AND S.SELLER_ID IN (" + SELLER_ID + ") AND S.USER_ID IN (" + USER_ID + ") AND S.SMS_STATUS = 'P' "
                + " AND S.TRN_TYPE IN (" + TRN_TYPE + ") AND S.SCHEDULER_DT <= SYSDATE() "
                + " AND S.ROUTE_ID = " + ROUTE_ID + " AND S.SMS_TYPE = '" + Common.SMS_TYPE + "' ORDER BY S.TRN_TYPE ASC, S.TRAN_DT ASC LIMIT " + Common.SELECT_LIMIT;

        try {
            sms_stmt = conn.createStatement();
            rs_sms = sms_stmt.executeQuery(ls_sql_sms);

            while (rs_sms.next()) {

                sms_queue.add(new SMS(rs_sms.getLong("TRAN_ID"),
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
                        rs_sms.getString("TRN_TYPE"),
                        rs_sms.getString("TEMPLATE_ID"),
                        rs_sms.getString("PE_ID"),
                        rs_sms.getString("TELEMARKETER_ID"),
                        (rs_sms.getString("TRN_TYPE").equalsIgnoreCase("O") ? "A" : rs_sms.getString("TRN_TYPE").equalsIgnoreCase("T") ? "B" : "C")
                ));

                TRAN_IDs.add("'" + rs_sms.getString("TRAN_ID") + "'");
                if (!TRN_TYPEs.contains("'" + rs_sms.getString("TRN_TYPE") + "'")) {
                    TRN_TYPEs.add("'" + rs_sms.getString("TRN_TYPE") + "'");
                }

                //VSMS
                if (Common.VSMS_SERVICE_CLIENT != null && rs_sms.getString("VSMS_AGENT_ID").trim().length() > 0 && (rs_sms.getString("TRN_TYPE").equalsIgnoreCase("O") || rs_sms.getString("TRN_TYPE").equalsIgnoreCase("T"))) {
                    ls_postbackData = rs_sms.getString("TRAN_ID") + "|" + rs_sms.getString("MOBILE_NO") + "|" + rs_sms.getString("TRN_TYPE");
                    ls_message = rs_sms.getString("MESSAGE").trim();
                    if (rs_sms.getString("TRN_TYPE").equalsIgnoreCase("O")) {
                        try {
                            ls_message = AESEncryption.decryptText(ls_message, rs_sms.getString("SENDER_ID").trim(), rs_sms.getString("MOBILE_NO").trim());
                        } catch (Exception ex) {
                        }
                    }
                    recipientsAndMessages.add(new RecipientAndMessage("+" + rs_sms.getString("MOBILE_NO").trim(), ls_message, ls_postbackData, rs_sms.getString("VSMS_AGENT_ID")));
                }
            }

            Common.SetMessageSendState(String.join(",", TRAN_IDs), String.join(",", TRN_TYPEs));

            //VSMS
            if (recipientsAndMessages.size() > 0) {
                Common.VSMS_SERVICE_CLIENT.createHashes(recipientsAndMessages, Common.VSMS_PUBLIC_KEY,
                        new VerifiedSmsCompletionCallbackWithResult<Collection<String>>() {
                    @Override
                    public void onSuccess(Collection<String> reachablePhoneNumbers) {
                        // TODO: Send actual SMS messages
                        System.out.println(TimeFormatter.format(new Date()) + " --: reachablePhoneNumbers : " + reachablePhoneNumbers + "\n");
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        System.out.println(TimeFormatter.format(new Date()) + " --:  createHashes  Throwable : " + t.getMessage() + "\n");
                    }
                });
            }

            rs_sms.close();
            sms_stmt.close();
        } catch (SQLException ex) {
            System.out.print(TimeFormatter.format(new Date()) + " --:  SEND SendSms SQLException " + ex.getMessage() + "\n");
            dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "SEND SendSms SQLException " + ex.getMessage());
        } catch (Exception ex) {
            System.out.print(TimeFormatter.format(new Date()) + " --:  SEND SendSms Exception " + ex.getMessage() + "\n");
            dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "SEND SendSms Exception " + ex.getMessage());
        }
    }

    private class ThreadWatcherThread extends Thread {

        private ThreadWatcherThread() {
            super("ThreadWatcherThread: " + SendSMS.this);
        }

        @Override
        public void run() {
            System.out.print(TimeFormatter.format(new Date()) + " --:  Starting ThreadWatcherThread\n");
            while (Common.LoadSetUpValues(ROUTE_ID)) {
                for (int i = 0; i < smsSession.length; i++) {
                    if (threadIsAlive[i] && !smsSession[i].isAlive()) {
                        //smsSession[i].interrupt();
                        threadIsAlive[i] = false;
                        System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION: " + i + " IS DEAD\n");
                        smsSession[i] = new SMSSession(Common.SMPP_IP_ADDRESS, Common.SMPP_PORT, Common.SMPP_USER_ID, Common.SMPP_PASSWORD, i + 1, ROUTE_ID);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                        }
                        smsSession[i].start();
                        System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION: " + i + " START\n");
                        threadIsAlive[i] = true;
                    }
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {
                }

                System.gc();
                dbconfig.WriteStatus(Common.STATUS_LOG_PATH, "SMPP IS RUNNING...");
            }
            interrupt();
        }
    }

    private class TrafficWatcherThread extends Thread {

        private TrafficWatcherThread() {
            super("TrafficWatcherThread: " + SendSMS.this);
        }

        @Override
        public void run() {
            if (Common.bindType.IsTransmittable()) {
                System.out.print(SendSMS.TimeFormatter.format(new Date()) + " --:  Starting traffic watcher...\n");
                while (Common.IS_RUN) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ex) {
                    }
                    final int requestPerSecond = SendSMS.this.requestCounter.getAndSet(0);
                    final int responsePerSecond = SendSMS.this.responseCounter.getAndSet(0);
                    final long maxDelayPerSecond = SendSMS.this.maxDelay.getAndSet(0L);
                    SendSMS.this.totalRequestCounter.addAndGet(requestPerSecond);
                    final long total = SendSMS.this.totalResponseCounter.addAndGet(responsePerSecond);
                    System.out.print(SendSMS.TimeFormatter.format(new Date()) + " --:  Request/Response per second: " + requestPerSecond + "/" + responsePerSecond + " of " + total + " maxDelay=" + maxDelayPerSecond + "\n");
                }
                interrupt();
            }
        }
    }

    private class DeliveryReceiptThread extends Thread {

        private DeliveryReceiptThread() {
            super("DeliveryReceiptThread: " + SendSMS.this);
        }

        @Override
        public void run() {
            String ls_Query = "INSERT INTO sms_response_mst (`MOBILE_NO`,`SENDER_ID`,`MESSAGE_ID`,`SUB`,`DELIVER`,`SUBMIT_DT`,`DONE_DT`,`DELIVERY_STATUS`,`ERROR`,`TEXT`,`ROUTE_ID`) VALUES ";
            String ls_sql = "";
            int count = 0;
            Connection db_conn = null;
            Statement ins_stmt = null;
            long startTime = System.currentTimeMillis();
            if (Common.bindType.IsReceivable()) {
                System.out.print(TimeFormatter.format(new Date()) + " --:  Starting DeliveryReceiptThread\n");
                while (Common.IS_RUN) {
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
                                    + "'" + dlr.getSUBMIT_DT() + "','" + dlr.getDONE_DT() + "','" + dlr.getDELIVERY_STATUS() + "','" + dlr.getERROR() + "',''," + ROUTE_ID + ")";
                            try {
                                db_conn = dbconfig.GetConncetion();
                                ins_stmt = db_conn.createStatement();
                                ins_stmt.executeUpdate(ls_Query + ls_sql);
                                ins_stmt.close();
                                db_conn.close();
                                System.out.print(TimeFormatter.format(new Date()) + " --:  DLR INSERT " + count + "\n");
                            } catch (SQLException ex) {
                                dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "SQLException " + ex.getMessage());
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
                                    + "'" + dlr.getSUBMIT_DT() + "','" + dlr.getDONE_DT() + "','" + dlr.getDELIVERY_STATUS() + "','" + dlr.getERROR() + "',''," + ROUTE_ID + "),";
                        }
                        count++;
//                        InsertDeliverSm(dlr.getMOBILE_NO(), dlr.getSENDER_ID(), dlr.getMESSAGE_ID(), dlr.getSUB(), dlr.getDELIVER(), dlr.getSUBMIT_DT(), dlr.getDONE_DT(), dlr.getDELIVERY_STATUS(), dlr.getERROR(), dlr.getTEXT());                        
                    } catch (Exception ex) {
                        dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "Try Exception " + ex.getMessage());
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
            }
            interrupt();
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
        private ThreadPoolExecutor executorService;

        public SMSSession(final String SMPP_IP_ADDRESS, final int SMPP_PORT, final String SMPP_USER_ID, final String SMPP_PASSWORD, final int SESSION_ID, final int ROUTE_ID) {
            super("SMSSession: " + SendSMS.this);
            this.session = null;
            this.bindParameter = null;
            this.SMPP_IP_ADDRESS = "";
            this.SMPP_PORT = 0;
            this.SMPP_USER_ID = "";
            this.SMPP_PASSWORD = "";
            this.SESSION_ID = 0;
            this.stringRelativeTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            this.executorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
            this.executorService.setCorePoolSize(Common.SMPP_TPS);
            this.executorService.setMaximumPoolSize(Integer.MAX_VALUE);

            this.SMPP_IP_ADDRESS = SMPP_IP_ADDRESS;
            this.SMPP_PORT = SMPP_PORT;
            this.SMPP_USER_ID = SMPP_USER_ID;
            this.SMPP_PASSWORD = SMPP_PASSWORD;
            this.SESSION_ID = SESSION_ID;
            this.ROUTE_ID = ROUTE_ID;
        }

        @Override
        public void run() {
            AtomicLong smsCounter = new AtomicLong();
            long lStartTime = 0L;
            long delay = 0L;
            try {
                this.session = newSession();
            } catch (IOException e) {
                System.out.print(SendSMS.TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + this.SESSION_ID + " Run Exception " + e.getMessage() + "\n");
                dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "Run Exception " + e.getMessage());
            }
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " CONNECT\n");
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " SEND START...\n");
            if (Common.bindType.IsTransmittable()) {

                while (Common.IS_RUN) {
                    try {
                        final SMS sms = (SMS) sms_queue.take();
                        if (this.session != null && this.session.getSessionState().isTransmittable()) {
                            smsCounter.addAndGet(sms.getNo_of_sms());
                            if (smsCounter.get() > Common.SMPP_TPS) {
                                delay = System.currentTimeMillis() - lStartTime;
                                if (delay < 1000L) {
                                    try {
                                        System.out.print(new SimpleDateFormat("dd-MM-yyyy hh:mm:ss.SSS").format(new Date()) + " --:  SMPP SESSION " + this.SESSION_ID + " SYSTEM SLEEP FOR: " + (1000L - delay) + "\n");
                                        dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "SYSTEM SLEEP FOR: " + (1000L - delay));
                                        Thread.sleep(1000L - delay);
                                    } catch (InterruptedException ex2) {
                                    }
                                }
                                smsCounter.set(0);
                                lStartTime = System.currentTimeMillis();
                            }
                            String message = sms.getMessage();
                            //if (SendSMS.ROUTE_ID != 12 || SendSMS.ROUTE_ID != 14 || SendSMS.ROUTE_ID != 21 || SendSMS.ROUTE_ID != 25) {
                                message = sms.getMessage7bit();
                            //}
                            this.executorService.execute(this.newSendTask(sms.getTran_cd(), sms.getSource(), sms.getDestination(), message, sms.isFlash(), sms.isUnicode(), sms.isScheduler(), sms.getScheduleDeliveryTime(), sms.getTrntype(), sms.getSend_part(), sms.getRef_no(), sms.getTempId(), sms.getPeId(), sms.getTeleMarketerId()));
                        } else {
                            Common.SetMessagePending(sms.getTran_cd(), sms.getTrntype());
                        }
                    } catch (Exception ex) {
                        System.out.print(SendSMS.TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + this.SESSION_ID + " Try Exception " + ex.getMessage() + "\n");
                        dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "Try Exception " + ex.getMessage());
                    }
                }

                this.executorService.shutdown();
                try {
                    this.executorService.awaitTermination(Common.SMPP_TRANSACTIONTIMER, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e2) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted");
                }
            } else if (Common.bindType.IsReceivable()) {
                while (Common.IS_RUN) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ex3) {
                    }
                }
            }
            this.unbindAndClose();
        }

        private Runnable newSendTask(final long tranCode, final String source, final String destination, final String message, final boolean ab_flash, final boolean ab_unicode, final boolean ab_scheduler, final String scheduleDeliveryDt, final String trntype, final int send_part, final int ref_no, final String templeteId, final String peId, final String telemarketerId) {
            return new Runnable() {
                @Override
                public void run() {
                    String ls_messageId = null;
                    String scheduleDeliveryTime = "";
                    String validityPeriod = "";
                    try {
                        if (ab_scheduler) {
//                            try {
//                                final Date scheduler_dt = SMSSession.this.stringRelativeTimeFormatter.parse(scheduleDeliveryDt);
//                                scheduleDeliveryTime = SendSMS.this.absoluteTimeFormatter.format(scheduler_dt);
//                                validityPeriod = SendSMS.this.validityPeriodFormatter.format(scheduler_dt, Common.VALIDITY_PERIOD);
//
//                                final Date targetTime = new Date();
//                                validityPeriod = SendSMS.this.validityPeriodFormatter.format(targetTime, Common.VALIDITY_PERIOD);
//                                scheduleDeliveryTime = "";
//                            } catch (ParseException ex4) {
//                            }
                            if (Common.VALIDITY_PERIOD > 0) {
                                final Date targetTime = new Date();
                                validityPeriod = SendSMS.this.validityPeriodFormatter.format(targetTime, Common.VALIDITY_PERIOD);
                            }
                        } else if (Common.VALIDITY_PERIOD > 0) {
                            final Date targetTime = new Date();
                            validityPeriod = SendSMS.this.validityPeriodFormatter.format(targetTime, Common.VALIDITY_PERIOD);
                        }

                        for (int li_loop = 1; li_loop <= Common.RETRY_ATTEMPT; li_loop++) {
                            try {
                                ls_messageId = SMSSession.this.SendSms(tranCode, trntype, source, destination, ab_flash, ab_unicode, message, scheduleDeliveryTime, validityPeriod, send_part, ref_no, templeteId, peId, telemarketerId);
                            } catch (ResponseTimeoutException ex) {
                                //  In case of submit timeout, so when internet connection is lost after
                                //  submitting an SMS, you cannot tell whether the SMS was sent or not.
                                //  The service provider will not give back any response.
                                System.out.print(SendSMS.TimeFormatter.format(new Date()) + " --:  ResponseTimeoutException " + ex.getMessage() + "\n");
                                dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "ResponseTimeoutException " + ex.getMessage());
                            } catch (NegativeResponseException ex) {
                                // In case of submit sm Throttling error (ESME has exceeded allowed message limits)
                                // or 168 DND Block, 1009 Black Listed Number or 11 Invalid destination address or 300 Incorrect destination address
                                System.out.print(SendSMS.TimeFormatter.format(new Date()) + " --:  NegativeResponseException " + ex.getMessage() + "\n");
                                dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, " NegativeResponseException " + ex.getMessage());
                                break;
                            }

                            if (ls_messageId != null) {
                                System.out.print(SendSMS.TimeFormatter.format(new Date()) + " --:  SEND SMS MOBILE NO: " + destination + " SENDER ID " + source + " SEND ATTEMPT " + li_loop + " MESSAGE ID " + ls_messageId + "\n");
                                Common.UpdateMessageId(tranCode, trntype, ls_messageId.toUpperCase());
                                break;
                            } else {
                                Common.UpdateSendAttempt(tranCode, trntype);
                                if (li_loop == Common.RETRY_ATTEMPT) {
                                    Common.RejectMessage(tranCode, trntype, 8);
                                }
                            }
                        }
                    } catch (Exception ex2) {
                        System.out.print(SendSMS.TimeFormatter.format(new Date()) + " --:  Exception Message Ref. No: " + tranCode + " " + ex2.getMessage() + "\n");
                        Common.SetMessagePending(tranCode, trntype);
                        dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "Exception Message Ref. No: " + tranCode + " " + ex2.getMessage());
                    }
                }
            };
        }

        private String SendSms(long tranCode, String trnType, String source, String destination,
                boolean ab_flash, boolean ab_unicode, String message, String scheduleDeliveryTime,
                String validityPeriod, int noOfSubmitedPart, int ref_no, String templeteId, String peId,
                String telemarketerId)
                throws IOException, ResponseTimeoutException, NegativeResponseException {

            String ls_message_Id = null;
            MessageClass messageClass = MessageClass.CLASS1;
            Alphabet alphabet;
            int maximumSingleMessageSize;
            int maximumMultipartMessageSegmentSize;
            byte[] byteSingleMessage;
            boolean IsSingleMessage = true;

            if (ab_unicode) {
                alphabet = (ab_flash) ? Alphabet.ALPHA_UCS2_FLASH : Alphabet.ALPHA_UCS2;
                byteSingleMessage = message.getBytes("UTF-16BE");
                maximumSingleMessageSize = Common.MAX_SINGLE_MSG_SEGMENT_SIZE_UCS2;
                maximumMultipartMessageSegmentSize = Common.MAX_MULTIPART_MSG_SEGMENT_SIZE_UCS2;
            } else {
                if (SendSMS.ROUTE_ID == 12 || SendSMS.ROUTE_ID == 14 || SendSMS.ROUTE_ID == 21 || SendSMS.ROUTE_ID == 25) {
                    alphabet = (ab_flash) ? Alphabet.ALPHA_FLASH : Alphabet.ALPHA_DEFAULT;
                } else {
                    alphabet = (ab_flash) ? Alphabet.ALPHA_FLASH : Alphabet.ALPHA_LATIN1; // For Symbol Support in Airtel
                }
                byteSingleMessage = message.getBytes();
                maximumSingleMessageSize = Common.MAX_SINGLE_MSG_SEGMENT_SIZE_7BIT;
                maximumMultipartMessageSegmentSize = Common.MAX_MULTIPART_MSG_SEGMENT_SIZE_7BIT;
            }

            if (SendSMS.ROUTE_ID == 22 || SendSMS.ROUTE_ID == 23) {
                //telemarketerId = "1001096933494158";
                telemarketerId = "1202159178048728302";
            }

            byte[][] byteMessagesArray;
            ESMClass esmClass;

            if (message.length() > maximumSingleMessageSize) {
                byteMessagesArray = Common.splitUnicodeMessage(byteSingleMessage, maximumMultipartMessageSegmentSize, tranCode, trnType, ref_no);
                esmClass = new ESMClass(MessageMode.DEFAULT, MessageType.DEFAULT, GSMSpecificFeature.UDHI);
                IsSingleMessage = false;
            } else {
                byteMessagesArray = new byte[][]{byteSingleMessage};
                esmClass = new ESMClass();
            }

            for (int msgLoop = noOfSubmitedPart; msgLoop <= byteMessagesArray.length; msgLoop++) {
                requestCounter.incrementAndGet();
                try {
                    byte[] msg = byteMessagesArray[msgLoop - 1];
                    if (!ab_unicode) {
                        List<Byte> arrays = new ArrayList<>();
                        for (byte b : msg) {
                            if (b != 27) {  // escape
                                arrays.add(b);
                            }
                        }
                        msg = new byte[arrays.size()];
                        for (int i = 0; i < msg.length; i++) {
                            msg[i] = arrays.get(i);
                        }
                    }

                    ls_message_Id = submitMessage(msg, source, destination, messageClass,
                            alphabet, esmClass, scheduleDeliveryTime, validityPeriod, peId, templeteId, telemarketerId);

                    if (!IsSingleMessage && (trnType.equalsIgnoreCase("O") || trnType.equalsIgnoreCase("T"))) {
                        SMSPartDtl partDtl = new SMSPartDtl(tranCode, trnType, source, destination, (int) byteMessagesArray[0][3], msgLoop, byteMessagesArray.length, ls_message_Id, conn);
                        new InsertSMSDtl(partDtl);
                    }

                } catch (NegativeResponseException ex) {

                    if (!IsSingleMessage && ex.getCommandStatus() == 88) { //Throttling error (ESME has exceeded allowed message limits)
                        Common.UpdateSendPartAndRefNo(msgLoop - 1, (int) byteMessagesArray[0][3], tranCode, trnType);

                    } else if (ex.getCommandStatus() == 88) { //Throttling error (ESME has exceeded allowed message limits)
                        Common.SetMessagePending(tranCode, trnType);

                    } else { // 168 DND Block, 1009 Black Listed Number, 11 Invalid destination address, 300 Incorrect destination address
                        Common.RejectMessage(tranCode, trnType, ex.getCommandStatus());

                    }
                    throw ex;
                }
                responseCounter.incrementAndGet();
            }
            return ls_message_Id;
        }

        private String submitMessage(byte[] message, String source, String destination,
                MessageClass messageClass, Alphabet alphabet, ESMClass esmClass,
                String scheduleDeliveryTime, String validityPeriod,
                String peId,
                String templeteId,
                String teleMarketerId
        ) throws IOException, NegativeResponseException, ResponseTimeoutException {

            String messageId = null;
            try {
                OptionalParameter PrincipalEntityID = new OptionalParameter.Principal_Entity_ID(peId.getBytes());
                OptionalParameter TemplateID = new OptionalParameter.Template_ID(templeteId.getBytes());
                OptionalParameter TelemarketerID = new OptionalParameter.Telemarketer_ID(teleMarketerId.getBytes());

                messageId = session.submitShortMessage("CMT", TypeOfNumber.ALPHANUMERIC, NumberingPlanIndicator.UNKNOWN,
                        source, TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.ISDN, destination, esmClass,
                        (byte) 0, (byte) 3, scheduleDeliveryTime, validityPeriod, new RegisteredDelivery(SMSCDeliveryReceipt.SUCCESS_FAILURE),
                        (byte) 0, new GeneralDataCoding(alphabet, esmClass), (byte) 0, message,
                        PrincipalEntityID, TemplateID, TelemarketerID);

            } catch (PDUException e) {
                System.out.print(TimeFormatter.format(new Date()) + " --:  PDUException " + e.getMessage() + "\n");
                // Invalid PDU parameter
            } catch (InvalidResponseException e) {
                dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "InvalidResponseException " + e.getMessage());
                // Invalid response
            }
            return messageId;
        }

        private class SessionStateListenerImpl implements SessionStateListener {

            @Override
            public void onStateChange(SessionState newState, SessionState oldState, Session source) {
                dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "ON STATE CHANGE NEW STATE " + newState.toString() + " OLD STATE " + oldState.toString());
                System.out.print(TimeFormatter.format(new Date()) + " --:  ON STATE CHANGE NEW STATE " + newState.toString() + " OLD STATE " + oldState.toString() + "\n");

                if (Common.IS_RUN && !newState.isBound() && oldState.isBound()) {
                    dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "SMPP SESSION " + SESSION_ID + " DISCONNECT");
                    System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " DISCONNECT\n");
                    //session = null;
                    reconnectAfter(Common.RECONNECT_INTERVAL);
                }
            }
        }

        private void reconnectAfter(final long timeInMillis) {
            try {
                Thread.sleep(timeInMillis);
            } catch (InterruptedException ignored) {
            }
            dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "RECONNECT AFTER SMPP SESSION " + SESSION_ID);

            try {
                int attempt = 0;
                while (this.session == null || this.session.getSessionState().equals(SessionState.CLOSED)) {
                    attempt++;
                    try {
                        this.session = null;
                        System.gc();
                        System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " RECONNECT ATTEMPT " + attempt + "\n");
                        this.session = newSession();
                        try {///Sleep for session init.
                            Thread.sleep(2000);
                        } catch (InterruptedException ignored) {
                        }
                    } catch (IOException e) {
                        System.out.print(TimeFormatter.format(new Date()) + " --:  Reconnect IOException: " + e.getMessage() + "\n");
                        dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "Reconnect IOException: " + e.getMessage());
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
                dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "RECONNECT WHILE LOOP " + e.getMessage());
            }
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " RECONNECT\n");
        }

        private SMPPSession newSession() throws IOException {
            final SMPPSession tmpSession = new SMPPSession();
            tmpSession.setPduProcessorDegree(Common.SMPP_TPS);
            tmpSession.setTransactionTimer(Common.SMPP_TRANSACTIONTIMER);
            tmpSession.setEnquireLinkTimer(Common.SMPP_ENQUIRELINKTIMEOUT);
            tmpSession.addSessionStateListener(new SessionStateListenerImpl());
            if (Common.bindType.IsReceivable()) {
                tmpSession.setMessageReceiverListener(MessageReceiverListener);
            }
            this.bindParameter = new BindParameter(Common.bindType, this.SMPP_USER_ID, this.SMPP_PASSWORD, "TR", TypeOfNumber.ALPHANUMERIC, NumberingPlanIndicator.UNKNOWN, null);
            tmpSession.connectAndBind(this.SMPP_IP_ADDRESS, this.SMPP_PORT, this.bindParameter);
            return tmpSession;
        }

        public void unbindAndClose() {
            System.out.print(TimeFormatter.format(new Date()) + " --:  SMPP SESSION " + SESSION_ID + " STOP...\n");
            this.session.unbindAndClose();
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
                    dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "InvalidDeliveryReceiptException " + e.getMessage());
                }
            } else {
                dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "Receiving message " + new String(deliverSm.getShortMessage()));
            }
        }

        @Override
        public void onAcceptAlertNotification(AlertNotification alertNotification) {
            dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "onAcceptAlertNotification " + alertNotification.toString());
        }

        @Override
        public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
            dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "onAcceptDataSm " + dataSm.toString());
            throw new ProcessRequestException(DATASM_NOT_IMPLEMENTED, SMPPConstant.STAT_ESME_RINVCMDID);
        }
    }
}
