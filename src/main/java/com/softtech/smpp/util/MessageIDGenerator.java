package com.softtech.smpp.util;



/**
 * Is the message_id generator.
 * 
 * @author SUTHAR
 * @version 1.0
 * @since 1.0
 *
 */
public interface MessageIDGenerator {
	
	/**
	 * Generate message-id, max 65 C-Octet String.
	 * @return the generated message id.
	 */
	MessageId newMessageId();
}
