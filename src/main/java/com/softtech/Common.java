/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softtech;

import com.google.communication.businessmessaging.verifiedsms.v1.VerifiedSmsServiceClient;
import static com.softtech.SendSMS.TimeFormatter;
import com.softtech.smpp.bean.BindType;
import com.softtech.smpp.util.DeliveryReceiptState;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 *
 * @author vinod
 */
public class Common {

    public static boolean IS_RUN = false;
    public static String SMPP = "";
    public static final String WRITE_LOG = "Y";
    public static final String ERROR_LOG_PATH = "/opt/SMPP/logs/";
    public static final String STATUS_LOG_PATH = "/opt/SMPP/logs/";

    public static final long RECONNECT_INTERVAL = 5000L; // 5 seconds
    public static final long SMPP_TRANSACTIONTIMER = 20000L; // 10 seconds
    public static final int SMPP_ENQUIRELINKTIMEOUT = 5000; // 5 seconds

    public static int MAX_MULTIPART_MSG_SEGMENT_SIZE_UCS2 = 67;
    public static int MAX_SINGLE_MSG_SEGMENT_SIZE_UCS2 = 70;
    public static int MAX_MULTIPART_MSG_SEGMENT_SIZE_7BIT = 153;
    public static int MAX_SINGLE_MSG_SEGMENT_SIZE_7BIT = 160;
    
    public static final String VSMS_PRIVATE_KEY_PATH = "/opt/SMPP/VSMS/verified-sms-SOFT-TECH-private-key-P-384-pkcs8.der";
    public static final String VSMS_SERVICE_ACCOUNT_JSON = "/opt/SMPP/VSMS/verified-sms-338007-1f6ec7312b4a.json";
    public static byte[] VSMS_PUBLIC_KEY;
    public static VerifiedSmsServiceClient VSMS_SERVICE_CLIENT = null;

    public static int SMPP_TPS = 5;
    public static int SESSION = 0;
    public static int SELECT_LIMIT = 10;
    public static int RETRY_ATTEMPT = 1;
    public static int VALIDITY_PERIOD = 0;

    public static String SMPP_IP_ADDRESS = "";
    public static int SMPP_PORT = 8888;
    public static String SMPP_USER_ID = "";
    public static String SMPP_PASSWORD = "";
    public static String SMPP_MODE = "TRX";
    public static String SMS_TYPE = "T";
    public static String TRN_TYPE = "";
    public static BindType bindType;
    

