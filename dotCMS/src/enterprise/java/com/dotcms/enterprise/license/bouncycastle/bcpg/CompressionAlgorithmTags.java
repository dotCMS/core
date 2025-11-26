/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.bcpg;

/**
 * Basic tags for compression algorithms
 */
public interface CompressionAlgorithmTags 
{
    public static final int UNCOMPRESSED = 0;          // Uncompressed
    public static final int ZIP = 1;                   // ZIP (RFC 1951)
    public static final int ZLIB = 2;                  // ZLIB (RFC 1950)
    public static final int BZIP2 = 3;                 // BZ2
}
