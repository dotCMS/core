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
import com.dotcms.enterprise.license.bouncycastle.crypto.params.KeyParameter;

/**
 * a class that provides a basic DESede (or Triple DES) engine.
 */
public class DESedeEngine
    extends DESEngine
{
    protected static final int  BLOCK_SIZE = 8;

    private int[]               workingKey1 = null;
    private int[]               workingKey2 = null;
    private int[]               workingKey3 = null;

    private boolean             forEncryption;

    /**
     * standard constructor.
     */
    public DESedeEngine()
    {
    }

    /**
     * initialise a DESede cipher.
     *
     * @param encrypting whether or not we are for encryption.
     * @param params the parameters required to set up the cipher.
     * @exception IllegalArgumentException if the params argument is
     * inappropriate.
     */
    public void init(
        boolean           encrypting,
        CipherParameters  params)
    {
        if (!(params instanceof KeyParameter))
        {
            throw new IllegalArgumentException("invalid parameter passed to DESede init - " + params.getClass().getName());
        }

        byte[] keyMaster = ((KeyParameter)params).getKey();

        if (keyMaster.length > 24)
        {
            throw new IllegalArgumentException("key size greater than 24 bytes");
        }

        this.forEncryption = encrypting;

        byte[] key1 = new byte[8];
        System.arraycopy(keyMaster, 0, key1, 0, key1.length);
        workingKey1 = generateWorkingKey(encrypting, key1);

        byte[] key2 = new byte[8];
        System.arraycopy(keyMaster, 8, key2, 0, key2.length);
        workingKey2 = generateWorkingKey(!encrypting, key2);

        if (keyMaster.length == 24)
        {
            byte[] key3 = new byte[8];
            System.arraycopy(keyMaster, 16, key3, 0, key3.length);
            workingKey3 = generateWorkingKey(encrypting, key3);
        }
        else    // 16 byte key
        {
            workingKey3 = workingKey1;
        }
    }

    public String getAlgorithmName()
    {
        return "DESede";
    }

    public int getBlockSize()
    {
        return BLOCK_SIZE;
    }

    public int processBlock(
        byte[] in,
        int inOff,
        byte[] out,
        int outOff)
    {
        if (workingKey1 == null)
        {
            throw new IllegalStateException("DESede engine not initialised");
        }

        if ((inOff + BLOCK_SIZE) > in.length)
        {
            throw new DataLengthException("input buffer too short");
        }

        if ((outOff + BLOCK_SIZE) > out.length)
        {
            throw new DataLengthException("output buffer too short");
        }

        byte[] temp = new byte[BLOCK_SIZE];

        if (forEncryption)
        {
            desFunc(workingKey1, in, inOff, temp, 0);
            desFunc(workingKey2, temp, 0, temp, 0);
            desFunc(workingKey3, temp, 0, out, outOff);
        }
        else
        {
            desFunc(workingKey3, in, inOff, temp, 0);
            desFunc(workingKey2, temp, 0, temp, 0);
            desFunc(workingKey1, temp, 0, out, outOff);
        }

        return BLOCK_SIZE;
    }

    public void reset()
    {
    }
}
