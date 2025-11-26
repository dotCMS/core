/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.esf;

import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

public interface ESFAttributes
{
    public static final DERObjectIdentifier  sigPolicyId = PKCSObjectIdentifiers.id_aa_ets_sigPolicyId;
    public static final DERObjectIdentifier  commitmentType = PKCSObjectIdentifiers.id_aa_ets_commitmentType;
    public static final DERObjectIdentifier  signerLocation = PKCSObjectIdentifiers.id_aa_ets_signerLocation;
    public static final DERObjectIdentifier  signerAttr = PKCSObjectIdentifiers.id_aa_ets_signerAttr;
    public static final DERObjectIdentifier  otherSigCert = PKCSObjectIdentifiers.id_aa_ets_otherSigCert;
    public static final DERObjectIdentifier  contentTimestamp = PKCSObjectIdentifiers.id_aa_ets_contentTimestamp;
    public static final DERObjectIdentifier  certificateRefs = PKCSObjectIdentifiers.id_aa_ets_certificateRefs;
    public static final DERObjectIdentifier  revocationRefs = PKCSObjectIdentifiers.id_aa_ets_revocationRefs;
    public static final DERObjectIdentifier  certValues = PKCSObjectIdentifiers.id_aa_ets_certValues;
    public static final DERObjectIdentifier  revocationValues = PKCSObjectIdentifiers.id_aa_ets_revocationValues;
    public static final DERObjectIdentifier  escTimeStamp = PKCSObjectIdentifiers.id_aa_ets_escTimeStamp;
    public static final DERObjectIdentifier  certCRLTimestamp = PKCSObjectIdentifiers.id_aa_ets_certCRLTimestamp;
    public static final DERObjectIdentifier  archiveTimestamp = PKCSObjectIdentifiers.id_aa_ets_archiveTimestamp;
}
