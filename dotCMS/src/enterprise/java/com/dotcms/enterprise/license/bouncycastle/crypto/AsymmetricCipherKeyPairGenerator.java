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
 * interface that a public/private key pair generator should conform to.
 */
public interface AsymmetricCipherKeyPairGenerator
{
    /**
     * intialise the key pair generator.
     *
     * @param param the parameters the key pair is to be initialised with.
     */
    public void init(KeyGenerationParameters param);

    /**
     * return an AsymmetricCipherKeyPair containing the generated keys.
     *
     * @return an AsymmetricCipherKeyPair containing the generated keys.
     */
    public AsymmetricCipherKeyPair generateKeyPair();
}
