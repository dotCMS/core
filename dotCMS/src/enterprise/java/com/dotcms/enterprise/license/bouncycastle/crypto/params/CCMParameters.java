/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.params;

public class CCMParameters
    extends AEADParameters
{
    /**
     * Base constructor.
     * 
     * @param key key to be used by underlying cipher
     * @param macSize macSize in bits
     * @param nonce nonce to be used
     * @param associatedText associated text, if any
     */
    public CCMParameters(KeyParameter key, int macSize, byte[] nonce, byte[] associatedText)
    {
        super(key, macSize, nonce, associatedText);
    }
}
