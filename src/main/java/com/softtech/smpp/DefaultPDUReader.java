package com.softtech.smpp;

import java.io.DataInputStream;
import java.io.IOException;
import com.softtech.smpp.bean.Command;
import com.softtech.smpp.util.OctetUtil;


/**
 * Default implementation of {@link PDUReader}.
 *
 * @author SUTHAR
 * @version 1.0
 * @since 1.0
 *
 */
public class DefaultPDUReader implements PDUReader {

    /* (non-Javadoc)
     * @see com.softtech.PDUReader#readPDUHeader(java.io.DataInputStream)
     */
    public Command readPDUHeader(DataInputStream in)
            throws InvalidCommandLengthException, IOException {
        Command header = new Command();
        header.setCommandLength(in.readInt());

        if (header.getCommandLength() < 16) {
            // command length too short, read the left dump anyway
            byte[] dump = new byte[header.getCommandLength()];
            if (header.getCommandLength() >= 4) {
                in.read(dump, 4, header.getCommandLength() - 4);
            }
            throw new InvalidCommandLengthException("Command length "
                    + header.getCommandLength() + " is too short");
        }
        header.setCommandId(in.readInt());
        header.setCommandStatus(in.readInt());
        header.setSequenceNumber(in.readInt());
        return header;
    }

    /* (non-Javadoc)
     * @see com.softtech.PDUReader#readPDU(java.io.InputStream, com.softtech.bean.Command)
     */
    public byte[] readPDU(DataInputStream in, Command pduHeader) throws IOException {
        byte[] b = readPDU(in, pduHeader.getCommandLength(), pduHeader
                .getCommandId(), pduHeader.getCommandStatus(), pduHeader
                .getSequenceNumber());
        
        //System.out.print(TimeFormatter.format(new Date()) + " <--: "+ByteToString(b)+"\n");
        return b;
    }

    /* (non-Javadoc)
     * @see com.softtech.PDUReader#readPDU(java.io.InputStream, int, int, int, int)
     */
    public byte[] readPDU(DataInputStream in, int commandLength, int commandId,
            int commandStatus, int sequenceNumber) throws IOException {

        byte[] b = new byte[commandLength];
        System.arraycopy(OctetUtil.intToBytes(commandLength), 0, b, 0, 4);
        System.arraycopy(OctetUtil.intToBytes(commandId), 0, b, 4, 4);
        System.arraycopy(OctetUtil.intToBytes(commandStatus), 0, b, 8, 4);
        System.arraycopy(OctetUtil.intToBytes(sequenceNumber), 0, b, 12, 4);

        if (commandLength > 16) {
            synchronized (in) {
                in.readFully(b, 16, commandLength - 16);
            }
        }
        return b;
    }
    
    public static String ByteToString(byte[] _bytes) {
       StringBuilder sb = new StringBuilder();
        for(byte b : _bytes){
            sb.append(String.format("%02x", b&0xff));
        }
        return sb.toString();
    }
}
