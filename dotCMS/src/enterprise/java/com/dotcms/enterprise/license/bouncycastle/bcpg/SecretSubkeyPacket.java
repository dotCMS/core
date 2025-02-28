/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.bcpg;

import java.io.*;

/**
 * basic packet for a PGP secret key
 */
public class SecretSubkeyPacket 
    extends SecretKeyPacket
{
    /**
     * 
     * @param in
     * @throws IOException
     */
    SecretSubkeyPacket(
        BCPGInputStream    in)
        throws IOException
    { 
        super(in);
    }
    
    /**
     * 
     * @param pubKeyPacket
     * @param encAlgorithm
     * @param s2k
     * @param iv
     * @param secKeyData
     */
    public SecretSubkeyPacket(
        PublicKeyPacket  pubKeyPacket,
        int              encAlgorithm,
        S2K              s2k,
        byte[]           iv,
        byte[]           secKeyData)
    {
        super(pubKeyPacket, encAlgorithm, s2k, iv, secKeyData);
    }
 
    public SecretSubkeyPacket(
        PublicKeyPacket  pubKeyPacket,
        int              encAlgorithm,
        int              s2kUsage,
        S2K              s2k,
        byte[]           iv,
        byte[]           secKeyData)
    {
        super(pubKeyPacket, encAlgorithm, s2kUsage, s2k, iv, secKeyData);
    }
    
    public void encode(
        BCPGOutputStream    out)
        throws IOException
    {
        out.writePacket(SECRET_SUBKEY, getEncodedContents(), true);
    }
}
