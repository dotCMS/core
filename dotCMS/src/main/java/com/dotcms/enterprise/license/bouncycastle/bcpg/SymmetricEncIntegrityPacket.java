package com.dotcms.enterprise.license.bouncycastle.bcpg;

import java.io.*;

/**
 */
public class SymmetricEncIntegrityPacket 
    extends InputStreamPacket
{    
    int        version;
    
    SymmetricEncIntegrityPacket(
        BCPGInputStream    in)
        throws IOException
    {
        super(in);
        
        version = in.read();
    }
}
