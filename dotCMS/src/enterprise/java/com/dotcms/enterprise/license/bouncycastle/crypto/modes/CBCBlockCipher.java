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

package com.dotcms.enterprise.license.bouncycastle.crypto.modes;

import com.dotcms.enterprise.license.bouncycastle.crypto.BlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DataLengthException;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithIV;
import com.dotcms.enterprise.license.bouncycastle.util.Arrays;

/**
 * implements Cipher-Block-Chaining (CBC) mode on top of a simple cipher.
 */
public class CBCBlockCipher
    implements BlockCipher
{
    private byte[]          IV;
    private byte[]          cbcV;
    private byte[]          cbcNextV;

    private int             blockSize;
    private BlockCipher     cipher = null;
    private boolean         encrypting;

    /**
     * Basic constructor.
     *
     * @param cipher the block cipher to be used as the basis of chaining.
     */
    public CBCBlockCipher(
        BlockCipher cipher)
    {
        this.cipher = cipher;
        this.blockSize = cipher.getBlockSize();

        this.IV = new byte[blockSize];
        this.cbcV = new byte[blockSize];
        this.cbcNextV = new byte[blockSize];
    }

    /**
     * return the underlying block cipher that we are wrapping.
     *
     * @return the underlying block cipher that we are wrapping.
     */
    public BlockCipher getUnderlyingCipher()
    {
        return cipher;
    }

    /**
     * Initialise the cipher and, possibly, the initialisation vector (IV).
     * If an IV isn't passed as part of the parameter, the IV will be all zeros.
     *
     * @param encrypting if true the cipher is initialised for
     *  encryption, if false for decryption.
     * @param params the key and other data required by the cipher.
     * @exception IllegalArgumentException if the params argument is
     * inappropriate.
     */
    public void init(
        boolean             encrypting,
        CipherParameters    params)
        throws IllegalArgumentException
    {
        this.encrypting = encrypting;
        
        if (params instanceof ParametersWithIV)
        {
                ParametersWithIV ivParam = (ParametersWithIV)params;
                byte[]      iv = ivParam.getIV();

                if (iv.length != blockSize)
                {
                    throw new IllegalArgumentException("initialisation vector must be the same length as block size");
                }

                System.arraycopy(iv, 0, IV, 0, iv.length);

                reset();

                cipher.init(encrypting, ivParam.getParameters());
        }
        else
        {
                reset();

                cipher.init(encrypting, params);
        }
    }

    /**
     * return the algorithm name and mode.
     *
     * @return the name of the underlying algorithm followed by "/CBC".
     */
    public String getAlgorithmName()
    {
        return cipher.getAlgorithmName() + "/CBC";
    }

    /**
     * return the block size of the underlying cipher.
     *
     * @return the block size of the underlying cipher.
     */
    public int getBlockSize()
    {
        return cipher.getBlockSize();
    }

    /**
     * Process one block of input from the array in and write it to
     * the out array.
     *
     * @param in the array containing the input data.
     * @param inOff offset into the in array the data starts at.
     * @param out the array the output data will be copied into.
     * @param outOff the offset into the out array the output will start at.
     * @exception DataLengthException if there isn't enough data in in, or
     * space in out.
     * @exception IllegalStateException if the cipher isn't initialised.
     * @return the number of bytes processed and produced.
     */
    public int processBlock(
        byte[]      in,
        int         inOff,
        byte[]      out,
        int         outOff)
        throws DataLengthException, IllegalStateException
    {
        return (encrypting) ? encryptBlock(in, inOff, out, outOff) : decryptBlock(in, inOff, out, outOff);
    }

    /**
     * reset the chaining vector back to the IV and reset the underlying
     * cipher.
     */
    public void reset()
    {
        System.arraycopy(IV, 0, cbcV, 0, IV.length);
        Arrays.fill(cbcNextV, (byte)0);

        cipher.reset();
    }

    /**
     * Do the appropriate chaining step for CBC mode encryption.
     *
     * @param in the array containing the data to be encrypted.
     * @param inOff offset into the in array the data starts at.
     * @param out the array the encrypted data will be copied into.
     * @param outOff the offset into the out array the output will start at.
     * @exception DataLengthException if there isn't enough data in in, or
     * space in out.
     * @exception IllegalStateException if the cipher isn't initialised.
     * @return the number of bytes processed and produced.
     */
    private int encryptBlock(
        byte[]      in,
        int         inOff,
        byte[]      out,
        int         outOff)
        throws DataLengthException, IllegalStateException
    {
        if ((inOff + blockSize) > in.length)
        {
            throw new DataLengthException("input buffer too short");
        }

        /*
         * XOR the cbcV and the input,
         * then encrypt the cbcV
         */
        for (int i = 0; i < blockSize; i++)
        {
            cbcV[i] ^= in[inOff + i];
        }

        int length = cipher.processBlock(cbcV, 0, out, outOff);

        /*
         * copy ciphertext to cbcV
         */
        System.arraycopy(out, outOff, cbcV, 0, cbcV.length);

        return length;
    }

    /**
     * Do the appropriate chaining step for CBC mode decryption.
     *
     * @param in the array containing the data to be decrypted.
     * @param inOff offset into the in array the data starts at.
     * @param out the array the decrypted data will be copied into.
     * @param outOff the offset into the out array the output will start at.
     * @exception DataLengthException if there isn't enough data in in, or
     * space in out.
     * @exception IllegalStateException if the cipher isn't initialised.
     * @return the number of bytes processed and produced.
     */
    private int decryptBlock(
        byte[]      in,
        int         inOff,
        byte[]      out,
        int         outOff)
        throws DataLengthException, IllegalStateException
    {
        if ((inOff + blockSize) > in.length)
        {
            throw new DataLengthException("input buffer too short");
        }

        System.arraycopy(in, inOff, cbcNextV, 0, blockSize);

        int length = cipher.processBlock(in, inOff, out, outOff);

        /*
         * XOR the cbcV and the output
         */
        for (int i = 0; i < blockSize; i++)
        {
            out[outOff + i] ^= cbcV[i];
        }

        /*
         * swap the back up buffer into next position
         */
        byte[]  tmp;

        tmp = cbcV;
        cbcV = cbcNextV;
        cbcNextV = tmp;

        return length;
    }
}
