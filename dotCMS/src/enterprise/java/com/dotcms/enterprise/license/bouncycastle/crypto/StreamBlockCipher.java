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

package com.dotcms.enterprise.license.bouncycastle.crypto;

/**
 * a wrapper for block ciphers with a single byte block size, so that they
 * can be treated like stream ciphers.
 */
public class StreamBlockCipher
    implements StreamCipher
{
    private BlockCipher  cipher;

    private byte[]  oneByte = new byte[1];

    /**
     * basic constructor.
     *
     * @param cipher the block cipher to be wrapped.
     * @exception IllegalArgumentException if the cipher has a block size other than
     * one.
     */
    public StreamBlockCipher(
        BlockCipher cipher)
    {
        if (cipher.getBlockSize() != 1)
        {
            throw new IllegalArgumentException("block cipher block size != 1.");
        }

        this.cipher = cipher;
    }

    /**
     * initialise the underlying cipher.
     *
     * @param forEncryption true if we are setting up for encryption, false otherwise.
     * @param params the necessary parameters for the underlying cipher to be initialised.
     */
    public void init(
        boolean forEncryption,
        CipherParameters params)
    {
        cipher.init(forEncryption, params);
    }

    /**
     * return the name of the algorithm we are wrapping.
     *
     * @return the name of the algorithm we are wrapping.
     */
    public String getAlgorithmName()
    {
        return cipher.getAlgorithmName();
    }

    /**
     * encrypt/decrypt a single byte returning the result.
     *
     * @param in the byte to be processed.
     * @return the result of processing the input byte.
     */
    public byte returnByte(
        byte    in)
    {
        oneByte[0] = in;

        cipher.processBlock(oneByte, 0, oneByte, 0);

        return oneByte[0];
    }

    /**
     * process a block of bytes from in putting the result into out.
     * 
     * @param in the input byte array.
     * @param inOff the offset into the in array where the data to be processed starts.
     * @param len the number of bytes to be processed.
     * @param out the output buffer the processed bytes go into.   
     * @param outOff the offset into the output byte array the processed data stars at.
     * @exception DataLengthException if the output buffer is too small.
     */
    public void processBytes(
        byte[]  in,
        int     inOff,
        int     len,
        byte[]  out,
        int     outOff)
        throws DataLengthException
    {
        if (outOff + len > out.length)
        {
            throw new DataLengthException("output buffer too small in processBytes()");
        }

        for (int i = 0; i != len; i++)
        {
                cipher.processBlock(in, inOff + i, out, outOff + i);
        }
    }

    /**
     * reset the underlying cipher. This leaves it in the same state
     * it was at after the last init (if there was one).
     */
    public void reset()
    {
        cipher.reset();
    }
}
