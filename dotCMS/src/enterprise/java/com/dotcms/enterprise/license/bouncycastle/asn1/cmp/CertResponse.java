/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.cmp;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEREncodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

public class CertResponse
    extends ASN1Encodable
{
    private DERInteger certReqId;
    private PKIStatusInfo status;
    private CertifiedKeyPair certifiedKeyPair;
    private ASN1OctetString rspInfo;

    private CertResponse(ASN1Sequence seq)
    {
        certReqId = DERInteger.getInstance(seq.getObjectAt(0));
        status = PKIStatusInfo.getInstance(seq.getObjectAt(1));

        if (seq.size() >= 3)
        {
            if (seq.size() == 3)
            {
                DEREncodable o = seq.getObjectAt(2);
                if (o instanceof ASN1OctetString)
                {
                    rspInfo = ASN1OctetString.getInstance(o);
                }
                else
                {
                    certifiedKeyPair = CertifiedKeyPair.getInstance(o);
                }
            }
            else
            {
                certifiedKeyPair = CertifiedKeyPair.getInstance(seq.getObjectAt(2));
                rspInfo = ASN1OctetString.getInstance(seq.getObjectAt(3));
            }
        }
    }

    public static CertResponse getInstance(Object o)
    {
        if (o instanceof CertResponse)
        {
            return (CertResponse)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new CertResponse((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public DERInteger getCertReqId()
    {
        return certReqId;
    }

    public PKIStatusInfo getStatus()
    {
        return status;
    }

    public CertifiedKeyPair getCertifiedKeyPair()
    {
        return certifiedKeyPair;
    }

    /**
     * <pre>
     * CertResponse ::= SEQUENCE {
     *                            certReqId           INTEGER,
     *                            -- to match this response with corresponding request (a value
     *                            -- of -1 is to be used if certReqId is not specified in the
     *                            -- corresponding request)
     *                            status              PKIStatusInfo,
     *                            certifiedKeyPair    CertifiedKeyPair    OPTIONAL,
     *                            rspInfo             OCTET STRING        OPTIONAL
     *                            -- analogous to the id-regInfo-utf8Pairs string defined
     *                            -- for regInfo in CertReqMsg [CRMF]
     *             }
     * </pre> 
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(certReqId);
        v.add(status);

        if (certifiedKeyPair != null)
        {
            v.add(certifiedKeyPair);
        }

        if (rspInfo != null)
        {
            v.add(rspInfo);
        }
        
        return new DERSequence(v);
    }
}
