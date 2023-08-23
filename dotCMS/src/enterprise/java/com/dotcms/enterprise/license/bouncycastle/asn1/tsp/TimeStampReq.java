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
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERBoolean;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.X509Extensions;

public class TimeStampReq
    extends ASN1Encodable
{
    DERInteger version;

    MessageImprint messageImprint;

    DERObjectIdentifier tsaPolicy;

    DERInteger nonce;

    DERBoolean certReq;

    X509Extensions extensions;

    public static TimeStampReq getInstance(Object o)
    {
        if (o == null || o instanceof TimeStampReq)
        {
            return (TimeStampReq) o;
        }
        else if (o instanceof ASN1Sequence)
        {
            return new TimeStampReq((ASN1Sequence) o);
        }

        throw new IllegalArgumentException(
                "Unknown object in 'TimeStampReq' factory : "
                        + o.getClass().getName() + ".");
    }

    public TimeStampReq(ASN1Sequence seq)
    {
        int nbObjects = seq.size();

        int seqStart = 0;

        // version
        version = DERInteger.getInstance(seq.getObjectAt(seqStart));

        seqStart++;

        // messageImprint
        messageImprint = MessageImprint.getInstance(seq.getObjectAt(seqStart));

        seqStart++;

        for (int opt = seqStart; opt < nbObjects; opt++)
        {
            // tsaPolicy
            if (seq.getObjectAt(opt) instanceof DERObjectIdentifier)
            {
                tsaPolicy = DERObjectIdentifier.getInstance(seq.getObjectAt(opt));
            }
            // nonce
            else if (seq.getObjectAt(opt) instanceof DERInteger)
            {
                nonce = DERInteger.getInstance(seq.getObjectAt(opt));
            }
            // certReq
            else if (seq.getObjectAt(opt) instanceof DERBoolean)
            {
                certReq = DERBoolean.getInstance(seq.getObjectAt(opt));
            }
            // extensions
            else if (seq.getObjectAt(opt) instanceof ASN1TaggedObject)
            {
                ASN1TaggedObject    tagged = (ASN1TaggedObject)seq.getObjectAt(opt);
                if (tagged.getTagNo() == 0)
                {
                    extensions = X509Extensions.getInstance(tagged, false);
                }
            }
        }
    }

    public TimeStampReq(
        MessageImprint      messageImprint,
        DERObjectIdentifier tsaPolicy,
        DERInteger          nonce,
        DERBoolean          certReq,
        X509Extensions      extensions)
    {
        // default
        version = new DERInteger(1);

        this.messageImprint = messageImprint;
        this.tsaPolicy = tsaPolicy;
        this.nonce = nonce;
        this.certReq = certReq;
        this.extensions = extensions;
    }

    public DERInteger getVersion()
    {
        return version;
    }

    public MessageImprint getMessageImprint()
    {
        return messageImprint;
    }

    public DERObjectIdentifier getReqPolicy()
    {
        return tsaPolicy;
    }

    public DERInteger getNonce()
    {
        return nonce;
    }

    public DERBoolean getCertReq()
    {
        return certReq;
    }

    public X509Extensions getExtensions()
    {
        return extensions;
    }

    /**
     * <pre>
     * TimeStampReq ::= SEQUENCE  {
     *  version                      INTEGER  { v1(1) },
     *  messageImprint               MessageImprint,
     *    --a hash algorithm OID and the hash value of the data to be
     *    --time-stamped
     *  reqPolicy             TSAPolicyId              OPTIONAL,
     *  nonce                 INTEGER                  OPTIONAL,
     *  certReq               BOOLEAN                  DEFAULT FALSE,
     *  extensions            [0] IMPLICIT Extensions  OPTIONAL
     * }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        
        v.add(version);
        v.add(messageImprint);
        
        if (tsaPolicy != null)
        {
            v.add(tsaPolicy);
        }
        
        if (nonce != null)
        {
            v.add(nonce);
        }
        
        if (certReq != null && certReq.isTrue())
        {
            v.add(certReq);
        }
        
        if (extensions != null)
        {
            v.add(new DERTaggedObject(false, 0, extensions));
        }

        return new DERSequence(v);
    }
}
