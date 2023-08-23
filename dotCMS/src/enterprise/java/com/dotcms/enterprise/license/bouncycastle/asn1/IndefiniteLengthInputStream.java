/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.license.bouncycastle.asn1;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

class IndefiniteLengthInputStream
    extends LimitedInputStream
{
    private int _b1;
    private int _b2;
    private boolean _eofReached = false;
    private boolean _eofOn00 = true;

    IndefiniteLengthInputStream(
        InputStream in)
        throws IOException
    {
        super(in);

        _b1 = in.read();
        _b2 = in.read();

        if (_b2 < 0)
        {
            // Corrupted stream
            throw new EOFException();
        }

        checkForEof();
    }

    void setEofOn00(
        boolean eofOn00)
    {
        _eofOn00 = eofOn00;
        checkForEof();
    }

    private boolean checkForEof()
    {
        if (!_eofReached && _eofOn00 && (_b1 == 0x00 && _b2 == 0x00))
        {
            _eofReached = true;
            setParentEofDetect(true);
        }
        return _eofReached;
    }

    public int read(byte[] b, int off, int len)
        throws IOException
    {
        // Only use this optimisation if we aren't checking for 00
        if (_eofOn00 || len < 3)
        {
            return super.read(b, off, len);
        }

        if (_eofReached)
        {
            return -1;
        }

        int numRead = _in.read(b, off + 2, len - 2);

        if (numRead < 0)
        {
            // Corrupted stream
            throw new EOFException();
        }

        b[off] = (byte)_b1;
        b[off + 1] = (byte)_b2;

        _b1 = _in.read();
        _b2 = _in.read();

        if (_b2 < 0)
        {
            // Corrupted stream
            throw new EOFException();
        }

        return numRead + 2;
    }

    public int read()
        throws IOException
    {
        if (checkForEof())
        {
            return -1;
        }

        int b = _in.read();

        if (b < 0)
        {
            // Corrupted stream
            throw new EOFException();
        }

        int v = _b1;

        _b1 = _b2;
        _b2 = b;

        return v;
    }
}
