package com.softtech.smpp;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.softtech.smpp.bean.Command;


/**
 * Reader for SMPP PDU from specified {@link InputStream}.
 * 
 * @author SUTHAR
 * @version 1.0
 * @since 1.0
 *
 */
public interface PDUReader {

    /**
     * Read the pdu header. If command length to short, we will read the left
     * bytes anyway, and throw {@link InvalidCommandLengthException}
     * 
     * @param in is the input stream of the pdu.
     * @return the header of smpp command.
     * @throws InvalidCommandLengthException if command_length is too short.
     * @throws IOException if an I/O error occurs.
     */
    public Command readPDUHeader(DataInputStream in)
            throws InvalidCommandLengthException, IOException;

    /**
     * Read all smpp pdu (excluding the command header) with specified pdu
     * header.
     * 
     * @param in is input stream of the pdu (the source).
     * @param pduHeader is the pdu header.
     * @return the complete byte of smpp command.
     * @throws IOException if an I/O error occurs.
     */
    public byte[] readPDU(DataInputStream in, Command pduHeader) throws IOException;

    /**
     * Read all smpp pdu (excluding the command header) with specified pdu
     * header values.
     * 
     * @param in is the input stream of the pdu (the source).
     * @param commandLength is the command length of smpp pdu command.
     * @param commandId is id of smpp command.
     * @param commandStatus is status of smpp command.
     * @param sequenceNumber is sequence number of smpp command.
     * @return the complete byte of smpp command.
     * @throws IOException if an I/O error occurs.
     */
    public byte[] readPDU(DataInputStream in, int commandLength, int commandId,
            int commandStatus, int sequenceNumber) throws IOException;

}