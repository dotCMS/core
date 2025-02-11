/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included 
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.crmf;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Choice;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;

public class POPOPrivKey
    extends ASN1Encodable
    implements ASN1Choice
{
    private DERObject obj;

    private POPOPrivKey(DERObject obj)
    {
        this.obj = obj;
    }

    public static ASN1Encodable getInstance(ASN1TaggedObject tagged, boolean explicit)
    {
        return new POPOPrivKey(tagged.getObject()); // must be explictly tagged as choice
    }

    /**
     * <pre>
     * POPOPrivKey ::= CHOICE {
     *        thisMessage       [0] BIT STRING,         -- Deprecated
     *         -- possession is proven in this message (which contains the private
     *         -- key itself (encrypted for the CA))
     *        subsequentMessage [1] SubsequentMessage,
     *         -- possession will be proven in a subsequent message
     *        dhMAC             [2] BIT STRING,         -- Deprecated
     *        agreeMAC          [3] PKMACValue,
     *        encryptedKey      [4] EnvelopedData }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        return obj;
    }
}
