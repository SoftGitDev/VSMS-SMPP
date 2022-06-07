/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softtech.smpp.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author SUTHAR
 */
public class AESEncryption {
    
    /**
     * gets the AES encryption key. In your actual programs, this should be
     * safely stored.
     *
     * @param sender_id
     * @param mobile_no
     * @return
     * @throws Exception
     */
    public static SecretKey getSecretEncryptionKey(String sender_id, String mobile_no) throws Exception {
        String mykey = sender_id + "|" + mobile_no;
        mykey = mykey.substring(0, 16);
        SecretKeySpec secretKey = new SecretKeySpec(mykey.getBytes("UTF8"), "AES");
        return secretKey;
    }
    
    /**
     * Encrypts plainText in AES using the secret key
     *
     * @param plainText
     * @return String
     * @throws Exception
     */
    public static String encryptText(String plainText, String sender_id, String mobile_no) throws Exception {
        SecretKey secKey = AESEncryption.getSecretEncryptionKey(sender_id, mobile_no);
        //AES defaults to AES in Java 7
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.ENCRYPT_MODE, secKey);
        byte[] byteCipherText = aesCipher.doFinal(plainText.getBytes("UTF-8"));
        return AESEncryption.bytesToHex(byteCipherText);
    }

    /**
     * Decrypts encrypted byte array using the key used for encryption.
     *
     * @param data
     * @param sender_id
     * @param mobile_no
     * @return String
     * @throws Exception
     */
    public static String decryptText(String data, String sender_id, String mobile_no) throws Exception {
        SecretKey secKey = AESEncryption.getSecretEncryptionKey(sender_id, mobile_no);
        byte[] byteCipherText = hexStringToByteArray(data);
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.DECRYPT_MODE, secKey);
        byte[] bytePlainText = aesCipher.doFinal(byteCipherText);
        return new String(bytePlainText);
    }

    /**
     * Convert a binary byte array into readable hex form
     *
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Convert a String to binary byte array into readable hex form
     *
     * @param s
     * @return
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
