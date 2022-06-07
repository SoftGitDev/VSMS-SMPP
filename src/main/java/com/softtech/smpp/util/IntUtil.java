package com.softtech.smpp.util;

/**
 * The int util.
 * 
 * @author SUTHAR
 * @version 1.0
 * @since 1.0
 * 
 */
public class IntUtil {

    private IntUtil() {
        throw new InstantiationError("This class must not be instantiated");
    }

    public static String to4DigitString(final int value) {
        return toNDigitString(value, 4);
    }

    public static String to2DigitString(final int value) {
        return toNDigitString(value, 2);
    }

    public static String toNDigitString(final int value, final int digitLength) {
        StringBuilder stringBuilder = new StringBuilder(String.valueOf(value));
        while (stringBuilder.length() < digitLength) {
            stringBuilder.insert(0, "0");
        }
        return stringBuilder.toString();
    }

    public static final String toHexString(int value) {
        return HexUtil.conventBytesToHexString(OctetUtil.intToBytes(value));
    }
}
