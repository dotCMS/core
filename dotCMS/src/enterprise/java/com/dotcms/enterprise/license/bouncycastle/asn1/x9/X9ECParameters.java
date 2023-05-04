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

package com.dotcms.enterprise.license.bouncycastle.asn1.x9;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECCurve;
import com.dotcms.enterprise.license.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

/**
 * ASN.1 def for Elliptic-Curve ECParameters structure. See
 * X9.62, for further details.
 */
public class X9ECParameters
    extends ASN1Encodable
    implements X9ObjectIdentifiers
{
    private static final BigInteger   ONE = BigInteger.valueOf(1);

    private X9FieldID           fieldID;
    private ECCurve             curve;
    private ECPoint             g;
    private BigInteger          n;
    private BigInteger          h;
    private byte[]              seed;

    public X9ECParameters(
        ASN1Sequence  seq)
    {
        if (!(seq.getObjectAt(0) instanceof DERInteger)
           || !((DERInteger)seq.getObjectAt(0)).getValue().equals(ONE))
        {
            throw new IllegalArgumentException("bad version in X9ECParameters");
        }

        X9Curve     x9c = new X9Curve(
                        new X9FieldID((ASN1Sequence)seq.getObjectAt(1)),
                        (ASN1Sequence)seq.getObjectAt(2));

        this.curve = x9c.getCurve();
        this.g = new X9ECPoint(curve, (ASN1OctetString)seq.getObjectAt(3)).getPoint();
        this.n = ((DERInteger)seq.getObjectAt(4)).getValue();
        this.seed = x9c.getSeed();

        if (seq.size() == 6)
        {
            this.h = ((DERInteger)seq.getObjectAt(5)).getValue();
        }
    }

    public X9ECParameters(
        ECCurve     curve,
        ECPoint     g,
        BigInteger  n)
    {
        this(curve, g, n, ONE, null);
    }

    public X9ECParameters(
        ECCurve     curve,
        ECPoint     g,
        BigInteger  n,
        BigInteger  h)
    {
        this(curve, g, n, h, null);
    }

    public X9ECParameters(
        ECCurve     curve,
        ECPoint     g,
        BigInteger  n,
        BigInteger  h,
        byte[]      seed)
    {
        this.curve = curve;
        this.g = g;
        this.n = n;
        this.h = h;
        this.seed = seed;

        if (curve instanceof ECCurve.Fp)
        {
            this.fieldID = new X9FieldID(((ECCurve.Fp)curve).getQ());
        }
        else
        {
            if (curve instanceof ECCurve.F2m)
            {
                ECCurve.F2m curveF2m = (ECCurve.F2m)curve;
                this.fieldID = new X9FieldID(curveF2m.getM(), curveF2m.getK1(),
                    curveF2m.getK2(), curveF2m.getK3());
            }
        }
    }

    public ECCurve getCurve()
    {
        return curve;
    }

    public ECPoint getG()
    {
        return g;
    }

    public BigInteger getN()
    {
        return n;
    }

    public BigInteger getH()
    {
        if (h == null)
        {
            return ONE;        // TODO - this should be calculated, it will cause issues with custom curves.
        }

        return h;
    }

    public byte[] getSeed()
    {
        return seed;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     *  ECParameters ::= SEQUENCE {
     *      version         INTEGER { ecpVer1(1) } (ecpVer1),
     *      fieldID         FieldID {{FieldTypes}},
     *      curve           X9Curve,
     *      base            X9ECPoint,
     *      order           INTEGER,
     *      cofactor        INTEGER OPTIONAL
     *  }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(new DERInteger(1));
        v.add(fieldID);
        v.add(new X9Curve(curve, seed));
        v.add(new X9ECPoint(g));
        v.add(new DERInteger(n));

        if (h != null)
        {
            v.add(new DERInteger(h));
        }

        return new DERSequence(v);
    }
}
