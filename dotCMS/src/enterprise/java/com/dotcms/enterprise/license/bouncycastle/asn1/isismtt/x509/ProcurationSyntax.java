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

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEREncodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERPrintableString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x500.DirectoryString;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.GeneralName;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.IssuerSerial;

import java.util.Enumeration;

/**
 * Attribute to indicate that the certificate holder may sign in the name of a
 * third person.
 * <p>
 * ISIS-MTT PROFILE: The corresponding ProcurationSyntax contains either the
 * name of the person who is represented (subcomponent thirdPerson) or a
 * reference to his/her base certificate (in the component signingFor,
 * subcomponent certRef), furthermore the optional components country and
 * typeSubstitution to indicate the country whose laws apply, and respectively
 * the type of procuration (e.g. manager, procuration, custody).
 * <p>
 * ISIS-MTT PROFILE: The GeneralName MUST be of type directoryName and MAY only
 * contain: - RFC3039 attributes, except pseudonym (countryName, commonName,
 * surname, givenName, serialNumber, organizationName, organizationalUnitName,
 * stateOrProvincename, localityName, postalAddress) and - SubjectDirectoryName
 * attributes (title, dateOfBirth, placeOfBirth, gender, countryOfCitizenship,
 * countryOfResidence and NameAtBirth).
 * 
 * <pre>
 *               ProcurationSyntax ::= SEQUENCE {
 *                 country [1] EXPLICIT PrintableString(SIZE(2)) OPTIONAL,
 *                 typeOfSubstitution [2] EXPLICIT DirectoryString (SIZE(1..128)) OPTIONAL,
 *                 signingFor [3] EXPLICIT SigningFor 
 *               }
 *               
 *               SigningFor ::= CHOICE 
 *               { 
 *                 thirdPerson GeneralName,
 *                 certRef IssuerSerial 
 *               }
 * </pre>
 * 
 */
public class ProcurationSyntax
    extends ASN1Encodable
{
    private String country;
    private DirectoryString typeOfSubstitution;

    private GeneralName thirdPerson;
    private IssuerSerial certRef;

    public static ProcurationSyntax getInstance(Object obj)
    {
        if (obj == null || obj instanceof ProcurationSyntax)
        {
            return (ProcurationSyntax)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new ProcurationSyntax((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }

    /**
     * Constructor from ASN1Sequence.
     * <p/>
     * The sequence is of type ProcurationSyntax:
     * <p/>
     * <pre>
     *               ProcurationSyntax ::= SEQUENCE {
     *                 country [1] EXPLICIT PrintableString(SIZE(2)) OPTIONAL,
     *                 typeOfSubstitution [2] EXPLICIT DirectoryString (SIZE(1..128)) OPTIONAL,
     *                 signingFor [3] EXPLICIT SigningFor
     *               }
     * <p/>
     *               SigningFor ::= CHOICE
     *               {
     *                 thirdPerson GeneralName,
     *                 certRef IssuerSerial
     *               }
     * </pre>
     *
     * @param seq The ASN.1 sequence.
     */
    private ProcurationSyntax(ASN1Sequence seq)
    {
        if (seq.size() < 1 || seq.size() > 3)
        {
            throw new IllegalArgumentException("Bad sequence size: " + seq.size());
        }
        Enumeration e = seq.getObjects();

        while (e.hasMoreElements())
        {
            ASN1TaggedObject o = ASN1TaggedObject.getInstance(e.nextElement());
            switch (o.getTagNo())
            {
                case 1:
                    country = DERPrintableString.getInstance(o, true).getString();
                    break;
                case 2:
                    typeOfSubstitution = DirectoryString.getInstance(o, true);
                    break;
                case 3:
                    DEREncodable signingFor = o.getObject();
                    if (signingFor instanceof ASN1TaggedObject)
                    {
                        thirdPerson = GeneralName.getInstance(signingFor);
                    }
                    else
                    {
                        certRef = IssuerSerial.getInstance(signingFor);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Bad tag number: " + o.getTagNo());
            }
        }
    }

    /**
     * Constructor from a given details.
     * <p/>
     * <p/>
     * Either <code>generalName</code> or <code>certRef</code> MUST be
     * <code>null</code>.
     *
     * @param country            The country code whose laws apply.
     * @param typeOfSubstitution The type of procuration.
     * @param certRef            Reference to certificate of the person who is represented.
     */
    public ProcurationSyntax(
        String country,
        DirectoryString typeOfSubstitution,
        IssuerSerial certRef)
    {
        this.country = country;
        this.typeOfSubstitution = typeOfSubstitution;
        this.thirdPerson = null;
        this.certRef = certRef;
    }

    /**
     * Constructor from a given details.
     * <p/>
     * <p/>
     * Either <code>generalName</code> or <code>certRef</code> MUST be
     * <code>null</code>.
     *
     * @param country            The country code whose laws apply.
     * @param typeOfSubstitution The type of procuration.
     * @param thirdPerson        The GeneralName of the person who is represented.
     */
    public ProcurationSyntax(
        String country,
        DirectoryString typeOfSubstitution,
        GeneralName thirdPerson)
    {
        this.country = country;
        this.typeOfSubstitution = typeOfSubstitution;
        this.thirdPerson = thirdPerson;
        this.certRef = null;
    }

    public String getCountry()
    {
        return country;
    }

    public DirectoryString getTypeOfSubstitution()
    {
        return typeOfSubstitution;
    }

    public GeneralName getThirdPerson()
    {
        return thirdPerson;
    }

    public IssuerSerial getCertRef()
    {
        return certRef;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *               ProcurationSyntax ::= SEQUENCE {
     *                 country [1] EXPLICIT PrintableString(SIZE(2)) OPTIONAL,
     *                 typeOfSubstitution [2] EXPLICIT DirectoryString (SIZE(1..128)) OPTIONAL,
     *                 signingFor [3] EXPLICIT SigningFor
     *               }
     * <p/>
     *               SigningFor ::= CHOICE
     *               {
     *                 thirdPerson GeneralName,
     *                 certRef IssuerSerial
     *               }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        if (country != null)
        {
            vec.add(new DERTaggedObject(true, 1, new DERPrintableString(country, true)));
        }
        if (typeOfSubstitution != null)
        {
            vec.add(new DERTaggedObject(true, 2, typeOfSubstitution));
        }
        if (thirdPerson != null)
        {
            vec.add(new DERTaggedObject(true, 3, thirdPerson));
        }
        else
        {
            vec.add(new DERTaggedObject(true, 3, certRef));
        }

        return new DERSequence(vec);
    }
}
