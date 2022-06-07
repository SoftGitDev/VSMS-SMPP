package com.softtech.smpp.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import com.softtech.smpp.PDUStringException;

/**
 * Generate random numeric message id
 * 
 * @author SUTHAR
 * @version 1.0
 * @since 2.5
 * 
 */
public class RandomDecimalMessageIDGenerator implements MessageIDGenerator {
    private Random random;

    public RandomDecimalMessageIDGenerator() {
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
         * use random into decimal representation
         */
        try {
            synchronized (random) {
                return new MessageId(String.format("%010d", random.nextInt(Integer.MAX_VALUE), 10));
            }
        } catch (PDUStringException e) {
            throw new RuntimeException("Failed creating message id", e);
        }
    }
}