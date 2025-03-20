/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import com.dotcms.enterprise.license.bouncycastle.util.io.Streams;

class DefiniteLengthInputStream
        extends LimitedInputStream
{
    private static final byte[] EMPTY_BYTES = new byte[0];

    private final int _originalLength;
    private int _remaining;

    DefiniteLengthInputStream(
        InputStream in,
        int         length)
    {
        super(in);

        if (length < 0)
        {
            throw new IllegalArgumentException("negative lengths not allowed");
        }

        this._originalLength = length;
        this._remaining = length;

        if (length == 0)
        {
            setParentEofDetect(true);
        }
    }

    int getRemaining()
    {
        return _remaining;
    }

    public int read()
        throws IOException
    {
        if (_remaining == 0)
        {
            return -1;
        }

        int b = _in.read();

        if (b < 0)
        {
            throw new EOFException("DEF length " + _originalLength + " object truncated by " + _remaining);
        }

        if (--_remaining == 0)
        {
            setParentEofDetect(true);
        }

        return b;
    }

    public int read(byte[] buf, int off, int len)
        throws IOException
    {
        if (_remaining == 0)
        {
            return -1;
        }

        int toRead = Math.min(len, _remaining);
        int numRead = _in.read(buf, off, toRead);

        if (numRead < 0)
        {
            throw new EOFException("DEF length " + _originalLength + " object truncated by " + _remaining);
        }

        if ((_remaining -= numRead) == 0)
        {
            setParentEofDetect(true);
        }

        return numRead;
    }

    byte[] toByteArray()
        throws IOException
    {
        if (_remaining == 0)
        {
            return EMPTY_BYTES;
        }

        byte[] bytes = new byte[_remaining];
        if ((_remaining -= Streams.readFully(_in, bytes)) != 0)
        {
            throw new EOFException("DEF length " + _originalLength + " object truncated by " + _remaining);
        }
        setParentEofDetect(true);
        return bytes;
    }
}
