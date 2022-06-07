package com.softtech.smpp.util;

/**
 * It's a dumb capacity policy. It calculate nothing, just return the new
 * capacity same as requeiredCapacity.
 * 
 * @author SUTHAR
 * 
 */
public class DumbCapacityPolicy implements CapacityPolicy {
    
    /* (non-Javadoc)
     * @see com.softtech.util.CapacityPolicy#ensureCapacity(int, int)
     */
    public int ensureCapacity(int requiredCapacity, int currentCapacity) {
        return requiredCapacity;
    }
}
