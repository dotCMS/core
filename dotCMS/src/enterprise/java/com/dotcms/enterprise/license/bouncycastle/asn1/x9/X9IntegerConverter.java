/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.x9;

import com.dotcms.enterprise.license.bouncycastle.math.ec.ECCurve;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECFieldElement;

import java.math.BigInteger;

public class X9IntegerConverter
{
    public int getByteLength(
        ECCurve c)
    {
        return (c.getFieldSize() + 7) / 8;
    }

    public int getByteLength(
        ECFieldElement fe)
    {
        return (fe.getFieldSize() + 7) / 8;
    }

    public byte[] integerToBytes(
        BigInteger s,
        int        qLength)
    {
        byte[] bytes = s.toByteArray();
        
        if (qLength < bytes.length)
        {
            byte[] tmp = new byte[qLength];
        
            System.arraycopy(bytes, bytes.length - tmp.length, tmp, 0, tmp.length);
            
            return tmp;
        }
        else if (qLength > bytes.length)
        {
            byte[] tmp = new byte[qLength];
        
            System.arraycopy(bytes, 0, tmp, tmp.length - bytes.length, bytes.length);
            
            return tmp; 
        }
    
        return bytes;
    }
}
