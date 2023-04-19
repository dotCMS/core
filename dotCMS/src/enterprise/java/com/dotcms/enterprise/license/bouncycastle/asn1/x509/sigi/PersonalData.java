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

package com.dotcms.enterprise.license.bouncycastle.asn1.x509.sigi;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERGeneralizedTime;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERPrintableString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x500.DirectoryString;

import java.math.BigInteger;
import java.util.Enumeration;

/**
 * Contains personal data for the otherName field in the subjectAltNames
 * extension.
 * <p/>
 * <pre>
 *     PersonalData ::= SEQUENCE {
 *       nameOrPseudonym NameOrPseudonym,
 *       nameDistinguisher [0] INTEGER OPTIONAL,
 *       dateOfBirth [1] GeneralizedTime OPTIONAL,
 *       placeOfBirth [2] DirectoryString OPTIONAL,
 *       gender [3] PrintableString OPTIONAL,
 *       postalAddress [4] DirectoryString OPTIONAL
 *       }
 * </pre>
 *
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.x509.sigi.NameOrPseudonym
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.x509.sigi.SigIObjectIdentifiers
 */
public class PersonalData
    extends ASN1Encodable
{
    private NameOrPseudonym nameOrPseudonym;
    private BigInteger nameDistinguisher;
    private DERGeneralizedTime dateOfBirth;
    private DirectoryString placeOfBirth;
    private String gender;
    private DirectoryString postalAddress;

    public static PersonalData getInstance(Object obj)
    {
        if (obj == null || obj instanceof PersonalData)
        {
            return (PersonalData)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new PersonalData((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * Constructor from ASN1Sequence.
     * <p/>
     * The sequence is of type NameOrPseudonym:
     * <p/>
     * <pre>
     *     PersonalData ::= SEQUENCE {
     *       nameOrPseudonym NameOrPseudonym,
     *       nameDistinguisher [0] INTEGER OPTIONAL,
     *       dateOfBirth [1] GeneralizedTime OPTIONAL,
     *       placeOfBirth [2] DirectoryString OPTIONAL,
     *       gender [3] PrintableString OPTIONAL,
     *       postalAddress [4] DirectoryString OPTIONAL
     *       }
     * </pre>
     *
     * @param seq The ASN.1 sequence.
     */
    private PersonalData(ASN1Sequence seq)
    {
        if (seq.size() < 1)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }

        Enumeration e = seq.getObjects();

        nameOrPseudonym = NameOrPseudonym.getInstance(e.nextElement());

        while (e.hasMoreElements())
        {
            ASN1TaggedObject o = ASN1TaggedObject.getInstance(e.nextElement());
            int tag = o.getTagNo();
            switch (tag)
            {
                case 0:
                    nameDistinguisher = DERInteger.getInstance(o, false).getValue();
                    break;
                case 1:
                    dateOfBirth = DERGeneralizedTime.getInstance(o, false);
                    break;
                case 2:
                    placeOfBirth = DirectoryString.getInstance(o, true);
                    break;
                case 3:
                    gender = DERPrintableString.getInstance(o, false).getString();
                    break;
                case 4:
                    postalAddress = DirectoryString.getInstance(o, true);
                    break;
                default:
                    throw new IllegalArgumentException("Bad tag number: " + o.getTagNo());
            }
        }
    }

    /**
     * Constructor from a given details.
     *
     * @param nameOrPseudonym   Name or pseudonym.
     * @param nameDistinguisher Name distinguisher.
     * @param dateOfBirth       Date of birth.
     * @param placeOfBirth      Place of birth.
     * @param gender            Gender.
     * @param postalAddress     Postal Address.
     */
    public PersonalData(NameOrPseudonym nameOrPseudonym,
                        BigInteger nameDistinguisher, DERGeneralizedTime dateOfBirth,
                        DirectoryString placeOfBirth, String gender, DirectoryString postalAddress)
    {
        this.nameOrPseudonym = nameOrPseudonym;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.nameDistinguisher = nameDistinguisher;
        this.postalAddress = postalAddress;
        this.placeOfBirth = placeOfBirth;
    }

    public NameOrPseudonym getNameOrPseudonym()
    {
        return nameOrPseudonym;
    }

    public BigInteger getNameDistinguisher()
    {
        return nameDistinguisher;
    }

    public DERGeneralizedTime getDateOfBirth()
    {
        return dateOfBirth;
    }

    public DirectoryString getPlaceOfBirth()
    {
        return placeOfBirth;
    }

    public String getGender()
    {
        return gender;
    }

    public DirectoryString getPostalAddress()
    {
        return postalAddress;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *     PersonalData ::= SEQUENCE {
     *       nameOrPseudonym NameOrPseudonym,
     *       nameDistinguisher [0] INTEGER OPTIONAL,
     *       dateOfBirth [1] GeneralizedTime OPTIONAL,
     *       placeOfBirth [2] DirectoryString OPTIONAL,
     *       gender [3] PrintableString OPTIONAL,
     *       postalAddress [4] DirectoryString OPTIONAL
     *       }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        vec.add(nameOrPseudonym);
        if (nameDistinguisher != null)
        {
            vec.add(new DERTaggedObject(false, 0, new DERInteger(nameDistinguisher)));
        }
        if (dateOfBirth != null)
        {
            vec.add(new DERTaggedObject(false, 1, dateOfBirth));
        }
        if (placeOfBirth != null)
        {
            vec.add(new DERTaggedObject(true, 2, placeOfBirth));
        }
        if (gender != null)
        {
            vec.add(new DERTaggedObject(false, 3, new DERPrintableString(gender, true)));
        }
        if (postalAddress != null)
        {
            vec.add(new DERTaggedObject(true, 4, postalAddress));
        }
        return new DERSequence(vec);
    }
}
