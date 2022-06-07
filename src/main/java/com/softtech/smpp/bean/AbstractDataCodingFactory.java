package com.softtech.smpp.bean;

/**
 * @author SUTHAR
 *
 */
public abstract class AbstractDataCodingFactory implements DataCodingFactory {

    protected final byte group;
    protected final byte mask;

    public AbstractDataCodingFactory(byte mask, byte group) {
        this.mask = mask;
        this.group = group;
    }

    public boolean isRecognized(byte dataCoding) {
        return (dataCoding & mask) == group;
    }
}
