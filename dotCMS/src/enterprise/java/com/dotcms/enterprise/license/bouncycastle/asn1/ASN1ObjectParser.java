/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1;

import java.io.InputStream;

/**
 * @deprecated will be removed
 */
public class ASN1ObjectParser
{
    ASN1StreamParser _aIn;

    protected ASN1ObjectParser(
        int         baseTag,
        int         tagNumber,
        InputStream contentStream)
    {
        _aIn = new ASN1StreamParser(contentStream);
    }
}
