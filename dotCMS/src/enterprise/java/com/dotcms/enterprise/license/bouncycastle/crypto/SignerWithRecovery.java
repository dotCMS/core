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
 * Signer with message recovery.
 */
public interface SignerWithRecovery 
    extends Signer
{
    /**
     * Returns true if the signer has recovered the full message as
     * part of signature verification.
     * 
     * @return true if full message recovered.
     */
    public boolean hasFullMessage();
    
    /**
     * Returns a reference to what message was recovered (if any).
     * 
     * @return full/partial message, null if nothing.
     */
    public byte[] getRecoveredMessage();
}
