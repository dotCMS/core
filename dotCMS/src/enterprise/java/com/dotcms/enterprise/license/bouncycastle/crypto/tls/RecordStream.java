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

package com.dotcms.enterprise.license.bouncycastle.crypto.tls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An implementation of the TLS 1.0 record layer.
 */
public class RecordStream
{

    private TlsProtocolHandler handler;
    private InputStream is;
    private OutputStream os;
    protected CombinedHash hash1;
    protected CombinedHash hash2;
    protected TlsCipherSuite readSuite = null;
    protected TlsCipherSuite writeSuite = null;


    protected RecordStream(TlsProtocolHandler handler, InputStream is, OutputStream os)
    {
        this.handler = handler;
        this.is = is;
        this.os = os;
        hash1 = new CombinedHash();
        hash2 = new CombinedHash();
        this.readSuite = new TlsNullCipherSuite();
        this.writeSuite = this.readSuite;
    }

    public void readData() throws IOException
    {
        short type = TlsUtils.readUint8(is);
        TlsUtils.checkVersion(is, handler);
        int size = TlsUtils.readUint16(is);
        byte[] buf = decodeAndVerify(type, is, size);
        handler.processData(type, buf, 0, buf.length);

    }

    protected byte[] decodeAndVerify(short type, InputStream is, int len) throws IOException
    {
        byte[] buf = new byte[len];
        TlsUtils.readFully(buf, is);
        byte[] result = readSuite.decodeCiphertext(type, buf, 0, buf.length, handler);
        return result;
    }

    protected void writeMessage(short type, byte[] message, int offset, int len) throws IOException
    {
        if (type == 22) // TlsProtocolHandler.RL_HANDSHAKE
        {
            hash1.update(message, offset, len);
            hash2.update(message, offset, len);
        }
        byte[] ciphertext = writeSuite.encodePlaintext(type, message, offset, len);
        byte[] writeMessage = new byte[ciphertext.length + 5];
        TlsUtils.writeUint8(type, writeMessage, 0);
        TlsUtils.writeUint8((short)3, writeMessage, 1);
        TlsUtils.writeUint8((short)1, writeMessage, 2);
        TlsUtils.writeUint16(ciphertext.length, writeMessage, 3);
        System.arraycopy(ciphertext, 0, writeMessage, 5, ciphertext.length);
        os.write(writeMessage);
        os.flush();
    }

    protected void close() throws IOException
    {
        IOException e = null;
        try
        {
            is.close();
        }
        catch (IOException ex)
        {
            e = ex;
        }
        try
        {
            os.close();
        }
        catch (IOException ex)
        {
            e = ex;
        }
        if (e != null)
        {
            throw e;
        }
    }

    protected void flush() throws IOException
    {
        os.flush();
    }

}
