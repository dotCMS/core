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

package com.dotcms.enterprise.license.bouncycastle.asn1.tsp;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1InputStream;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERBoolean;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERGeneralizedTime;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.GeneralName;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.X509Extensions;

import java.io.IOException;
import java.util.Enumeration;

public class TSTInfo
    extends ASN1Encodable
{
    DERInteger version;

    DERObjectIdentifier tsaPolicyId;

    MessageImprint messageImprint;

    DERInteger serialNumber;

    DERGeneralizedTime genTime;

    Accuracy accuracy;

    DERBoolean ordering;

    DERInteger nonce;

    GeneralName tsa;

    X509Extensions extensions;

    public static TSTInfo getInstance(Object o)
    {
        if (o == null || o instanceof TSTInfo)
        {
            return (TSTInfo) o;
        }
        else if (o instanceof ASN1Sequence)
        {
            return new TSTInfo((ASN1Sequence) o);
        }
        else if (o instanceof ASN1OctetString)
        {
            try
            {
                return getInstance(new ASN1InputStream(((ASN1OctetString)o).getOctets()).readObject());
            }
            catch (IOException ioEx)
            {
                throw new IllegalArgumentException(
                        "Bad object format in 'TSTInfo' factory.");
            }
        }

        throw new IllegalArgumentException(
                "Unknown object in 'TSTInfo' factory : "
                        + o.getClass().getName() + ".");
    }

    public TSTInfo(ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();

        // version
        version = DERInteger.getInstance(e.nextElement());

        // tsaPolicy
        tsaPolicyId = DERObjectIdentifier.getInstance(e.nextElement());

        // messageImprint
        messageImprint = MessageImprint.getInstance(e.nextElement());

        // serialNumber
        serialNumber = DERInteger.getInstance(e.nextElement());

        // genTime
        genTime = DERGeneralizedTime.getInstance(e.nextElement());

        // default for ordering
        ordering = new DERBoolean(false);
        
        while (e.hasMoreElements())
        {
            DERObject o = (DERObject) e.nextElement();

            if (o instanceof ASN1TaggedObject)
            {
                DERTaggedObject tagged = (DERTaggedObject) o;

                switch (tagged.getTagNo())
                {
                case 0:
                    tsa = GeneralName.getInstance(tagged, true);
                    break;
                case 1:
                    extensions = X509Extensions.getInstance(tagged, false);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown tag value " + tagged.getTagNo());
                }
            }
            else if (o instanceof DERSequence)
            {
                accuracy = Accuracy.getInstance(o);
            }
            else if (o instanceof DERBoolean)
            {
                ordering = DERBoolean.getInstance(o);
            }
            else if (o instanceof DERInteger)
            {
                nonce = DERInteger.getInstance(o);
            }

        }
    }

    public TSTInfo(DERObjectIdentifier tsaPolicyId, MessageImprint messageImprint,
            DERInteger serialNumber, DERGeneralizedTime genTime,
            Accuracy accuracy, DERBoolean ordering, DERInteger nonce,
            GeneralName tsa, X509Extensions extensions)
    {
        version = new DERInteger(1);
        this.tsaPolicyId = tsaPolicyId;
        this.messageImprint = messageImprint;
        this.serialNumber = serialNumber;
        this.genTime = genTime;

        this.accuracy = accuracy;
        this.ordering = ordering;
        this.nonce = nonce;
        this.tsa = tsa;
        this.extensions = extensions;
    }

    public MessageImprint getMessageImprint()
    {
        return messageImprint;
    }

    public DERObjectIdentifier getPolicy()
    {
        return tsaPolicyId;
    }

    public DERInteger getSerialNumber()
    {
        return serialNumber;
    }

    public Accuracy getAccuracy()
    {
        return accuracy;
    }

    public DERGeneralizedTime getGenTime()
    {
        return genTime;
    }

    public DERBoolean getOrdering()
    {
        return ordering;
    }

    public DERInteger getNonce()
    {
        return nonce;
    }

    public GeneralName getTsa()
    {
        return tsa;
    }

    public X509Extensions getExtensions()
    {
        return extensions;
    }

    /**
     * <pre>
     * 
     *     TSTInfo ::= SEQUENCE  {
     *        version                      INTEGER  { v1(1) },
     *        policy                       TSAPolicyId,
     *        messageImprint               MessageImprint,
     *          -- MUST have the same value as the similar field in
     *          -- TimeStampReq
     *        serialNumber                 INTEGER,
     *         -- Time-Stamping users MUST be ready to accommodate integers
     *         -- up to 160 bits.
     *        genTime                      GeneralizedTime,
     *        accuracy                     Accuracy                 OPTIONAL,
     *        ordering                     BOOLEAN             DEFAULT FALSE,
     *        nonce                        INTEGER                  OPTIONAL,
     *          -- MUST be present if the similar field was present
     *          -- in TimeStampReq.  In that case it MUST have the same value.
     *        tsa                          [0] GeneralName          OPTIONAL,
     *        extensions                   [1] IMPLICIT Extensions   OPTIONAL  }
     * 
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector seq = new ASN1EncodableVector();
        seq.add(version);

        seq.add(tsaPolicyId);
        seq.add(messageImprint);
        seq.add(serialNumber);
        seq.add(genTime);

        if (accuracy != null)
        {
            seq.add(accuracy);
        }
        
        if (ordering != null && ordering.isTrue())
        {
            seq.add(ordering);
        }
        
        if (nonce != null)
        {
            seq.add(nonce);
        }
        
        if (tsa != null)
        {
            seq.add(new DERTaggedObject(true, 0, tsa));
        }
        
        if (extensions != null)
        {
            seq.add(new DERTaggedObject(false, 1, extensions));
        }

        return new DERSequence(seq);
    }
}
