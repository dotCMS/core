/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.misc;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public interface MiscObjectIdentifiers
{
    //
    // Netscape
    //       iso/itu(2) joint-assign(16) us(840) uscompany(1) netscape(113730) cert-extensions(1) }
    //
    static final String                 netscape                = "2.16.840.1.113730.1";
    static final DERObjectIdentifier    netscapeCertType        = new DERObjectIdentifier(netscape + ".1");
    static final DERObjectIdentifier    netscapeBaseURL         = new DERObjectIdentifier(netscape + ".2");
    static final DERObjectIdentifier    netscapeRevocationURL   = new DERObjectIdentifier(netscape + ".3");
    static final DERObjectIdentifier    netscapeCARevocationURL = new DERObjectIdentifier(netscape + ".4");
    static final DERObjectIdentifier    netscapeRenewalURL      = new DERObjectIdentifier(netscape + ".7");
    static final DERObjectIdentifier    netscapeCApolicyURL     = new DERObjectIdentifier(netscape + ".8");
    static final DERObjectIdentifier    netscapeSSLServerName   = new DERObjectIdentifier(netscape + ".12");
    static final DERObjectIdentifier    netscapeCertComment     = new DERObjectIdentifier(netscape + ".13");

    //
    // Novell
    //       iso/itu(2) country(16) us(840) organization(1) novell(113719)
    //
    static final String                 novell                  = "2.16.840.1.113719";
    static final DERObjectIdentifier    novellSecurityAttribs   = new DERObjectIdentifier(novell + ".1.9.4.1");

    //
    // Entrust
    //       iso(1) member-body(16) us(840) nortelnetworks(113533) entrust(7)
    //
    static final String                 entrust                 = "1.2.840.113533.7";
    static final DERObjectIdentifier    entrustVersionExtension = new DERObjectIdentifier(entrust + ".65.0");
}
