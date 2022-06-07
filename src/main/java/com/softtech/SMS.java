package com.softtech;

import com.softtech.smpp.util.AESEncryption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class SMS implements Comparable<SMS> {

    private final String ERROR_LOG_PATH = "/opt/SMPP/logs/";
    private final static String SMPP = "";
    
    private static final Set<String> ESCAPE  = new HashSet<>(Arrays.asList(
            new String[]{
                "|", "^", "€", "{", "}", "[", "]", "~"
            }
    ));

    long tran_cd;
    String tran_dt;
    String source;
    String destination;
    String message;
    String tempid;
    String peid;
    String telemarketerid;
    private String trntype;
    private String srt;
    boolean unicode;
    boolean flash;
    boolean scheduler;
    String scheduleDeliveryTime;
    int no_of_sms;
    int send_part;
    int ref_no;

    public SMS(long tran_cd, String tran_dt, String source, String destination, String message, boolean unicode, boolean flash, boolean scheduler, String scheduleDeliveryTime, int send_part, int ref_no, int no_of_sms, String trntype,String tempid,
            String peid,
            String telemarketerid,
            String srt) {
        this.tran_cd = tran_cd;
        this.tran_dt = tran_dt;
        this.source = source;
        this.destination = destination;
        this.message = message;
        this.unicode = unicode;
        this.flash = flash;
        this.scheduler = scheduler;
        this.scheduleDeliveryTime = scheduleDeliveryTime;
        this.no_of_sms = no_of_sms;
        this.send_part = send_part;
        this.ref_no = ref_no;
        this.trntype = trntype;
        this.srt = srt;
        this.tempid = tempid;
        this.peid = peid;
        this.telemarketerid = telemarketerid;
        //SetMessageSendState(this.tran_cd, this.trntype);
    }
    
    
    public String getTempId() {
        return tempid;
    }
    
    public String getPeId() {
        return peid;
    }
    
    public String getTeleMarketerId() {
        return telemarketerid;
    }

    public int getNo_of_sms() {
        return no_of_sms;
    }

    public long getTran_cd() {
        return tran_cd;
    }

    public String getTran_dt() {
        return tran_dt;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public String getMessage() {
        if (this.getTrntype().equalsIgnoreCase("O")) {
            try {
                return AESEncryption.decryptText(this.message, this.source, this.destination);
            } catch (Exception ex) {
                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SMPP, "Exception Original Massage: " + this.message + " Error " + ex.getMessage());
                return this.message;
            }
        }
        return this.message;
    }
    
    public String getMessage7bit() {
        if (this.getTrntype().equalsIgnoreCase("O")) {
            try {
                String msg = AESEncryption.decryptText(this.message, this.source, this.destination);
                if(!this.unicode){
                    return getMessage(msg);
                }
                return msg;
            } catch (Exception ex) {
                dbconfig.WriteLog("Y", ERROR_LOG_PATH, SMPP, "Exception Original Massage: " + this.message + " Error " + ex.getMessage());
                if(!this.unicode){
                    return getMessage(this.message);
                }
                return this.message;
            }
        }
        if(!this.unicode){
            return getMessage(this.message);
        }
        return this.message;
    }

    public boolean isUnicode() {
        return unicode;
    }

    public boolean isFlash() {
        return flash;
    }

    public boolean isScheduler() {
        return scheduler;
    }

    public String getScheduleDeliveryTime() {
        return scheduleDeliveryTime;
    }

    public String getTrntype() {
        return trntype;
    }

    public void setTrntype(String trntype) {
        this.trntype = trntype;
    }

    public String getSrt() {
        return srt;
    }

    public void setStr(String srt) {
        this.srt = srt;
    }

    private void SetMessageSendState(long tran_cd, String as_trn_type) {
        try {
            final Connection con = dbconfig.GetConncetion();
            final Statement upd_stmt = con.createStatement();
            final String sql = "UPDATE sms_trn SET SMS_STATUS='S' WHERE TRAN_ID=" + tran_cd + " AND TRN_TYPE ='" + as_trn_type + "'";
            final int rows = upd_stmt.executeUpdate(sql);
            upd_stmt.close();
            con.close();
        } catch (SQLException ex) {
            System.out.print(new SimpleDateFormat("dd-MM-yyyy hh:mm:ss.SSS").format(new Date()) + " --:  UPADATE SQLException " + ex.getMessage() + "\n");
            dbconfig.WriteLog("Y", "/opt/SMPP/logs/", "", "UPADATE SQLException " + ex.getMessage());
        }
    }

    /**
     * @return the send_part
     */
    public int getSend_part() {
        return this.send_part + 1;
    }

    /**
     * @return the ref_no
     */
    public int getRef_no() {
        return this.ref_no;
    }

    @Override
    public int compareTo(SMS o) {
        return this.srt.compareTo(o.srt);
    }

    public String toString() {
        return this.source + "\t" + this.destination + "\t" + this.message;
    }
    
    public String getMessage(String content) {

        StringBuilder content7bit = new StringBuilder();

        // Add escape characters for extended charset
        for (int i = 0; i < content.length(); i++) {
            if (!ESCAPE.contains(content.charAt(i) + "")) {
                content7bit.append(content.charAt(i));
            } else {
                content7bit.append('\u001b');
                content7bit.append(content.charAt(i));
            }
        }
        return content7bit.toString();
    }
}
