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

/**
 * packet giving signature creation time.
 */
public class PreferredAlgorithms 
    extends SignatureSubpacket
{    
    private static byte[] intToByteArray(
        int[]    v)
    {
        byte[]    data = new byte[v.length];
        
        for (int i = 0; i != v.length; i++)
        {
            data[i] = (byte)v[i];
        }
        
        return data;
    }
    
    public PreferredAlgorithms(
        int        type,
        boolean    critical,
        byte[]     data)
    {
        super(type, critical, data);
    }
    
    public PreferredAlgorithms(
        int        type,
        boolean    critical,
        int[]      preferrences)
    {
        super(type, critical, intToByteArray(preferrences));
    }
    
    /**
     * @deprecated mispelt!
     */
    public int[] getPreferrences()
    {
        return getPreferences();
    }

    public int[] getPreferences()
    {
        int[]    v = new int[data.length];
        
        for (int i = 0; i != v.length; i++)
        {
            v[i] = data[i] & 0xff;
        }
        
        return v;
    }
}
