/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.tls;

import com.dotcms.enterprise.license.bouncycastle.asn1.x509.X509CertificateStructure;

/**
 * A certificate verifyer, that will always return true.
 * <pre>
 * DO NOT USE THIS FILE UNLESS YOU KNOW EXACTLY WHAT YOU ARE DOING.
 * </pre>
 */
public class AlwaysValidVerifyer implements CertificateVerifyer
{

    /**
     * Return true.
     *
     * @see com.dotcms.enterprise.license.bouncycastle.crypto.tls.CertificateVerifyer#isValid(com.dotcms.enterprise.license.bouncycastle.asn1.x509.X509CertificateStructure[])
     */
    public boolean isValid(X509CertificateStructure[] certs)
    {
        return true;
    }

}