    public static boolean LoadSetUpValues(int forRoute) {
        Connection con = null;
        Statement sql_stmt = null;
        ResultSet rs = null;

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
            String ls_sql = "SELECT ROUTE_NM,USER_NM,PASSWORD,TRN_TYPE,SMS_TYPE,MODE,TPS,SESSION,IP_ADDRESS,"
                    + "PORT,RETRY_ATTEMPT,ACTIVE,RUN_STATUS,SELECT_LIMIT,VALIDITY_PERIOD,"
                    + "7BIT_SINGLE_MSG_SIZE,7BIT_MULTIPART_MSG_SIZE,UCS2_SINGLE_MSG_SIZE,UCS2_MULTIPART_MSG_SIZE "
                    + " FROM sms_route_mst WHERE ACTIVE ='Y' AND ID =" + forRoute;

            rs = sql_stmt.executeQuery(ls_sql);
            while (rs.next()) {

                SESSION = rs.getInt("SESSION");
                int TPS = rs.getInt("TPS");
                SMPP_PORT = rs.getInt("PORT");
                SMPP = rs.getString("ROUTE_NM").trim();
                SMPP_MODE = rs.getString("MODE").trim();
                SMPP_USER_ID = rs.getString("USER_NM").trim();
                SMPP_PASSWORD = rs.getString("PASSWORD").trim();
                SMPP_IP_ADDRESS = rs.getString("IP_ADDRESS").trim();
                RETRY_ATTEMPT = rs.getInt("RETRY_ATTEMPT");
                SELECT_LIMIT = rs.getInt("SELECT_LIMIT");
                VALIDITY_PERIOD = rs.getInt("VALIDITY_PERIOD");
                SMS_TYPE = rs.getString("SMS_TYPE").trim();
                TRN_TYPE = rs.getString("TRN_TYPE").trim();

                IS_RUN = rs.getString("RUN_STATUS").equalsIgnoreCase("Y");

                MAX_SINGLE_MSG_SEGMENT_SIZE_7BIT = rs.getInt("7BIT_SINGLE_MSG_SIZE");
                MAX_MULTIPART_MSG_SEGMENT_SIZE_7BIT = rs.getInt("7BIT_MULTIPART_MSG_SIZE");
                MAX_SINGLE_MSG_SEGMENT_SIZE_UCS2 = rs.getInt("UCS2_SINGLE_MSG_SIZE");
                MAX_MULTIPART_MSG_SEGMENT_SIZE_UCS2 = rs.getInt("UCS2_MULTIPART_MSG_SIZE");

                if (RETRY_ATTEMPT <= 0) {
                    RETRY_ATTEMPT = 1;
                }

                if (SELECT_LIMIT <= 0) {
                    SELECT_LIMIT = 1;
                }
                SMPP_TPS = Math.round(TPS / SESSION);
                if (SMPP_TPS <= 0) {
                    SMPP_TPS = 10;
                }

                switch (SMPP_MODE) {
                    case "TX":
                        bindType = BindType.BIND_TX;
                        break;
                    case "RX":
                        bindType = BindType.BIND_RX;
                        break;
                    default:
                        bindType = BindType.BIND_TRX;
                        break;
                }
            }
            rs.close();
            sql_stmt.close();
            con.close();
            return IS_RUN;
        } catch (SQLException ex) {
            System.out.print(TimeFormatter.format(new Date()) + " --:  SQLException " + ex.getMessage() + "\n");
            dbconfig.WriteLog(WRITE_LOG, ERROR_LOG_PATH, SMPP, "SQLException " + ex.getMessage());
        }
        return false;
    }
    
    public static int RandomNumber() {
        Random r = new Random();
        return ((1 + r.nextInt(2)) * 10000 + r.nextInt(10000));
    }

    public static byte[][] splitUnicodeMessage(byte[] aMessage, Integer maximumMultipartMessageSegmentSize, long tran_cd, String trntype, int ref_no) {

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
            //new Random().nextBytes(referenceNumber);
            referenceNumber[0] = (byte) RandomNumber();
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

    public static void SetMessageSendState(String tran_cd, String as_trn_type) {
        try {
            final Connection con = dbconfig.GetConncetion();
            final Statement upd_stmt = con.createStatement();
            final String sql = "UPDATE sms_trn SET SMS_STATUS='S' WHERE SMS_STATUS='P' AND TRAN_ID IN (" + tran_cd + ") AND TRN_TYPE IN (" + as_trn_type + ")";
            final int rows = upd_stmt.executeUpdate(sql);
            upd_stmt.close();
            con.close();
        } catch (SQLException ex) {
            System.out.print(new SimpleDateFormat("dd-MM-yyyy hh:mm:ss.SSS").format(new Date()) + " --:  UPADATE SQLException " + ex.getMessage() + "\n");
            dbconfig.WriteLog("Y", "/opt/SMPP/logs/", "", "UPADATE SQLException " + ex.getMessage());
        }
    }

    public static void UpdateMessageId(long al_tran_cd, String as_trn_type, String as_messageId) {
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

    public static void RejectMessage(long al_tran_cd, String as_trn_type, int error_cd) {
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

    public static void SetMessagePending(long al_tran_cd, String as_trn_type) {
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

    public static void UpdateSendAttempt(long al_tran_cd, String as_trn_type) {
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

    public static void UpdateSendPartAndRefNo(int al_send_part, int ref_no, long al_tran_cd, String as_trn_type) {
        Connection con = null;
        Statement upd_stmt = null;

        try {
            con = dbconfig.GetConncetion();
            upd_stmt = con.createStatement();
            String sql = "UPDATE sms_trn SET SMS_STATUS = 'P' , SEND_PART = " + al_send_part + ", REF_NO ='" + ref_no + "' WHERE TRAN_ID =" + al_tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
            int rows = upd_stmt.executeUpdate(sql);
            upd_stmt.close();
            con.close();
        } catch (SQLException ex) {
            System.out.print(TimeFormatter.format(new Date()) + " --:  UpdateSendPart SQLException " + ex.getMessage() + "\n");
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

    public static void InsertDeliverSm(String MOBILE_NO, String SENDER_ID, String MESSAGE_ID, String SUB, String DELIVER, String SUBMIT_DT, String DONE_DT, DeliveryReceiptState DELIVERY_STATUS, String ERROR, String TEXT, int ROUTE_ID) {
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

    /*
    * Date : 31-08-2020 
    * Change By : Chandrakant Suthar
    * Funcation For get sms template id
     */
    private String GetSMSTemplateId(String argu_seller_id, String argu_user_id, String argu_message) {
        Connection conn = null;
        ResultSet rs = null;
        Statement sql_stmt = null;
        String ls_sql = "";
        String ls_result = "";
        try {
            conn = dbconfig.GetConncetion();
            sql_stmt = conn.createStatement();

            // AND MESSAGE = ''
            ls_sql = "SELECT TEMPLATE_ID,TEMPLATE_NM,MESSAGE FROM dtl_template_mst WHERE SELLER_ID = '" + argu_seller_id + "' AND USER_ID = '" + argu_user_id + "';";
            rs = sql_stmt.executeQuery(ls_sql);

            if (rs.next()) {
                ls_result = rs.getString("TEMPLATE_ID");
            }
        } catch (SQLException ex) {
            System.out.print(new SimpleDateFormat("dd-MM-yyyy hh:mm:ss.SSS").format(new Date()) + " --:  GET TEMPLATE_ID SQLException " + ex.getMessage() + "\n");
            dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, "", "GET TEMPLATE_ID SQLException " + ex.getMessage());
            return ls_result;
        } finally {
            try {
                sql_stmt.close();
            } catch (SQLException ex) {
            }
            try {
                rs.close();
            } catch (SQLException ex) {
            }
            try {
                conn.close();
            } catch (SQLException ex) {
            }
        }
        return ls_result;
    }
}
