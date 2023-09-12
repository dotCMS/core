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
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;


public class Accuracy
    extends ASN1Encodable
{
    DERInteger seconds;

    DERInteger millis;

    DERInteger micros;

    // constantes
    protected static final int MIN_MILLIS = 1;

    protected static final int MAX_MILLIS = 999;

    protected static final int MIN_MICROS = 1;

    protected static final int MAX_MICROS = 999;

    protected Accuracy()
    {
    }

    public Accuracy(
        DERInteger seconds,
        DERInteger millis,
        DERInteger micros)
    {
        this.seconds = seconds;

        //Verifications
        if (millis != null
                && (millis.getValue().intValue() < MIN_MILLIS || millis
                        .getValue().intValue() > MAX_MILLIS))
        {
            throw new IllegalArgumentException(
                    "Invalid millis field : not in (1..999)");
        }
        else
        {
            this.millis = millis;
        }

        if (micros != null
                && (micros.getValue().intValue() < MIN_MICROS || micros
                        .getValue().intValue() > MAX_MICROS))
        {
            throw new IllegalArgumentException(
                    "Invalid micros field : not in (1..999)");
        }
        else
        {
            this.micros = micros;
        }

    }

    public Accuracy(ASN1Sequence seq)
    {
        seconds = null;
        millis = null;
        micros = null;

        for (int i = 0; i < seq.size(); i++)
        {
            // seconds
            if (seq.getObjectAt(i) instanceof DERInteger)
            {
                seconds = (DERInteger) seq.getObjectAt(i);
            }
            else if (seq.getObjectAt(i) instanceof DERTaggedObject)
            {
                DERTaggedObject extra = (DERTaggedObject) seq.getObjectAt(i);

                switch (extra.getTagNo())
                {
                case 0:
                    millis = DERInteger.getInstance(extra, false);
                    if (millis.getValue().intValue() < MIN_MILLIS
                            || millis.getValue().intValue() > MAX_MILLIS)
                    {
                        throw new IllegalArgumentException(
                                "Invalid millis field : not in (1..999).");
                    }
                    break;
                case 1:
                    micros = DERInteger.getInstance(extra, false);
                    if (micros.getValue().intValue() < MIN_MICROS
                            || micros.getValue().intValue() > MAX_MICROS)
                    {
                        throw new IllegalArgumentException(
                                "Invalid micros field : not in (1..999).");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalig tag number");
                }
            }
        }
    }

    public static Accuracy getInstance(Object o)
    {
        if (o == null || o instanceof Accuracy)
        {
            return (Accuracy) o;
        }
        else if (o instanceof ASN1Sequence)
        {
            return new Accuracy((ASN1Sequence) o);
        }

        throw new IllegalArgumentException(
                "Unknown object in 'Accuracy' factory : "
                        + o.getClass().getName() + ".");
    }

    public DERInteger getSeconds()
    {
        return seconds;
    }

    public DERInteger getMillis()
    {
        return millis;
    }

    public DERInteger getMicros()
    {
        return micros;
    }

    /**
     * <pre>
     * Accuracy ::= SEQUENCE {
     *             seconds        INTEGER              OPTIONAL,
     *             millis     [0] INTEGER  (1..999)    OPTIONAL,
     *             micros     [1] INTEGER  (1..999)    OPTIONAL
     *             }
     * </pre>
     */
    public DERObject toASN1Object()
    {

        ASN1EncodableVector v = new ASN1EncodableVector();
        
        if (seconds != null)
        {
            v.add(seconds);
        }
        
        if (millis != null)
        {
            v.add(new DERTaggedObject(false, 0, millis));
        }
        
        if (micros != null)
        {
            v.add(new DERTaggedObject(false, 1, micros));
        }

        return new DERSequence(v);
    }
}
