/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.bcpg;

import java.io.*;
import java.math.BigInteger;

/**
 * basic packet for a PGP public key
 */
public class PublicKeyEncSessionPacket 
    extends ContainedPacket implements PublicKeyAlgorithmTags
{
    private int            version;
    private long           keyID;
    private int            algorithm;
    private BigInteger[]   data;
    
    PublicKeyEncSessionPacket(
        BCPGInputStream    in)
        throws IOException
    {      
        version = in.read();
        
        keyID |= (long)in.read() << 56;
        keyID |= (long)in.read() << 48;
        keyID |= (long)in.read() << 40;
        keyID |= (long)in.read() << 32;
        keyID |= (long)in.read() << 24;
        keyID |= (long)in.read() << 16;
        keyID |= (long)in.read() << 8;
        keyID |= in.read();
        
        algorithm = in.read();
        
        switch (algorithm)
        {
        case RSA_ENCRYPT:
        case RSA_GENERAL:
            data = new BigInteger[1];
            
            data[0] = new MPInteger(in).getValue();
            break;
        case ELGAMAL_ENCRYPT:
        case ELGAMAL_GENERAL:
            data = new BigInteger[2];
            
            data[0] = new MPInteger(in).getValue();
            data[1] = new MPInteger(in).getValue();
            break;
        default:
            throw new IOException("unknown PGP public key algorithm encountered");
        }
    }
    
    public PublicKeyEncSessionPacket(
        long           keyID,
        int            algorithm,
        BigInteger[]   data)
    {
        this.version = 3;
        this.keyID = keyID;
        this.algorithm = algorithm;
        this.data = data;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public long getKeyID()
    {
        return keyID;
    }
    
    public int getAlgorithm()
    {
        return algorithm;
    }
    
    public BigInteger[] getEncSessionKey()
    {
        return data;
    }
    
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        ByteArrayOutputStream  bOut = new ByteArrayOutputStream();
        BCPGOutputStream       pOut = new BCPGOutputStream(bOut);
  
          pOut.write(version);
          
        pOut.write((byte)(keyID >> 56));
        pOut.write((byte)(keyID >> 48));
        pOut.write((byte)(keyID >> 40));
        pOut.write((byte)(keyID >> 32));
        pOut.write((byte)(keyID >> 24));
        pOut.write((byte)(keyID >> 16));
        pOut.write((byte)(keyID >> 8));
        pOut.write((byte)(keyID));
        
        pOut.write(algorithm);
        
        for (int i = 0; i != data.length; i++)
        {
            pOut.writeObject(new MPInteger(data[i]));
        }
        
        out.writePacket(PUBLIC_KEY_ENC_SESSION , bOut.toByteArray(), true);
    }
}
