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
 * this exception is thrown if a buffer that is meant to have output
 * copied into it turns out to be too short, or if we've been given 
 * insufficient input. In general this exception will get thrown rather
 * than an ArrayOutOfBounds exception.
 */
public class DataLengthException 
    extends RuntimeCryptoException
{
    /**
     * base constructor.
     */
    public DataLengthException()
    {
    }

    /**
     * create a DataLengthException with the given message.
     *
     * @param message the message to be carried with the exception.
     */
    public DataLengthException(
        String  message)
    {
        super(message);
    }
}
