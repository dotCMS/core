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

package com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.x509;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Choice;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERBoolean;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERGeneralizedTime;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERPrintableString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;

/**
 * A declaration of majority.
 * <p/>
 * <pre>
 *           DeclarationOfMajoritySyntax ::= CHOICE
 *           {
 *             notYoungerThan [0] IMPLICIT INTEGER,
 *             fullAgeAtCountry [1] IMPLICIT SEQUENCE
 *             {
 *               fullAge BOOLEAN DEFAULT TRUE,
 *               country PrintableString (SIZE(2))
 *             }
 *             dateOfBirth [2] IMPLICIT GeneralizedTime
 *           }
 * </pre>
 * <p/>
 * fullAgeAtCountry indicates the majority of the owner with respect to the laws
 * of a specific country.
 */
public class DeclarationOfMajority
    extends ASN1Encodable
    implements ASN1Choice
{
    public static final int notYoungerThan = 0;
    public static final int fullAgeAtCountry = 1;
    public static final int dateOfBirth = 2;

    private ASN1TaggedObject declaration;

    public DeclarationOfMajority(int notYoungerThan)
    {
        declaration = new DERTaggedObject(false, 0, new DERInteger(notYoungerThan));
    }

    public DeclarationOfMajority(boolean fullAge, String country)
    {
        if (country.length() > 2)
        {
            throw new IllegalArgumentException("country can only be 2 characters");
        }

        if (fullAge)
        {
            declaration = new DERTaggedObject(false, 1, new DERSequence(new DERPrintableString(country, true)));
        }
        else
        {
            ASN1EncodableVector v = new ASN1EncodableVector();

            v.add(DERBoolean.FALSE);
            v.add(new DERPrintableString(country, true));

            declaration = new DERTaggedObject(false, 1, new DERSequence(v));
        }
    }

    public DeclarationOfMajority(DERGeneralizedTime dateOfBirth)
    {
        declaration = new DERTaggedObject(false, 2, dateOfBirth);
    }

    public static DeclarationOfMajority getInstance(Object obj)
    {
        if (obj == null || obj instanceof DeclarationOfMajority)
        {
            return (DeclarationOfMajority)obj;
        }

        if (obj instanceof ASN1TaggedObject)
        {
            return new DeclarationOfMajority((ASN1TaggedObject)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }

    private DeclarationOfMajority(ASN1TaggedObject o)
    {
        if (o.getTagNo() > 2)
        {
                throw new IllegalArgumentException("Bad tag number: " + o.getTagNo());
        }
        declaration = o;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *           DeclarationOfMajoritySyntax ::= CHOICE
     *           {
     *             notYoungerThan [0] IMPLICIT INTEGER,
     *             fullAgeAtCountry [1] IMPLICIT SEQUENCE
     *             {
     *               fullAge BOOLEAN DEFAULT TRUE,
     *               country PrintableString (SIZE(2))
     *             }
     *             dateOfBirth [2] IMPLICIT GeneralizedTime
     *           }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        return declaration;
    }

    public int getType()
    {
        return declaration.getTagNo();
    }

    /**
     * @return notYoungerThan if that's what we are, -1 otherwise
     */
    public int notYoungerThan()
    {
        if (declaration.getTagNo() != 0)
        {
            return -1;
        }

        return DERInteger.getInstance(declaration, false).getValue().intValue();
    }

    public ASN1Sequence fullAgeAtCountry()
    {
        if (declaration.getTagNo() != 1)
        {
            return null;
        }

        return ASN1Sequence.getInstance(declaration, false);
    }

    public DERGeneralizedTime getDateOfBirth()
    {
        if (declaration.getTagNo() != 2)
        {
            return null;
        }

        return DERGeneralizedTime.getInstance(declaration, false);
    }
}
