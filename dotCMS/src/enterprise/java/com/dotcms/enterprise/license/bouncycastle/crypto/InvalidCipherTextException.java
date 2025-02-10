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
 * this exception is thrown whenever we find something we don't expect in a
 * message.
 */
public class InvalidCipherTextException 
    extends CryptoException
{
    /**
     * base constructor.
     */
    public InvalidCipherTextException()
    {
    }

    /**
     * create a InvalidCipherTextException with the given message.
     *
     * @param message the message to be carried with the exception.
     */
    public InvalidCipherTextException(
        String  message)
    {
        super(message);
    }
}
