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

package com.dotcms.enterprise.license.bouncycastle.bcpg;

import java.io.*;
import java.math.BigInteger;

/**
 * base class for an RSA Secret (or Private) Key.
 */
public class RSASecretBCPGKey 
    extends BCPGObject implements BCPGKey 
{
    MPInteger    d;
    MPInteger    p;
    MPInteger    q;
    MPInteger    u;
    
    BigInteger    expP, expQ, crt;
    
    /**
     * 
     * @param in
     * @throws IOException
     */
    public RSASecretBCPGKey(
        BCPGInputStream    in)
        throws IOException
    {
        this.d = new MPInteger(in);
        this.p = new MPInteger(in);
        this.q = new MPInteger(in);
        this.u = new MPInteger(in);

        expP = d.getValue().remainder(p.getValue().subtract(BigInteger.valueOf(1)));
        expQ = d.getValue().remainder(q.getValue().subtract(BigInteger.valueOf(1)));
        crt = q.getValue().modInverse(p.getValue());
    }
    
    /**
     * 
     * @param d
     * @param p
     * @param q
     */
    public RSASecretBCPGKey(
        BigInteger    d,
        BigInteger    p,
        BigInteger    q)
    {
        //
        // pgp requires (p < q)
        //
        int cmp = p.compareTo(q);
        if (cmp >= 0)
        {
            if (cmp == 0)
            {
                throw new IllegalArgumentException("p and q cannot be equal");
            }

            BigInteger tmp = p;
            p = q;
            q = tmp;
        }

        this.d = new MPInteger(d);
        this.p = new MPInteger(p);
        this.q = new MPInteger(q);
        this.u = new MPInteger(p.modInverse(q));

        expP = d.remainder(p.subtract(BigInteger.valueOf(1)));
        expQ = d.remainder(q.subtract(BigInteger.valueOf(1)));
        crt = q.modInverse(p);
    }
    
    /**
     * return the modulus for this key.
     * 
     * @return BigInteger
     */
    public BigInteger getModulus()
    {
        return p.getValue().multiply(q.getValue());
    }
    
    /**
     * return the private exponent for this key.
     * 
     * @return BigInteger
     */
    public BigInteger getPrivateExponent()
    {
        return d.getValue();
    }
    
    /**
     * return the prime P
     */
    public BigInteger getPrimeP()
    {
        return p.getValue();
    }
    
    /**
     * return the prime Q
     */
    public BigInteger getPrimeQ()
    {
        return q.getValue();
    }
    
    /**
     * return the prime exponent of p
     */
    public BigInteger getPrimeExponentP()
    {
        return expP;
    }
    
    /**
     * return the prime exponent of q
     */
    public BigInteger getPrimeExponentQ()
    {
        return expQ;
    }
    
    /**
     * return the crt coefficient
     */
    public BigInteger getCrtCoefficient()
    {
        return crt;
    }
    
    /**
     *  return "PGP"
     * 
     * @see com.dotcms.enterprise.license.bouncycastle.bcpg.BCPGKey#getFormat()
     */
    public String getFormat() 
    {
        return "PGP";
    }

    /**
     * return the standard PGP encoding of the key.
     * 
     * @see com.dotcms.enterprise.license.bouncycastle.bcpg.BCPGKey#getEncoded()
     */
    public byte[] getEncoded() 
    {
        try
        { 
            ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
            BCPGOutputStream         pgpOut = new BCPGOutputStream(bOut);
        
            pgpOut.writeObject(this);
        
            return bOut.toByteArray();
        }
        catch (IOException e)
        {
            return null;
        }
    }
    
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        out.writeObject(d);
        out.writeObject(p);
        out.writeObject(q);
        out.writeObject(u);
    }
}
