/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.tls;

/**
 * A NULL CipherSuite in java, this should only be used during handshake.
 */
public class TlsNullCipherSuite extends TlsCipherSuite
{

    protected void init(byte[] ms, byte[] cr, byte[] sr)
    {
        throw new TlsRuntimeException("Sorry, init of TLS_NULL_WITH_NULL_NULL is forbidden");
    }

    protected byte[] encodePlaintext(short type, byte[] plaintext, int offset, int len)
    {
        byte[] result = new byte[len];
        System.arraycopy(plaintext, offset, result, 0, len);
        return result;
    }

    protected byte[] decodeCiphertext(short type, byte[] plaintext, int offset, int len, TlsProtocolHandler handler)
    {
        byte[] result = new byte[len];
        System.arraycopy(plaintext, offset, result, 0, len);
        return result;
    }

    protected short getKeyExchangeAlgorithm()
    {
        return 0;
    }

}
