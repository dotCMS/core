/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.bcpg.sig;

import com.dotcms.enterprise.license.bouncycastle.bcpg.SignatureSubpacket;
import com.dotcms.enterprise.license.bouncycastle.bcpg.SignatureSubpacketTags;

/**
 * packet giving trust.
 */
public class TrustSignature 
    extends SignatureSubpacket
{    
    private static byte[] intToByteArray(
        int    v1,
        int    v2)
    {
        byte[]    data = new byte[2];
        
        data[0] = (byte)v1;
        data[1] = (byte)v2;
        
        return data;
    }
    
    public TrustSignature(
        boolean    critical,
        byte[]     data)
    {
        super(SignatureSubpacketTags.TRUST_SIG, critical, data);
    }
    
    public TrustSignature(
        boolean    critical,
        int        depth,
        int        trustAmount)
    {
        super(SignatureSubpacketTags.TRUST_SIG, critical, intToByteArray(depth, trustAmount));
    }
    
    public int getDepth()
    {
        return data[0] & 0xff;
    }
    
    public int getTrustAmount()
    {
        return data[1] & 0xff;
    }
}
