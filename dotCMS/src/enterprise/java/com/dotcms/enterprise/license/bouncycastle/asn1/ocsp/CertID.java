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

package com.dotcms.enterprise.license.bouncycastle.asn1.ocsp;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class CertID
    extends ASN1Encodable
{
    AlgorithmIdentifier    hashAlgorithm;
    ASN1OctetString        issuerNameHash;
    ASN1OctetString        issuerKeyHash;
    DERInteger             serialNumber;

    public CertID(
        AlgorithmIdentifier hashAlgorithm,
        ASN1OctetString     issuerNameHash,
        ASN1OctetString     issuerKeyHash,
        DERInteger          serialNumber)
    {
        this.hashAlgorithm = hashAlgorithm;
        this.issuerNameHash = issuerNameHash;
        this.issuerKeyHash = issuerKeyHash;
        this.serialNumber = serialNumber;
    }

    public CertID(
        ASN1Sequence    seq)
    {
        hashAlgorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(0));
        issuerNameHash = (ASN1OctetString)seq.getObjectAt(1);
        issuerKeyHash = (ASN1OctetString)seq.getObjectAt(2);
        serialNumber = (DERInteger)seq.getObjectAt(3);
    }

    public static CertID getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static CertID getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof CertID)
        {
            return (CertID)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new CertID((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public AlgorithmIdentifier getHashAlgorithm()
    {
        return hashAlgorithm;
    }

    public ASN1OctetString getIssuerNameHash()
    {
        return issuerNameHash;
    }

    public ASN1OctetString getIssuerKeyHash()
    {
        return issuerKeyHash;
    }

    public DERInteger getSerialNumber()
    {
        return serialNumber;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * CertID          ::=     SEQUENCE {
     *     hashAlgorithm       AlgorithmIdentifier,
     *     issuerNameHash      OCTET STRING, -- Hash of Issuer's DN
     *     issuerKeyHash       OCTET STRING, -- Hash of Issuers public key
     *     serialNumber        CertificateSerialNumber }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector    v = new ASN1EncodableVector();

        v.add(hashAlgorithm);
        v.add(issuerNameHash);
        v.add(issuerKeyHash);
        v.add(serialNumber);

        return new DERSequence(v);
    }
}
