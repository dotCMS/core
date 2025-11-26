/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto;

public interface Wrapper
{
    public void init(boolean forWrapping, CipherParameters param);

    /**
     * Return the name of the algorithm the wrapper implements.
     *
     * @return the name of the algorithm the wrapper implements.
     */
    public String getAlgorithmName();

    public byte[] wrap(byte[] in, int inOff, int inLen);

    public byte[] unwrap(byte[] in, int inOff, int inLen)
        throws InvalidCipherTextException;
}
