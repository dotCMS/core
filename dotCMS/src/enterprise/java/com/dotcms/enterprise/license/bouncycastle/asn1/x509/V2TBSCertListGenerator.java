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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERGeneralizedTime;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEROctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERUTCTime;

/**
 * Generator for Version 2 TBSCertList structures.
 * <pre>
 *  TBSCertList  ::=  SEQUENCE  {
 *       version                 Version OPTIONAL,
 *                                    -- if present, shall be v2
 *       signature               AlgorithmIdentifier,
 *       issuer                  Name,
 *       thisUpdate              Time,
 *       nextUpdate              Time OPTIONAL,
 *       revokedCertificates     SEQUENCE OF SEQUENCE  {
 *            userCertificate         CertificateSerialNumber,
 *            revocationDate          Time,
 *            crlEntryExtensions      Extensions OPTIONAL
 *                                          -- if present, shall be v2
 *                                 }  OPTIONAL,
 *       crlExtensions           [0]  EXPLICIT Extensions OPTIONAL
 *                                          -- if present, shall be v2
 *                                 }
 * </pre>
 *
 * <b>Note: This class may be subject to change</b>
 */
public class V2TBSCertListGenerator
{
    DERInteger version = new DERInteger(1);

    AlgorithmIdentifier     signature;
    X509Name                issuer;
    Time                    thisUpdate, nextUpdate=null;
    X509Extensions          extensions=null;
    private Vector          crlentries=null;

    public V2TBSCertListGenerator()
    {
    }


    public void setSignature(
        AlgorithmIdentifier    signature)
    {
        this.signature = signature;
    }

    public void setIssuer(
        X509Name    issuer)
    {
        this.issuer = issuer;
    }

    public void setThisUpdate(
        DERUTCTime thisUpdate)
    {
        this.thisUpdate = new Time(thisUpdate);
    }

    public void setNextUpdate(
        DERUTCTime nextUpdate)
    {
        this.nextUpdate = new Time(nextUpdate);
    }

    public void setThisUpdate(
        Time thisUpdate)
    {
        this.thisUpdate = thisUpdate;
    }

    public void setNextUpdate(
        Time nextUpdate)
    {
        this.nextUpdate = nextUpdate;
    }

    public void addCRLEntry(
        ASN1Sequence crlEntry)
    {
        if (crlentries == null)
        {
            crlentries = new Vector();
        }
        
        crlentries.addElement(crlEntry);
    }

    public void addCRLEntry(DERInteger userCertificate, DERUTCTime revocationDate, int reason)
    {
        addCRLEntry(userCertificate, new Time(revocationDate), reason);
    }

    public void addCRLEntry(DERInteger userCertificate, Time revocationDate, int reason)
    {
        addCRLEntry(userCertificate, revocationDate, reason, null);
    }

    public void addCRLEntry(DERInteger userCertificate, Time revocationDate, int reason, DERGeneralizedTime invalidityDate)
    {
        Vector extOids = new Vector();
        Vector extValues = new Vector();
        
        if (reason != 0)
        {
            CRLReason crlReason = new CRLReason(reason);
            
            try
            {
                extOids.addElement(X509Extensions.ReasonCode);
                extValues.addElement(new X509Extension(false, new DEROctetString(crlReason.getEncoded())));
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException("error encoding reason: " + e);
            }
        }

        if (invalidityDate != null)
        {
            try
            {
                extOids.addElement(X509Extensions.InvalidityDate);
                extValues.addElement(new X509Extension(false, new DEROctetString(invalidityDate.getEncoded())));
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException("error encoding invalidityDate: " + e);
            }
        }
        
        if (extOids.size() != 0)
        {
            addCRLEntry(userCertificate, revocationDate, new X509Extensions(extOids, extValues));
        }
        else
        {
            addCRLEntry(userCertificate, revocationDate, null);
        }
    }

    public void addCRLEntry(DERInteger userCertificate, Time revocationDate, X509Extensions extensions)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(userCertificate);
        v.add(revocationDate);
        
        if (extensions != null)
        {
            v.add(extensions);
        }
        
        addCRLEntry(new DERSequence(v));
    }
    
    public void setExtensions(
        X509Extensions    extensions)
    {
        this.extensions = extensions;
    }

    public TBSCertList generateTBSCertList()
    {
        if ((signature == null) || (issuer == null) || (thisUpdate == null))
        {
            throw new IllegalStateException("Not all mandatory fields set in V2 TBSCertList generator.");
        }

        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(version);
        v.add(signature);
        v.add(issuer);

        v.add(thisUpdate);
        if (nextUpdate != null)
        {
            v.add(nextUpdate);
        }

        // Add CRLEntries if they exist
        if (crlentries != null)
        {
            ASN1EncodableVector certs = new ASN1EncodableVector();
            Enumeration it = crlentries.elements();
            while(it.hasMoreElements())
            {
                certs.add((ASN1Sequence)it.nextElement());
            }
            v.add(new DERSequence(certs));
        }

        if (extensions != null)
        {
            v.add(new DERTaggedObject(0, extensions));
        }

        return new TBSCertList(new DERSequence(v));
    }
}
