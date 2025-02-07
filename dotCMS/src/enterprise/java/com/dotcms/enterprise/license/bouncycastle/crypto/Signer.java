/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto;

/**
 * Generic signer interface for hash based and message recovery signers.
 */
public interface Signer 
{
    /**
     * Initialise the signer for signing or verification.
     * 
     * @param forSigning true if for signing, false otherwise
     * @param param necessary parameters.
     */
    public void init(boolean forSigning, CipherParameters param);

    /**
     * update the internal digest with the byte b
     */
    public void update(byte b);

    /**
     * update the internal digest with the byte array in
     */
    public void update(byte[] in, int off, int len);

    /**
     * generate a signature for the message we've been loaded with using
     * the key we were initialised with.
     */
    public byte[] generateSignature()
        throws CryptoException, DataLengthException;

    /**
     * return true if the internal state represents the signature described
     * in the passed in array.
     */
    public boolean verifySignature(byte[] signature);
    
    /**
     * reset the internal state
     */
    public void reset();
}
