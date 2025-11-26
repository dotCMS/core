/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.engines;

/**
 * an implementation of the AES Key Wrapper from the NIST Key Wrap
 * Specification.
 * <p>
 * For further details see: <a href="http://csrc.nist.gov/encryption/kms/key-wrap.pdf">http://csrc.nist.gov/encryption/kms/key-wrap.pdf</a>.
 */
public class AESWrapEngine
    extends RFC3394WrapEngine
{
    public AESWrapEngine()
    {
        super(new AESEngine());
    }
}
