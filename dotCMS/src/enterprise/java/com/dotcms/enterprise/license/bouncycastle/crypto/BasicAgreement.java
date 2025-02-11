/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto;

import java.math.BigInteger;

/**
 * The basic interface that basic Diffie-Hellman implementations
 * conforms to.
 */
public interface BasicAgreement
{
    /**
     * initialise the agreement engine.
     */
    public void init(CipherParameters param);

    /**
     * given a public key from a given party calculate the next
     * message in the agreement sequence. 
     */
    public BigInteger calculateAgreement(CipherParameters pubKey);
}
