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

import java.util.Enumeration;

import com.dotcms.enterprise.license.bouncycastle.asn1.*;

public class CrlID
    extends ASN1Encodable
{
    DERIA5String        crlUrl;
    DERInteger          crlNum;
    DERGeneralizedTime  crlTime;

    public CrlID(
        ASN1Sequence    seq)
    {
        Enumeration    e = seq.getObjects();

        while (e.hasMoreElements())
        {
            ASN1TaggedObject    o = (ASN1TaggedObject)e.nextElement();

            switch (o.getTagNo())
            {
            case 0:
                crlUrl = DERIA5String.getInstance(o, true);
                break;
            case 1:
                crlNum = DERInteger.getInstance(o, true);
                break;
            case 2:
                crlTime = DERGeneralizedTime.getInstance(o, true);
                break;
            default:
                throw new IllegalArgumentException(
                        "unknown tag number: " + o.getTagNo());
            }
        }
    }

    public DERIA5String getCrlUrl()
    {
        return crlUrl;
    }

    public DERInteger getCrlNum()
    {
        return crlNum;
    }

    public DERGeneralizedTime getCrlTime()
    {
        return crlTime;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * CrlID ::= SEQUENCE {
     *     crlUrl               [0]     EXPLICIT IA5String OPTIONAL,
     *     crlNum               [1]     EXPLICIT INTEGER OPTIONAL,
     *     crlTime              [2]     EXPLICIT GeneralizedTime OPTIONAL }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector    v = new ASN1EncodableVector();

        if (crlUrl != null)
        {
            v.add(new DERTaggedObject(true, 0, crlUrl));
        }

        if (crlNum != null)
        {
            v.add(new DERTaggedObject(true, 1, crlNum));
        }

        if (crlTime != null)
        {
            v.add(new DERTaggedObject(true, 2, crlTime));
        }

        return new DERSequence(v);
    }
}
