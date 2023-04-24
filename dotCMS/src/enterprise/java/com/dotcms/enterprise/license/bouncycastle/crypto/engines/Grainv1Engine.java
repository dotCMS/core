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

package com.dotcms.enterprise.license.bouncycastle.crypto.engines;

import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DataLengthException;
import com.dotcms.enterprise.license.bouncycastle.crypto.StreamCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Implementation of Martin Hell's, Thomas Johansson's and Willi Meier's stream
 * cipher, Grain v1.
 */
public class Grainv1Engine
    implements StreamCipher
{

    /**
     * Constants
     */
    private static final int STATE_SIZE = 5;

    /**
     * Variables to hold the state of the engine during encryption and
     * decryption
     */
    private byte[] workingKey;
    private byte[] workingIV;
    private byte[] out;
    private int[] lfsr;
    private int[] nfsr;
    private int output;
    private int index = 2;

    private boolean initialised = false;

    public String getAlgorithmName()
    {
        return "Grain v1";
    }

    /**
     * Initialize a Grain v1 cipher.
     *
     * @param forEncryption Whether or not we are for encryption.
     * @param params        The parameters required to set up the cipher.
     * @throws IllegalArgumentException If the params argument is inappropriate.
     */
    public void init(boolean forEncryption, CipherParameters params)
        throws IllegalArgumentException
    {
        /**
         * Grain encryption and decryption is completely symmetrical, so the
         * 'forEncryption' is irrelevant.
         */
        if (!(params instanceof ParametersWithIV))
        {
            throw new IllegalArgumentException(
                "Grain v1 Init parameters must include an IV");
        }

        ParametersWithIV ivParams = (ParametersWithIV)params;

        byte[] iv = ivParams.getIV();

        if (iv == null || iv.length != 8)
        {
            throw new IllegalArgumentException(
                "Grain v1 requires exactly 8 bytes of IV");
        }

        if (!(ivParams.getParameters() instanceof KeyParameter))
        {
            throw new IllegalArgumentException(
                "Grain v1 Init parameters must include a key");
        }

        KeyParameter key = (KeyParameter)ivParams.getParameters();

        /**
         * Initialize variables.
         */
        workingIV = new byte[key.getKey().length];
        workingKey = new byte[key.getKey().length];
        lfsr = new int[STATE_SIZE];
        nfsr = new int[STATE_SIZE];
        out = new byte[2];

        System.arraycopy(iv, 0, workingIV, 0, iv.length);
        System.arraycopy(key.getKey(), 0, workingKey, 0, key.getKey().length);

        setKey(workingKey, workingIV);
        initGrain();
    }

    /**
     * 160 clocks initialization phase.
     */
    private void initGrain()
    {
        for (int i = 0; i < 10; i++)
        {
            output = getOutput();
            nfsr = shift(nfsr, getOutputNFSR() ^ lfsr[0] ^ output);
            lfsr = shift(lfsr, getOutputLFSR() ^ output);
        }
        initialised = true;
    }

    /**
     * Get output from non-linear function g(x).
     *
     * @return Output from NFSR.
     */
    private int getOutputNFSR()
    {
        int b0 = nfsr[0];
        int b9 = nfsr[0] >>> 9 | nfsr[1] << 7;
        int b14 = nfsr[0] >>> 14 | nfsr[1] << 2;
        int b15 = nfsr[0] >>> 15 | nfsr[1] << 1;
        int b21 = nfsr[1] >>> 5 | nfsr[2] << 11;
        int b28 = nfsr[1] >>> 12 | nfsr[2] << 4;
        int b33 = nfsr[2] >>> 1 | nfsr[3] << 15;
        int b37 = nfsr[2] >>> 5 | nfsr[3] << 11;
        int b45 = nfsr[2] >>> 13 | nfsr[3] << 3;
        int b52 = nfsr[3] >>> 4 | nfsr[4] << 12;
        int b60 = nfsr[3] >>> 12 | nfsr[4] << 4;
        int b62 = nfsr[3] >>> 14 | nfsr[4] << 2;
        int b63 = nfsr[3] >>> 15 | nfsr[4] << 1;

        return (b62 ^ b60 ^ b52 ^ b45 ^ b37 ^ b33 ^ b28 ^ b21 ^ b14
            ^ b9 ^ b0 ^ b63 & b60 ^ b37 & b33 ^ b15 & b9 ^ b60 & b52 & b45
            ^ b33 & b28 & b21 ^ b63 & b45 & b28 & b9 ^ b60 & b52 & b37
            & b33 ^ b63 & b60 & b21 & b15 ^ b63 & b60 & b52 & b45 & b37
            ^ b33 & b28 & b21 & b15 & b9 ^ b52 & b45 & b37 & b33 & b28
            & b21) & 0x0000FFFF;
    }

    /**
     * Get output from linear function f(x).
     *
     * @return Output from LFSR.
     */
    private int getOutputLFSR()
    {
        int s0 = lfsr[0];
        int s13 = lfsr[0] >>> 13 | lfsr[1] << 3;
        int s23 = lfsr[1] >>> 7 | lfsr[2] << 9;
        int s38 = lfsr[2] >>> 6 | lfsr[3] << 10;
        int s51 = lfsr[3] >>> 3 | lfsr[4] << 13;
        int s62 = lfsr[3] >>> 14 | lfsr[4] << 2;

        return (s0 ^ s13 ^ s23 ^ s38 ^ s51 ^ s62) & 0x0000FFFF;
    }

    /**
     * Get output from output function h(x).
     *
     * @return Output from h(x).
     */
    private int getOutput()
    {
        int b1 = nfsr[0] >>> 1 | nfsr[1] << 15;
        int b2 = nfsr[0] >>> 2 | nfsr[1] << 14;
        int b4 = nfsr[0] >>> 4 | nfsr[1] << 12;
        int b10 = nfsr[0] >>> 10 | nfsr[1] << 6;
        int b31 = nfsr[1] >>> 15 | nfsr[2] << 1;
        int b43 = nfsr[2] >>> 11 | nfsr[3] << 5;
        int b56 = nfsr[3] >>> 8 | nfsr[4] << 8;
        int b63 = nfsr[3] >>> 15 | nfsr[4] << 1;
        int s3 = lfsr[0] >>> 3 | lfsr[1] << 13;
        int s25 = lfsr[1] >>> 9 | lfsr[2] << 7;
        int s46 = lfsr[2] >>> 14 | lfsr[3] << 2;
        int s64 = lfsr[4];

        return (s25 ^ b63 ^ s3 & s64 ^ s46 & s64 ^ s64 & b63 ^ s3
            & s25 & s46 ^ s3 & s46 & s64 ^ s3 & s46 & b63 ^ s25 & s46 & b63 ^ s46
            & s64 & b63 ^ b1 ^ b2 ^ b4 ^ b10 ^ b31 ^ b43 ^ b56) & 0x0000FFFF;
    }

    /**
     * Shift array 16 bits and add val to index.length - 1.
     *
     * @param array The array to shift.
     * @param val   The value to shift in.
     * @return The shifted array with val added to index.length - 1.
     */
    private int[] shift(int[] array, int val)
    {
        array[0] = array[1];
        array[1] = array[2];
        array[2] = array[3];
        array[3] = array[4];
        array[4] = val;

        return array;
    }

    /**
     * Set keys, reset cipher.
     *
     * @param keyBytes The key.
     * @param ivBytes  The IV.
     */
    private void setKey(byte[] keyBytes, byte[] ivBytes)
    {
        ivBytes[8] = (byte)0xFF;
        ivBytes[9] = (byte)0xFF;
        workingKey = keyBytes;
        workingIV = ivBytes;

        /**
         * Load NFSR and LFSR
         */
        int j = 0;
        for (int i = 0; i < nfsr.length; i++)
        {
            nfsr[i] = (workingKey[j + 1] << 8 | workingKey[j] & 0xFF) & 0x0000FFFF;
            lfsr[i] = (workingIV[j + 1] << 8 | workingIV[j] & 0xFF) & 0x0000FFFF;
            j += 2;
        }
    }

    public void processBytes(byte[] in, int inOff, int len, byte[] out,
                             int outOff)
        throws DataLengthException
    {
        if (!initialised)
        {
            throw new IllegalStateException(getAlgorithmName()
                + " not initialised");
        }

        if ((inOff + len) > in.length)
        {
            throw new DataLengthException("input buffer too short");
        }

        if ((outOff + len) > out.length)
        {
            throw new DataLengthException("output buffer too short");
        }

        for (int i = 0; i < len; i++)
        {
            out[outOff + i] = (byte)(in[inOff + i] ^ getKeyStream());
        }
    }

    public void reset()
    {
        index = 2;
        setKey(workingKey, workingIV);
        initGrain();
    }

    /**
     * Run Grain one round(i.e. 16 bits).
     */
    private void oneRound()
    {
        output = getOutput();
        out[0] = (byte)output;
        out[1] = (byte)(output >> 8);

        nfsr = shift(nfsr, getOutputNFSR() ^ lfsr[0]);
        lfsr = shift(lfsr, getOutputLFSR());
    }

    public byte returnByte(byte in)
    {
        if (!initialised)
        {
            throw new IllegalStateException(getAlgorithmName()
                + " not initialised");
        }
        return (byte)(in ^ getKeyStream());
    }

    private byte getKeyStream()
    {
        if (index > 1)
        {
            oneRound();
            index = 0;
        }
        return out[index++];
    }
}
