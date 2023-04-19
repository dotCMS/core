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

package com.dotcms.enterprise.license.bouncycastle.asn1.x509;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERBitString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

/**
 * The TBSCertificate object.
 * <pre>
 * TBSCertificate ::= SEQUENCE {
 *      version          [ 0 ]  Version DEFAULT v1(0),
 *      serialNumber            CertificateSerialNumber,
 *      signature               AlgorithmIdentifier,
 *      issuer                  Name,
 *      validity                Validity,
 *      subject                 Name,
 *      subjectPublicKeyInfo    SubjectPublicKeyInfo,
 *      issuerUniqueID    [ 1 ] IMPLICIT UniqueIdentifier OPTIONAL,
 *      subjectUniqueID   [ 2 ] IMPLICIT UniqueIdentifier OPTIONAL,
 *      extensions        [ 3 ] Extensions OPTIONAL
 *      }
 * </pre>
 * <p>
 * Note: issuerUniqueID and subjectUniqueID are both deprecated by the IETF. This class
 * will parse them, but you really shouldn't be creating new ones.
 */
public class TBSCertificateStructure
    extends ASN1Encodable
    implements X509ObjectIdentifiers, PKCSObjectIdentifiers
{
    ASN1Sequence            seq;

    DERInteger              version;
    DERInteger              serialNumber;
    AlgorithmIdentifier     signature;
    X509Name                issuer;
    Time                    startDate, endDate;
    X509Name                subject;
    SubjectPublicKeyInfo    subjectPublicKeyInfo;
    DERBitString            issuerUniqueId;
    DERBitString            subjectUniqueId;
    X509Extensions          extensions;

    public static TBSCertificateStructure getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static TBSCertificateStructure getInstance(
        Object  obj)
    {
        if (obj instanceof TBSCertificateStructure)
        {
            return (TBSCertificateStructure)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new TBSCertificateStructure((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public TBSCertificateStructure(
        ASN1Sequence  seq)
    {
        int         seqStart = 0;

        this.seq = seq;

        //
        // some certficates don't include a version number - we assume v1
        //
        if (seq.getObjectAt(0) instanceof DERTaggedObject)
        {
            version = DERInteger.getInstance(seq.getObjectAt(0));
        }
        else
        {
            seqStart = -1;          // field 0 is missing!
            version = new DERInteger(0);
        }

        serialNumber = DERInteger.getInstance(seq.getObjectAt(seqStart + 1));

        signature = AlgorithmIdentifier.getInstance(seq.getObjectAt(seqStart + 2));
        issuer = X509Name.getInstance(seq.getObjectAt(seqStart + 3));

        //
        // before and after dates
        //
        ASN1Sequence  dates = (ASN1Sequence)seq.getObjectAt(seqStart + 4);

        startDate = Time.getInstance(dates.getObjectAt(0));
        endDate = Time.getInstance(dates.getObjectAt(1));

        subject = X509Name.getInstance(seq.getObjectAt(seqStart + 5));

        //
        // public key info.
        //
        subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(seq.getObjectAt(seqStart + 6));

        for (int extras = seq.size() - (seqStart + 6) - 1; extras > 0; extras--)
        {
            DERTaggedObject extra = (DERTaggedObject)seq.getObjectAt(seqStart + 6 + extras);

            switch (extra.getTagNo())
            {
            case 1:
                issuerUniqueId = DERBitString.getInstance(extra, false);
                break;
            case 2:
                subjectUniqueId = DERBitString.getInstance(extra, false);
                break;
            case 3:
                extensions = X509Extensions.getInstance(extra);
            }
        }
    }

    public int getVersion()
    {
        return version.getValue().intValue() + 1;
    }

    public DERInteger getVersionNumber()
    {
        return version;
    }

    public DERInteger getSerialNumber()
    {
        return serialNumber;
    }

    public AlgorithmIdentifier getSignature()
    {
        return signature;
    }

    public X509Name getIssuer()
    {
        return issuer;
    }

    public Time getStartDate()
    {
        return startDate;
    }

    public Time getEndDate()
    {
        return endDate;
    }

    public X509Name getSubject()
    {
        return subject;
    }

    public SubjectPublicKeyInfo getSubjectPublicKeyInfo()
    {
        return subjectPublicKeyInfo;
    }

    public DERBitString getIssuerUniqueId()
    {
        return issuerUniqueId;
    }

    public DERBitString getSubjectUniqueId()
    {
        return subjectUniqueId;
    }

    public X509Extensions getExtensions()
    {
        return extensions;
    }

    public DERObject toASN1Object()
    {
        return seq;
    }
}
