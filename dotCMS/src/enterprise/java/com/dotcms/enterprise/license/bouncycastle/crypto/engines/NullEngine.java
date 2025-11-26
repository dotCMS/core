/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.engines;

import com.dotcms.enterprise.license.bouncycastle.crypto.BlockCipher;
import com.dotcms.enterprise.license.bouncycastle.crypto.CipherParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.DataLengthException;

/**
 * The no-op engine that just copies bytes through, irrespective of whether encrypting and decrypting.
 * Provided for the sake of completeness.
 */
public class NullEngine implements BlockCipher
{
    private boolean initialised;
    protected static final int BLOCK_SIZE = 1;
    
    /**
     * Standard constructor.
     */
    public NullEngine()
    {
        super();
    }

    /* (non-Javadoc)
     * @see org.bouncycastle.crypto.BlockCipher#init(boolean, org.bouncycastle.crypto.CipherParameters)
     */
    public void init(boolean forEncryption, CipherParameters params) throws IllegalArgumentException
    {
        // we don't mind any parameters that may come in
        this.initialised = true;
    }

    /* (non-Javadoc)
     * @see org.bouncycastle.crypto.BlockCipher#getAlgorithmName()
     */
    public String getAlgorithmName()
    {
        return "Null";
    }

    /* (non-Javadoc)
     * @see org.bouncycastle.crypto.BlockCipher#getBlockSize()
     */
    public int getBlockSize()
    {
        return BLOCK_SIZE;
    }

    /* (non-Javadoc)
     * @see org.bouncycastle.crypto.BlockCipher#processBlock(byte[], int, byte[], int)
     */
    public int processBlock(byte[] in, int inOff, byte[] out, int outOff)
        throws DataLengthException, IllegalStateException
    {
        if (!initialised)
        {
            throw new IllegalStateException("Null engine not initialised");
        }
            if ((inOff + BLOCK_SIZE) > in.length)
            {
                throw new DataLengthException("input buffer too short");
            }

            if ((outOff + BLOCK_SIZE) > out.length)
            {
                throw new DataLengthException("output buffer too short");
            }
            
            for (int i = 0; i < BLOCK_SIZE; ++i)
            {
                out[outOff + i] = in[inOff + i];
            }
            
            return BLOCK_SIZE;
    }

    /* (non-Javadoc)
     * @see org.bouncycastle.crypto.BlockCipher#reset()
     */
    public void reset()
    {
        // nothing needs to be done
    }
}
