package com.softtech.smpp.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import com.softtech.smpp.PDUStringException;

/**
 * Generate random alphanumeric
 * 
 * @author SUTHAR
 * @version 1.0
 * @since 1.0
 * 
 */
public class RandomMessageIDGenerator implements MessageIDGenerator {
    private Random random;
    
    public RandomMessageIDGenerator() {
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            random = new Random();
        }
    }
    
    /* (non-Javadoc)
     * @see com.softtech.util.MessageIDGenerator#newMessageId()
     */
    public MessageId newMessageId() {
        /*
         * use database sequence convert into hex representation or if not using
         * database using random
         */
        try {
            synchronized (random) {
                return new MessageId(Integer.toString(random.nextInt(Integer.MAX_VALUE), 16));
            }
        } catch (PDUStringException e) {
            throw new RuntimeException("Failed creating message id", e);
        }
    }
}