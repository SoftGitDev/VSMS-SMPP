/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softtech;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author vinod
 */
public class InsertSMSDtl extends Thread {

    private SMSPartDtl partDtl;

    public InsertSMSDtl(SMSPartDtl partDtl) {
        this.partDtl = partDtl;
        this.start();
    }

    @Override
    public void run() {
        
        Statement ins_stmt = null;

        String ls_Query = "INSERT INTO sms_trn_dtl(TRAN_ID, SENDER_ID, MOBILE_NO, TRN_TYPE, NO_OF_SEG, SEG_ID, REF_NO, SEND_DT, MESSAGE_ID) "
                + " VALUES (" + this.partDtl.getTranCode() + ",'" + this.partDtl.getSource() + "','" + this.partDtl.getDestination() + "','" + this.partDtl.getTrnType() + "'," + this.partDtl.getNoOfMessage() + "," + this.partDtl.getMsgLoop() + "," + this.partDtl.getMsg_ref_num() + ",SYSDATE(),'" + this.partDtl.getLs_message_Id() + "')";
        try {
            ins_stmt = this.partDtl.getDb_conn().createStatement();
            ins_stmt.executeUpdate(ls_Query);
            ins_stmt.close();
        } catch (Exception ex) {
            dbconfig.WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, Common.SMPP, "SQLException " + ex.getMessage());
            try {
                ins_stmt.close();
                ins_stmt = null;
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
        }

    }

}
