package com.softtech.smpp.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StrictBufferedInputStream extends BufferedInputStream {

    public StrictBufferedInputStream(final InputStream in) {
        super(in);
    }

    public StrictBufferedInputStream(final InputStream in, final int size) {
        super(in, size);
    }

    /** Workaround for an unexpected behavior of 'BufferedInputStream'! */
    @Override
    public int read(final byte[] buffer, final int bufPos, final int length)
            throws IOException {
        int i = super.read(buffer, bufPos, length);
        if ((i == length) || (i == -1))
            return i;
        int j = super.read(buffer, bufPos + i, length - i);
        if (j == -1)
            return i;
        return j + i;
    }

}