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
 * generic compressed data object.
 */
public class CompressedDataPacket 
    extends InputStreamPacket
{
    int    algorithm;
    
    CompressedDataPacket(
        BCPGInputStream    in)
        throws IOException
    {
        super(in);
        
        algorithm = in.read();    
    }
    
    /**
     * return the algorithm tag value.
     * 
     * @return algorithm tag value.
     */
    public int getAlgorithm()
    {
        return algorithm;
    }
}
