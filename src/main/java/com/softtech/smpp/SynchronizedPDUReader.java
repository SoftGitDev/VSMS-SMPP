package com.softtech.smpp;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.softtech.smpp.bean.Command;


/**
 * This class is an implementation of a {@link PDUReader} that uses synchronization when
 * accessing the {@link InputStream} or {@link DataInputStream} used to read the PDU.
 * 
 * @author SUTHAR
 * @version 1.1
 * @since 1.0
 * 
 */
public class SynchronizedPDUReader implements PDUReader {
    private final PDUReader pduReader;
    
    /**
     * Default constructor.
     */
    public SynchronizedPDUReader() {
        this(new DefaultPDUReader());
    }
    
    /**
     * Construct with specified pdu reader.
     * 
     * @param pduReader is the pdu reader.
     */
    public SynchronizedPDUReader(PDUReader pduReader) {
        this.pduReader = pduReader;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUReader#readPDU(java.io.InputStream,
     *      com.softtech.bean.Command)
     */
    public byte[] readPDU(DataInputStream in, Command pduHeader) throws IOException {
        synchronized (in) {
            return pduReader.readPDU(in, pduHeader);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUReader#readPDU(java.io.InputStream, int, int,
     *      int, int)
     */
    public byte[] readPDU(DataInputStream in, int commandLength, int commandId,
            int commandStatus, int sequenceNumber) throws IOException {
        synchronized (in) {
            return pduReader.readPDU(in, commandLength, commandId,
                    commandStatus, sequenceNumber);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUReader#readPDUHeader(java.io.DataInputStream)
     */
    public Command readPDUHeader(DataInputStream in)
            throws InvalidCommandLengthException, IOException {
        synchronized (in) {
            return pduReader.readPDUHeader(in);
        }
    }

}
