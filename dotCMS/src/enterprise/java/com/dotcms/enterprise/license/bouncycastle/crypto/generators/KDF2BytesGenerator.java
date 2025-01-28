/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.generators;

import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;

/**
 * KFD2 generator for derived keys and ivs as defined by IEEE P1363a/ISO 18033
 * <br>
 * This implementation is based on IEEE P1363/ISO 18033.
 */
public class KDF2BytesGenerator
    extends BaseKDFBytesGenerator
{
    /**
     * Construct a KDF2 bytes generator. Generates key material
     * according to IEEE P1363 or ISO 18033 depending on the initialisation.
     * <p>
     * @param digest the digest to be used as the source of derived keys.
     */
    public KDF2BytesGenerator(
        Digest  digest)
    {
        super(1, digest);
    }
}
