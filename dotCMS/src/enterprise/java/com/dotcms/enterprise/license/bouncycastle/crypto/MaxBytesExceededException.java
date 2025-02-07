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
 * this exception is thrown whenever a cipher requires a change of key, iv
 * or similar after x amount of bytes enciphered
 */
public class MaxBytesExceededException
    extends RuntimeCryptoException
{
    /**
     * base constructor.
     */
    public MaxBytesExceededException()
    {
    }

    /**
     * create an with the given message.
     *
     * @param message the message to be carried with the exception.
     */
    public MaxBytesExceededException(
        String  message)
    {
        super(message);
    }
}
