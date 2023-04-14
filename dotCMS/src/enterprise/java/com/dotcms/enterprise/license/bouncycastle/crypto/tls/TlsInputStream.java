package com.dotcms.enterprise.license.bouncycastle.crypto.tls;

import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream for an TLS 1.0 connection.
 */
public class TlsInputStream
    extends InputStream
{
    private byte[] buf = new byte[1];
    private TlsProtocolHandler handler = null;

    TlsInputStream (TlsProtocolHandler handler)
    {
        this.handler = handler;
    }

    public int read(byte[] buf, int offset, int len)
        throws IOException
    {
        return this.handler.readApplicationData(buf, offset, len);
    }
    
    public int read()
        throws IOException
    {
        if (this.read(buf) < 0)
        {
            return -1;
        }
        return buf[0] & 0xff;
    }
    
    public void close()
        throws IOException
    {
        handler.close();
    }
}
