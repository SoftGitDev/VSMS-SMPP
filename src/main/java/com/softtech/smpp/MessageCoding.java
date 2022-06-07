package com.softtech.smpp;

/**
 * This is an enum const that specifies the message coding (see SMPP specification).
 * 
 * @author SUTHAR
 * @version 1.0
 * @since 1.0
 *
 */
public enum MessageCoding {
    /**
     * Coding 7-bit.
     */
    CODING_7_BIT,
    
    /**
     * Coding 8-bit.
     */
    CODING_8_BIT,
    
    /**
     * Coding 16-bit.
     */
    CODING_16_BIT;
}
