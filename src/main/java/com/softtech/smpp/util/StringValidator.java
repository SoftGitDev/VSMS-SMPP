package com.softtech.smpp.util;

import com.softtech.smpp.PDUStringException;

/**
 * String validator for the SMPP PDU string types.
 * 
 * @author SUTHAR
 * @version 1.0
 * @since 1.0
 * 
 */
public final class StringValidator {
    private static final String ACTUAL_LENGTH_IS = ". Actual length is ";
    private static final String C_OCTET_STRING_VALUE = "C-Octet String value '";

    private StringValidator() {
        throw new InstantiationError("This class must not be instantiated");
    }

    public static void validateString(String value, StringParameter param)
            throws PDUStringException {
        if (param.getType() == StringType.C_OCTET_STRING) {
            if (param.isRangeMinAndMax()) {
                if (!isCOctetStringValid(value, param.getMax())) {
                    throw new PDUStringException(C_OCTET_STRING_VALUE
                            + value + "' length must be less than " + param.getMax()
                            + ACTUAL_LENGTH_IS + value.length(),
                            param);
                }
            } else if (!isCOctetStringNullOrNValValid(value, param.getMax())) {
                throw new PDUStringException(
                        C_OCTET_STRING_VALUE + value + "' length should be 1 or " + (param.getMax() - 1)
                                + ACTUAL_LENGTH_IS
                                + value.length(), param);
            }
        } else if (param.getType() == StringType.OCTET_STRING
                && !isOctetStringValid(value, param.getMax())) {
            throw new PDUStringException("Octet String value '" + value
                    + "' length must be less than or equal to " + param.getMax()
                    + ACTUAL_LENGTH_IS + value.length(), param);
        }
    }

    public static void validateString(byte[] value, StringParameter param)
            throws PDUStringException {
        if (param.getType() == StringType.C_OCTET_STRING) {
            if (param.isRangeMinAndMax()) {
                if (!isCOctetStringValid(value, param.getMax())) {
                    throw new PDUStringException(C_OCTET_STRING_VALUE
                            + new String(value) + "' length must be less than "
                            + param.getMax() + ACTUAL_LENGTH_IS
                            + value.length, param);
                }
            } else if (!isCOctetStringNullOrNValValid(value, param.getMax())) {
                throw new PDUStringException(
                        C_OCTET_STRING_VALUE + new String(value) + "' length should be 1 or " + (param.getMax() - 1)
                                + ACTUAL_LENGTH_IS
                                + value.length, param);
            }
        } else if (param.getType() == StringType.OCTET_STRING
                && !isOctetStringValid(value, param.getMax())) {
            throw new PDUStringException("Octet String value '"
                    + new String(value) + "' length must be less than or equal to "
                    + param.getMax() + ACTUAL_LENGTH_IS
                    + value.length, param);
        }
    }

    /**
     * Validate the C-Octet String.
     * 
     * @param value
     * @param maxLength
     * @return
     */
    static boolean isCOctetStringValid(String value, int maxLength) {
        if (value == null)
            return true;
        if (value.length() >= maxLength)
            return false;
        return true;

    }

    static boolean isCOctetStringValid(byte[] value, int maxLength) {
        if (value == null)
            return true;
        if (value.length >= maxLength)
            return false;
        return true;

    }

    /**
     * Validate the C-Octet String
     * 
     * @param value
     * @param length
     * @return
     */
    static boolean isCOctetStringNullOrNValValid(String value,
            int length) {
        if (value == null) {
            return true;
        }
        
        if (value.length() == 0) {
            return true;
        }
        
        if (value.length() == length - 1) {
            return true;
        }
        return false;
    }

    static boolean isCOctetStringNullOrNValValid(byte[] value,
            int length) {
        if (value == null)
            return true;
        if (value.length == 0)
            return true;
        if (value.length == length - 1) {
            return true;
        }
        return false;
    }

    /**
     * Validate the Octet String
     * 
     * @param value
     * @param maxLength
     * @return
     */
    static boolean isOctetStringValid(String value, int maxLength) {
        if (value == null)
            return true;
        if (value.length() > maxLength)
            return false;
        return true;
    }

    static boolean isOctetStringValid(byte[] value, int maxLength) {
        if (value == null)
            return true;
        if (value.length > maxLength)
            return false;
        return true;
    }
}
