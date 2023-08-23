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
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERGeneralizedTime;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.X509Extensions;

public class ResponseData
    extends ASN1Encodable
{
    private static final DERInteger V1 = new DERInteger(0);
    
    private boolean             versionPresent;
    
    private DERInteger          version;
    private ResponderID         responderID;
    private DERGeneralizedTime  producedAt;
    private ASN1Sequence        responses;
    private X509Extensions      responseExtensions;

    public ResponseData(
        DERInteger          version,
        ResponderID         responderID,
        DERGeneralizedTime  producedAt,
        ASN1Sequence        responses,
        X509Extensions      responseExtensions)
    {
        this.version = version;
        this.responderID = responderID;
        this.producedAt = producedAt;
        this.responses = responses;
        this.responseExtensions = responseExtensions;
    }
    
    public ResponseData(
        ResponderID         responderID,
        DERGeneralizedTime  producedAt,
        ASN1Sequence        responses,
        X509Extensions      responseExtensions)
    {
        this(V1, responderID, producedAt, responses, responseExtensions);
    }
    
    public ResponseData(
        ASN1Sequence    seq)
    {
        int index = 0;

        if (seq.getObjectAt(0) instanceof ASN1TaggedObject)
        {
            ASN1TaggedObject    o = (ASN1TaggedObject)seq.getObjectAt(0);

            if (o.getTagNo() == 0)
            {
                this.versionPresent = true;
                this.version = DERInteger.getInstance(
                                (ASN1TaggedObject)seq.getObjectAt(0), true);
                index++;
            }
            else
            {
                this.version = V1;
            }
        }
        else
        {
            this.version = V1;
        }

        this.responderID = ResponderID.getInstance(seq.getObjectAt(index++));
        this.producedAt = (DERGeneralizedTime)seq.getObjectAt(index++);
        this.responses = (ASN1Sequence)seq.getObjectAt(index++);

        if (seq.size() > index)
        {
            this.responseExtensions = X509Extensions.getInstance(
                                (ASN1TaggedObject)seq.getObjectAt(index), true);
        }
    }

    public static ResponseData getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static ResponseData getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof ResponseData)
        {
            return (ResponseData)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new ResponseData((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public DERInteger getVersion()
    {
        return version;
    }

    public ResponderID getResponderID()
    {
        return responderID;
    }

    public DERGeneralizedTime getProducedAt()
    {
        return producedAt;
    }

    public ASN1Sequence getResponses()
    {
        return responses;
    }

    public X509Extensions getResponseExtensions()
    {
        return responseExtensions;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * ResponseData ::= SEQUENCE {
     *     version              [0] EXPLICIT Version DEFAULT v1,
     *     responderID              ResponderID,
     *     producedAt               GeneralizedTime,
     *     responses                SEQUENCE OF SingleResponse,
     *     responseExtensions   [1] EXPLICIT Extensions OPTIONAL }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        if (versionPresent || !version.equals(V1))
        {
            v.add(new DERTaggedObject(true, 0, version));
        }

        v.add(responderID);
        v.add(producedAt);
        v.add(responses);
        if (responseExtensions != null)
        {
            v.add(new DERTaggedObject(true, 1, responseExtensions));
        }

        return new DERSequence(v);
    }
}
