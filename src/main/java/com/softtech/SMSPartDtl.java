/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softtech;

import java.sql.Connection;

/**
 *
 * @author vinod
 */
public class SMSPartDtl {

    private long tranCode;
    private String trnType;
    private String source;
    private String destination;
    private int msg_ref_num;
    private int msgLoop;
    private int noOfMessage;
    private String ls_message_Id;
    private Connection db_conn;

    public SMSPartDtl(long tranCode, String trnType, String source, String destination, int msg_ref_num, int msgLoop, int noOfMessage, String ls_message_Id, Connection db_conn) {
        this.tranCode = tranCode;
        this.trnType = trnType;
        this.source = source;
        this.destination = destination;
        this.msg_ref_num = msg_ref_num;
        this.msgLoop = msgLoop;
        this.noOfMessage = noOfMessage;
        this.ls_message_Id = ls_message_Id;
        this.db_conn =db_conn;
    }

    public long getTranCode() {
        return tranCode;
    }

    public String getTrnType() {
        return trnType;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public int getMsg_ref_num() {
        return msg_ref_num;
    }

    public int getMsgLoop() {
        return msgLoop;
    }

    public int getNoOfMessage() {
        return noOfMessage;
    }

    public String getLs_message_Id() {
        return ls_message_Id;
    }

    public Connection getDb_conn() {
        return db_conn;
    }

}
