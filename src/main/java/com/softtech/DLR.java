package com.softtech;

import com.softtech.smpp.util.DeliveryReceiptState;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DLR {

    private String SENDER_ID;
    private String MOBILE_NO;
    private String MESSAGE_ID;
    private int SUB;
    private int DELIVER;
    private Date SUBMIT_DT;
    private Date DONE_DT;
    private DeliveryReceiptState DELIVERY_STATUS;
    private String ERROR;
    private String TEXT;
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");

    public DLR(String SENDER_ID, String MOBILE_NO, String MESSAGE_ID, int SUB, int DELIVER, Date SUBMIT_DT, Date DONE_DT, DeliveryReceiptState DELIVERY_STATUS, String ERROR, String TEXT) {
        this.SENDER_ID = SENDER_ID;
        this.MOBILE_NO = MOBILE_NO;
        this.MESSAGE_ID = MESSAGE_ID;
        this.SUB = SUB;
        this.DELIVER = DELIVER;
        this.SUBMIT_DT = SUBMIT_DT;
        this.DONE_DT = DONE_DT;
        this.DELIVERY_STATUS = DELIVERY_STATUS;
        this.ERROR = ERROR;
        this.TEXT = TEXT;
    }

    

    private String intToString(int value, int digit) {
        StringBuilder stringBuilder = new StringBuilder(digit);
        stringBuilder.append(Integer.toString(value));
        while (stringBuilder.length() < digit) {
            stringBuilder.insert(0, "0");
        }
        return stringBuilder.toString();
    }

    /**
     * @return the SENDER_ID
     */
    public String getSENDER_ID() {
        return SENDER_ID;
    }

    /**
     * @param SENDER_ID the SENDER_ID to set
     */
    public void setSENDER_ID(String SENDER_ID) {
        this.SENDER_ID = SENDER_ID;
    }

    /**
     * @return the MOBILE_NO
     */
    public String getMOBILE_NO() {
        return MOBILE_NO;
    }

    /**
     * @param MOBILE_NO the MOBILE_NO to set
     */
    public void setMOBILE_NO(String MOBILE_NO) {
        this.MOBILE_NO = MOBILE_NO;
    }

    /**
     * @return the MESSAGE_ID
     */
    public String getMESSAGE_ID() {
        return MESSAGE_ID;
    }

    /**
     * @param MESSAGE_ID the MESSAGE_ID to set
     */
    public void setMESSAGE_ID(String MESSAGE_ID) {
        this.MESSAGE_ID = MESSAGE_ID;
    }

    /**
     * @return the SUB
     */
    public String getSUB() {
        return intToString(SUB, 3);
    }

    /**
     * @param SUB the SUB to set
     */
    public void setSUB(int SUB) {
        this.SUB = SUB;
    }

    /**
     * @return the DELIVER
     */
    public String getDELIVER() {
        return intToString(DELIVER, 3);
    }

    /**
     * @param DELIVER the DELIVER to set
     */
    public void setDELIVER(int DELIVER) {
        this.DELIVER = DELIVER;
    }

    /**
     * @return the SUBMIT_DT
     */
    public String getSUBMIT_DT() {
        return dateFormat.format(SUBMIT_DT);
    }

    /**
     * @param SUBMIT_DT the SUBMIT_DT to set
     */
    public void setSUBMIT_DT(Date SUBMIT_DT) {
        this.SUBMIT_DT = SUBMIT_DT;
    }

    /**
     * @return the DONE_DT
     */
    public String getDONE_DT() {
        return dateFormat.format(DONE_DT);
    }

    /**
     * @param DONE_DT the DONE_DT to set
     */
    public void setDONE_DT(Date DONE_DT) {
        this.DONE_DT = DONE_DT;
    }

    /**
     * @return the DELIVERY_STATUS
     */
    public DeliveryReceiptState getDELIVERY_STATUS() {
        return DELIVERY_STATUS;
    }

    /**
     * @param DELIVERY_STATUS the DELIVERY_STATUS to set
     */
    public void setDELIVERY_STATUS(DeliveryReceiptState DELIVERY_STATUS) {
        this.DELIVERY_STATUS = DELIVERY_STATUS;
    }

    /**
     * @return the ERROR
     */
    public String getERROR() {
        return ERROR;
    }

    /**
     * @param ERROR the ERROR to set
     */
    public void setERROR(String ERROR) {
        this.ERROR = ERROR;
    }

    /**
     * @return the TEXT
     */
    public String getTEXT() {
        return TEXT;
    }

    /**
     * @param TEXT the TEXT to set
     */
    public void setTEXT(String TEXT) {
        this.TEXT = TEXT;
    }
}
