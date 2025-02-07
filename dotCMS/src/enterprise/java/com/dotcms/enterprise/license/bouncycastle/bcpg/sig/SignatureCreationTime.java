/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.bcpg.sig;

import java.util.Date;

import com.dotcms.enterprise.license.bouncycastle.bcpg.SignatureSubpacket;
import com.dotcms.enterprise.license.bouncycastle.bcpg.SignatureSubpacketTags;

/**
 * packet giving signature creation time.
 */
public class SignatureCreationTime 
    extends SignatureSubpacket
{
    protected static byte[] timeToBytes(
        Date    date)
    {
        byte[]    data = new byte[4];
        long        t = date.getTime() / 1000;
        
        data[0] = (byte)(t >> 24);
        data[1] = (byte)(t >> 16);
        data[2] = (byte)(t >> 8);
        data[3] = (byte)t;
        
        return data;
    }
    
    public SignatureCreationTime(
        boolean    critical,
        byte[]     data)
    {
        super(SignatureSubpacketTags.CREATION_TIME, critical, data);
    }
    
    public SignatureCreationTime(
        boolean    critical,
        Date       date)
    {
        super(SignatureSubpacketTags.CREATION_TIME, critical, timeToBytes(date));
    }
    
    public Date getTime()
    {
        long    time = ((long)(data[0] & 0xff) << 24) | ((data[1] & 0xff) << 16) | ((data[2] & 0xff) << 8) | (data[3] & 0xff);
        
        return new Date(time * 1000);
    }
}
