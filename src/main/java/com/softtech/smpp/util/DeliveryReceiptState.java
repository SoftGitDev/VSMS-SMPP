package com.softtech.smpp.util;

/**
 * @author SUTHAR
 * @version 1.0
 * @since 1.0
 * 
 */
public enum DeliveryReceiptState {
    /**
     * ENROUTE
     */
    ENROUTE(0),
    /**
     * DELIVERED
     */
    DELIVRD(1),
    /**
     * EXPIRED
     */
    EXPIRED(2),
    /**
     * DELETED
     */
    DELETED(3),
    /**
     * UNDELIVERABLE
     */
    UNDELIV(4),
    /**
     * ACCEPTED
     */
    ACCEPTD(5),
    /**
     * UNKNOWN
     */
    UNKNOWN(6),
    /**
     * REJECTED
     */
    REJECTD(7),
    /**
     * REJECTED
     */
    REJECTED(8),
    /**
     * BLACKLIST_MSISDN
     */
    BLACKLIST_MSISDN(9),
    /**
     * SPAM_CONTENT
     */
    SPAM_CONTENT(10),
    /**
     * SUBMISSION_FAILED
     */
    SUBMISSION_FAILED(11),
    /**
     * SCRUBBING_FAILED
     */
    SCRUBBING_FAILED(12),
    /**
     * FAILED
     */
    FAILED(13);

    private int value;

    DeliveryReceiptState(int value) {
        this.value = value;
    }

    public static DeliveryReceiptState getByName(String name) {
        return valueOf(DeliveryReceiptState.class, name);
    }

    public static DeliveryReceiptState valueOf(int value)
            throws IllegalArgumentException {
        for (DeliveryReceiptState item : values()) {
            if (item.value() == value) {
                return item;
            }
        }
        throw new IllegalArgumentException(
                "No enum const DeliveryReceiptState with value " + value);
    }

    public int value() {
        return value;
    }
}