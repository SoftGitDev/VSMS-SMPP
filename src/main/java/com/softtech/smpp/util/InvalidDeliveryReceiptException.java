package com.softtech.smpp.util;

/**
 * This exception is throw if there is an invalid format on delivery receipt
 * content.
 * 
 * @author SUTHAR
 * @version 1.0
 * @since 1.0
 * 
 */
public class InvalidDeliveryReceiptException extends Exception {
    private static final long serialVersionUID = 4069256615018999757L;

    public InvalidDeliveryReceiptException(String message) {
        super(message);
    }

    public InvalidDeliveryReceiptException(String message, Exception cause) {
        super(message, cause);
    }
    
}
