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
import com.dotcms.enterprise.license.bouncycastle.asn1.DERGeneralizedTime;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERUTCTime;

import java.util.Enumeration;

/**
 * PKIX RFC-2459 - TBSCertList object.
 * <pre>
 * TBSCertList  ::=  SEQUENCE  {
 *      version                 Version OPTIONAL,
 *                                   -- if present, shall be v2
 *      signature               AlgorithmIdentifier,
 *      issuer                  Name,
 *      thisUpdate              Time,
 *      nextUpdate              Time OPTIONAL,
 *      revokedCertificates     SEQUENCE OF SEQUENCE  {
 *           userCertificate         CertificateSerialNumber,
 *           revocationDate          Time,
 *           crlEntryExtensions      Extensions OPTIONAL
 *                                         -- if present, shall be v2
 *                                }  OPTIONAL,
 *      crlExtensions           [0]  EXPLICIT Extensions OPTIONAL
 *                                         -- if present, shall be v2
 *                                }
 * </pre>
 */
public class TBSCertList
    extends ASN1Encodable
{
    public class CRLEntry
        extends ASN1Encodable
    {
        ASN1Sequence  seq;

        DERInteger          userCertificate;
        Time                revocationDate;
        X509Extensions      crlEntryExtensions;

        public CRLEntry(
            ASN1Sequence  seq)
        {
            if (seq.size() < 2 || seq.size() > 3)
            {
                throw new IllegalArgumentException("Bad sequence size: " + seq.size());
            }
            
            this.seq = seq;

            userCertificate = DERInteger.getInstance(seq.getObjectAt(0));
            revocationDate = Time.getInstance(seq.getObjectAt(1));
        }

        public DERInteger getUserCertificate()
        {
            return userCertificate;
        }

        public Time getRevocationDate()
        {
            return revocationDate;
        }

        public X509Extensions getExtensions()
        {
            if (crlEntryExtensions == null && seq.size() == 3)
            {
                crlEntryExtensions = X509Extensions.getInstance(seq.getObjectAt(2));
            }
            
            return crlEntryExtensions;
        }

        public DERObject toASN1Object()
        {
            return seq;
        }
    }

    private class RevokedCertificatesEnumeration
        implements Enumeration
    {
        private final Enumeration en;

        RevokedCertificatesEnumeration(Enumeration en)
        {
            this.en = en;
        }

        public boolean hasMoreElements()
        {
            return en.hasMoreElements();
        }

        public Object nextElement()
        {
            return new CRLEntry(ASN1Sequence.getInstance(en.nextElement()));
        }
    }

    private class EmptyEnumeration
        implements Enumeration
    {
        public boolean hasMoreElements()
        {
            return false;
        }

        public Object nextElement()
        {
            return null;   // TODO: check exception handling
        }
    }

    ASN1Sequence     seq;

    DERInteger              version;
    AlgorithmIdentifier     signature;
    X509Name                issuer;
    Time                    thisUpdate;
    Time                    nextUpdate;
    ASN1Sequence            revokedCertificates;
    X509Extensions          crlExtensions;

    public static TBSCertList getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static TBSCertList getInstance(
        Object  obj)
    {
        if (obj instanceof TBSCertList)
        {
            return (TBSCertList)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new TBSCertList((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public TBSCertList(
        ASN1Sequence  seq)
    {
        if (seq.size() < 3 || seq.size() > 7)
        {
            throw new IllegalArgumentException("Bad sequence size: " + seq.size());
        }

        int seqPos = 0;

        this.seq = seq;

        if (seq.getObjectAt(seqPos) instanceof DERInteger)
        {
            version = DERInteger.getInstance(seq.getObjectAt(seqPos++));
        }
        else
        {
            version = new DERInteger(0);
        }

        signature = AlgorithmIdentifier.getInstance(seq.getObjectAt(seqPos++));
        issuer = X509Name.getInstance(seq.getObjectAt(seqPos++));
        thisUpdate = Time.getInstance(seq.getObjectAt(seqPos++));

        if (seqPos < seq.size()
            && (seq.getObjectAt(seqPos) instanceof DERUTCTime
               || seq.getObjectAt(seqPos) instanceof DERGeneralizedTime
               || seq.getObjectAt(seqPos) instanceof Time))
        {
            nextUpdate = Time.getInstance(seq.getObjectAt(seqPos++));
        }

        if (seqPos < seq.size()
            && !(seq.getObjectAt(seqPos) instanceof DERTaggedObject))
        {
            revokedCertificates = ASN1Sequence.getInstance(seq.getObjectAt(seqPos++));
        }

        if (seqPos < seq.size()
            && seq.getObjectAt(seqPos) instanceof DERTaggedObject)
        {
            crlExtensions = X509Extensions.getInstance(seq.getObjectAt(seqPos));
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

    public AlgorithmIdentifier getSignature()
    {
        return signature;
    }

    public X509Name getIssuer()
    {
        return issuer;
    }

    public Time getThisUpdate()
    {
        return thisUpdate;
    }

    public Time getNextUpdate()
    {
        return nextUpdate;
    }

    public CRLEntry[] getRevokedCertificates()
    {
        if (revokedCertificates == null)
        {
            return new CRLEntry[0];
        }

        CRLEntry[] entries = new CRLEntry[revokedCertificates.size()];

        for (int i = 0; i < entries.length; i++)
        {
            entries[i] = new CRLEntry(ASN1Sequence.getInstance(revokedCertificates.getObjectAt(i)));
        }
        
        return entries;
    }

    public Enumeration getRevokedCertificateEnumeration()
    {
        if (revokedCertificates == null)
        {
            return new EmptyEnumeration();
        }

        return new RevokedCertificatesEnumeration(revokedCertificates.getObjects());
    }

    public X509Extensions getExtensions()
    {
        return crlExtensions;
    }

    public DERObject toASN1Object()
    {
        return seq;
    }
}
