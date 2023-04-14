package com.dotcms.enterprise.license.bouncycastle.bcpg;

import java.io.*;

/**
 * generic compressed data object.
 */
public class CompressedDataPacket 
    extends InputStreamPacket
{
    int    algorithm;
    
    CompressedDataPacket(
        BCPGInputStream    in)
        throws IOException
    {
        super(in);
        
        algorithm = in.read();    
    }
    
    /**
     * return the algorithm tag value.
     * 
     * @return algorithm tag value.
     */
    public int getAlgorithm()
    {
        return algorithm;
    }
}
