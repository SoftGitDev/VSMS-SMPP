package com.softtech.smpp.util;

import java.util.Random;

/**
 * Session id generator.
 * 
 * @author SUTHAR
 * @version 1.0
 * @since 1.0
 *
 */
public class RandomSessionIDGenerator implements SessionIDGenerator<String> {
	private final Random random = new Random();
	
	/**
	 * Generate new session id. Max 64 char.
	 */
	public String newSessionId() {
		byte[] b = new byte[32];
		random.nextBytes(b);
		return HexUtil.conventBytesToHexString(b);
	}
}
