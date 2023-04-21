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
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;

import java.util.Enumeration;

public class KeyRecRepContent
    extends ASN1Encodable
{
    private PKIStatusInfo status;
    private CMPCertificate newSigCert;
    private ASN1Sequence caCerts;
    private ASN1Sequence keyPairHist;

    private KeyRecRepContent(ASN1Sequence seq)
    {
        Enumeration en = seq.getObjects();

        status = PKIStatusInfo.getInstance(en.nextElement());

        while (en.hasMoreElements())
        {
            ASN1TaggedObject tObj = ASN1TaggedObject.getInstance(en.nextElement());

            switch (tObj.getTagNo())
            {
            case 0:
                newSigCert = CMPCertificate.getInstance(tObj.getObject());
                break;
            case 1:
                caCerts = ASN1Sequence.getInstance(tObj.getObject());
                break;
            case 2:
                keyPairHist = ASN1Sequence.getInstance(tObj.getObject());
                break;
            default:
                throw new IllegalArgumentException("unknown tag number: " + tObj.getTagNo());
            }
        }
    }

    public static KeyRecRepContent getInstance(Object o)
    {
        if (o instanceof KeyRecRepContent)
        {
            return (KeyRecRepContent)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new KeyRecRepContent((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }


    public PKIStatusInfo getStatus()
    {
        return status;
    }

    public CMPCertificate getNewSigCert()
    {
        return newSigCert;
    }

    public CMPCertificate[] getCaCerts()
    {
        if (caCerts == null)
        {
            return null;
        }

        CMPCertificate[] results = new CMPCertificate[caCerts.size()];

        for (int i = 0; i != results.length; i++)
        {
            results[i] = CMPCertificate.getInstance(caCerts.getObjectAt(i));
        }

        return results;
    }

    public CertifiedKeyPair[] getKeyPairHist()
    {
        if (keyPairHist == null)
        {
            return null;
        }

        CertifiedKeyPair[] results = new CertifiedKeyPair[keyPairHist.size()];

        for (int i = 0; i != results.length; i++)
        {
            results[i] = CertifiedKeyPair.getInstance(keyPairHist.getObjectAt(i));
        }

        return results;
    }

    /**
     * <pre>
     * KeyRecRepContent ::= SEQUENCE {
     *                         status                  PKIStatusInfo,
     *                         newSigCert          [0] CMPCertificate OPTIONAL,
     *                         caCerts             [1] SEQUENCE SIZE (1..MAX) OF
     *                                                           CMPCertificate OPTIONAL,
     *                         keyPairHist         [2] SEQUENCE SIZE (1..MAX) OF
     *                                                           CertifiedKeyPair OPTIONAL
     *              }
     * </pre> 
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(status);

        addOptional(v, 0, newSigCert);
        addOptional(v, 1, caCerts);
        addOptional(v, 2, keyPairHist);

        return new DERSequence(v);
    }

    private void addOptional(ASN1EncodableVector v, int tagNo, ASN1Encodable obj)
    {
        if (obj != null)
        {
            v.add(new DERTaggedObject(true, tagNo, obj));
        }
    }
}
