package com.softtech.smpp;

import com.softtech.smpp.util.StringParameter;

/**
 * Thrown if there is an invalid constraint for the PDU String parameter.
 * 
 * @author SUTHAR
 * @version 1.0
 * @since 1.0
 * 
 */
public class PDUStringException extends PDUException {
    private static final long serialVersionUID = 5456303478921567516L;
    private final StringParameter parameter;

    /**
     * Construct with specified message and parameter.
     * 
     * @param message is the detail message.
     * @param parameter is the constraint parameter.
     */
    public PDUStringException(String message, StringParameter parameter) {
        super(message);
        this.parameter = parameter;
    }

    /**
     * Get the constraint parameter.
     * 
     * @return the constraint parameter.
     */
    public StringParameter getParameter() {
        return parameter;
    }

    /**
     * Get the error code of the broken constraint.
     * 
     * @return the command status should be returned.
     */
    public int getErrorCode() {
        return parameter.getErrCode();
    }
}
