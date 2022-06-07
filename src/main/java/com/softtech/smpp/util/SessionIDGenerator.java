package com.softtech.smpp.util;

/**
 * The session id generator.
 * @author SUTHAR
 * @version 1.0
 * @since 1.0
 * 
 * @param <S> session id
 */
public interface SessionIDGenerator<S> {

    /**
     * @return
     */
    S newSessionId();
}
