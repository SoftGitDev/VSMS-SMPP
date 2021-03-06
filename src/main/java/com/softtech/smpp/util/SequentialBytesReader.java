package com.softtech.smpp.util;

/**
 * Utility to read value from bytes sequentially.
 * 
 * @author SUTHAR
 * 
 */
class SequentialBytesReader {
    private byte[] bytes;
    int cursor;

    public SequentialBytesReader(byte[] bytes) {
        this.bytes = bytes;
        cursor = 0;
    }

    public byte readByte() {
        return bytes[cursor++];
    }

    public int readInt() {
        int val = OctetUtil.bytesToInt(bytes, cursor);
        cursor += 4;
        return val;
    }

    public byte[] readBytesUntilNull() {
        // TODO SUTHAR: we can do some improvement here
        int i = cursor;
        while (bytes[i++] != (byte)0) {
        }
        int length = i - 1 - cursor;
        if (length == 0) {
            cursor += 1 + length;
            return new byte[] {};
        }
        byte[] data = new byte[length];
        System.arraycopy(bytes, cursor, data, 0, length);
        cursor += 1 + length;
        return data;
    }

    /**
     * @return <tt>String</tt> value. Nullable.
     */
    public String readCString() {
        // TODO we can do some improvement here
        int i = cursor;
        while (bytes[i++] != (byte)0) {
        }
        int length = i - 1 - cursor;
        if (length == 0) {
            cursor += 1 + length;
            return null;
        }
        String val = new String(bytes, cursor, length);
        cursor += 1 + length;
        return val;
    }

    public byte[] readBytes(int length) {
        if (length == 0)
            return new byte[0];
        byte[] data = new byte[length];
        System.arraycopy(bytes, cursor, data, 0, length);
        cursor += length;
        return data;
    }

    public byte[] readBytes(byte length) {
        return readBytes(length & 0xff);
    }

    /**
     * @param length
     * @return <tt>String</tt> value. Nullable.
     */
    public String readString(int length) {
        if (length == 0)
            return null;
        String val = new String(bytes, cursor, length);
        cursor += length;
        return val;
    }

    public short readShort() {
        short value = OctetUtil.bytesToShort(bytes, cursor);
        cursor += 2;
        return value;
    }

    /**
     * @param length
     * @return <tt>String</tt> value. Nullable.
     */
    public String readString(byte length) {
        /*
         * you have to convert the signed byte into unsigned byte (in
         * integer representation) with & operand by 0xff
         */
        return readString(length & 0xff);
    }
    
    public int remainBytesLength() {
        return bytes.length - cursor;
    }
    
    public boolean hasMoreBytes() {
        return cursor < (bytes.length - 1);
    }

    public void resetCursor() {
        cursor = 0;
    }

    public byte[] getBytes() {
        return bytes;
    }
}