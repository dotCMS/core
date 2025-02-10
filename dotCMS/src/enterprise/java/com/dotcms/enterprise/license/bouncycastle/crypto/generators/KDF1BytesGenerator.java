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
 * KDF1 generator for derived keys and ivs as defined by IEEE P1363a/ISO 18033
 * <br>
 * This implementation is based on ISO 18033/IEEE P1363a.
 */
public class KDF1BytesGenerator
    extends BaseKDFBytesGenerator
{
    /**
     * Construct a KDF1 byte generator.
     * <p>
     * @param digest the digest to be used as the source of derived keys.
     */
    public KDF1BytesGenerator(
        Digest  digest)
    {
        super(0, digest);
    }
}
