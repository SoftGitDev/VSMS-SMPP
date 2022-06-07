/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softtech;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author ASUS
 */
public class dbconfig {

    private static final String DATABASE_NM = "sms_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Software@1";

//    private final String DATABASE_NM = "sms_db";
//    private final String DB_USER = "root";
//    private final String DB_PASSWORD = "SOFTWARE";
    
    
    public static Connection GetConncetion() {
        Connection conn = null;
        try {
            String connectionURL = "jdbc:mysql://localhost:3307/" + DATABASE_NM;
//            String connectionURL = "jdbc:mysql://localhost:3306/" + DATABASE_NM;

            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(connectionURL, DB_USER, DB_PASSWORD);
            //connection.setAutoCommit(false);
            if (!conn.isClosed()) {
                Statement upd_stmt = null;
                upd_stmt = conn.createStatement();
                String sql = "SET GLOBAL max_connections = 50000;";
                int rows = upd_stmt.executeUpdate(sql);
                upd_stmt.close();
                return conn;
            }
        } catch (ClassNotFoundException ex) {
            System.out.print("ClassNotFoundException : " + ex.getMessage());
            WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, "ClassNotFoundException ", ex.getMessage());
            conn = null;
        } catch (IllegalAccessException ex) {
            System.out.print("IllegalAccessException : " + ex.getMessage());
            WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, "IllegalAccessException ", ex.getMessage());
            conn = null;
        } catch (InstantiationException ex) {
            System.out.print("InstantiationException : " + ex.getMessage());
            WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, "InstantiationException ", ex.getMessage());
            conn = null;
        } catch (SQLException ex) {
            System.out.print("SQLException : " + ex.getMessage());
            WriteLog(Common.WRITE_LOG, Common.ERROR_LOG_PATH, "SQLException ", ex.getMessage());
            conn = null;
        }
        System.gc();
        return conn;
    }

    public static Connection GetConncetion(Connection con) {
        Connection conn = null;
        if (isDbConnected(con)) {
            conn = con;
        } else {
            conn = GetConncetion();
        }
        return conn;
    }

    public static boolean isDbConnected(Connection db) {
        Statement sql_stmt = null;
        ResultSet rs = null;
        boolean isConnected = false;
        try {
            sql_stmt = db.createStatement();
            rs = sql_stmt.executeQuery("SELECT 1");
            while (rs.next()) {
                isConnected = true;
            }
        } catch (SQLException | NullPointerException e) {
            // handle SQL error here!
        }
        rs = null;
        sql_stmt = null;
        System.gc();
        return isConnected;
    }

    public static void WriteLog(String as_write_log, String as_path, String as_smpp, String as_text) {
        try {
            if (as_write_log.equals("Y")) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                String date = simpleDateFormat.format(new Date());
                FileWriter writer = new FileWriter(as_path + "LOG_" + date + ".txt", true);
                writer.write(new SimpleDateFormat("dd-MM-yyyy hh:mm:ss.SSS").format(new Date()) + " --:  " + as_smpp + " " + as_text);
                writer.write("\r\n");
                writer.close();
                simpleDateFormat = null;
                writer = null;
                date = null;
                System.gc();
            }
        } catch (IOException e) {
        }
    }

    public static void WriteStatus(String as_path, String as_text) {
        try {
            FileWriter writer = new FileWriter(as_path + "STATUS.txt", false);
            writer.write(new SimpleDateFormat("dd-MM-yyyy hh:mm:ss.SSS").format(new Date()) + " --:  " + as_text);
            writer.write("\r\n");
            writer.close();
            writer = null;
            System.gc();
        } catch (IOException e) {
        }
    }
}
