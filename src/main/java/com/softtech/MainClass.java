/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softtech;

/**
 *
 * @author SUTHAR
 */
public class MainClass {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        System.setProperty("io.grpc.netty.shaded.io.netty.native.workdir", "/etc/");
        
        if(args.length == 1){
            int route_id = Integer.parseInt(args[0]);        
            new SendSMS(route_id).start();
        }else{
            int route_id = 12;
            new SendSMS(route_id).start();
        }
    }
}
