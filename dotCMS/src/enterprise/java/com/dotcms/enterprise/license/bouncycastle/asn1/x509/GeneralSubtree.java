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
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;

import java.math.BigInteger;

/**
 * Class for containing a restriction object subtrees in NameConstraints. See
 * RFC 3280.
 * 
 * <pre>
 *       
 *       GeneralSubtree ::= SEQUENCE 
 *       {
 *         base                    GeneralName,
 *         minimum         [0]     BaseDistance DEFAULT 0,
 *         maximum         [1]     BaseDistance OPTIONAL 
 *       }
 * </pre>
 * 
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.x509.NameConstraints
 * 
 */
public class GeneralSubtree 
    extends ASN1Encodable 
{
    private static final BigInteger ZERO = BigInteger.valueOf(0);

    private GeneralName base;

    private DERInteger minimum;

    private DERInteger maximum;

    public GeneralSubtree(
        ASN1Sequence seq) 
    {
        base = GeneralName.getInstance(seq.getObjectAt(0));

        switch (seq.size()) 
        {
        case 1:
            break;
        case 2:
            ASN1TaggedObject o = ASN1TaggedObject.getInstance(seq.getObjectAt(1));
            switch (o.getTagNo()) 
            {
            case 0:
                minimum = DERInteger.getInstance(o, false);
                break;
            case 1:
                maximum = DERInteger.getInstance(o, false);
                break;
            default:
                throw new IllegalArgumentException("Bad tag number: "
                        + o.getTagNo());
            }
            break;
        case 3:
            minimum = DERInteger.getInstance(ASN1TaggedObject.getInstance(seq.getObjectAt(1)));
            maximum = DERInteger.getInstance(ASN1TaggedObject.getInstance(seq.getObjectAt(2)));
            break;
        default:
            throw new IllegalArgumentException("Bad sequence size: "
                    + seq.size());
        }
    }

    /**
     * Constructor from a given details.
     * 
     * According RFC 3280, the minimum and maximum fields are not used with any
     * name forms, thus minimum MUST be zero, and maximum MUST be absent.
     * <p>
     * If minimum is <code>null</code>, zero is assumed, if
     * maximum is <code>null</code>, maximum is absent.
     * 
     * @param base
     *            A restriction.
     * @param minimum
     *            Minimum
     * 
     * @param maximum
     *            Maximum
     */
    public GeneralSubtree(
        GeneralName base,
        BigInteger minimum,
        BigInteger maximum)
    {
        this.base = base;
        if (maximum != null)
        {
            this.maximum = new DERInteger(maximum);
        }
        if (minimum == null)
        {
            this.minimum = null;
        }
        else
        {
            this.minimum = new DERInteger(minimum);
        }
    }

    public GeneralSubtree(GeneralName base)
    {
        this(base, null, null);
    }

    public static GeneralSubtree getInstance(
        ASN1TaggedObject o,
        boolean explicit)
    {
        return new GeneralSubtree(ASN1Sequence.getInstance(o, explicit));
    }

    public static GeneralSubtree getInstance(
        Object obj)
    {
        if (obj == null)
        {
            return null;
        }

        if (obj instanceof GeneralSubtree)
        {
            return (GeneralSubtree) obj;
        }

        return new GeneralSubtree(ASN1Sequence.getInstance(obj));
    }

    public GeneralName getBase()
    {
        return base;
    }

    public BigInteger getMinimum()
    {
        if (minimum == null)
        {
            return ZERO;
        }

        return minimum.getValue();
    }

    public BigInteger getMaximum()
    {
        if (maximum == null)
        {
            return null;
        }

        return maximum.getValue();
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * 
     * Returns:
     * 
     * <pre>
     *       GeneralSubtree ::= SEQUENCE 
     *       {
     *         base                    GeneralName,
     *         minimum         [0]     BaseDistance DEFAULT 0,
     *         maximum         [1]     BaseDistance OPTIONAL 
     *       }
     * </pre>
     * 
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(base);

        if (minimum != null && !minimum.getValue().equals(ZERO))
        {
            v.add(new DERTaggedObject(false, 0, minimum));
        }

        if (maximum != null)
        {
            v.add(new DERTaggedObject(false, 1, maximum));
        }

        return new DERSequence(v);
    }
}
