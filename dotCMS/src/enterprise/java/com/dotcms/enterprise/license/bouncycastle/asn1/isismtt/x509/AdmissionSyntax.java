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
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.GeneralName;

import java.util.Enumeration;

/**
 * Attribute to indicate admissions to certain professions.
 * <p/>
 * <pre>
 *     AdmissionSyntax ::= SEQUENCE
 *     {
 *       admissionAuthority GeneralName OPTIONAL,
 *       contentsOfAdmissions SEQUENCE OF Admissions
 *     }
 * <p/>
 *     Admissions ::= SEQUENCE
 *     {
 *       admissionAuthority [0] EXPLICIT GeneralName OPTIONAL
 *       namingAuthority [1] EXPLICIT NamingAuthority OPTIONAL
 *       professionInfos SEQUENCE OF ProfessionInfo
 *     }
 * <p/>
 *     NamingAuthority ::= SEQUENCE
 *     {
 *       namingAuthorityId OBJECT IDENTIFIER OPTIONAL,
 *       namingAuthorityUrl IA5String OPTIONAL,
 *       namingAuthorityText DirectoryString(SIZE(1..128)) OPTIONAL
 *     }
 * <p/>
 *     ProfessionInfo ::= SEQUENCE
 *     {
 *       namingAuthority [0] EXPLICIT NamingAuthority OPTIONAL,
 *       professionItems SEQUENCE OF DirectoryString (SIZE(1..128)),
 *       professionOIDs SEQUENCE OF OBJECT IDENTIFIER OPTIONAL,
 *       registrationNumber PrintableString(SIZE(1..128)) OPTIONAL,
 *       addProfessionInfo OCTET STRING OPTIONAL
 *     }
 * </pre>
 * <p/>
 * <p/>
 * ISIS-MTT PROFILE: The relatively complex structure of AdmissionSyntax
 * supports the following concepts and requirements:
 * <ul>
 * <li> External institutions (e.g. professional associations, chambers, unions,
 * administrative bodies, companies, etc.), which are responsible for granting
 * and verifying professional admissions, are indicated by means of the data
 * field admissionAuthority. An admission authority is indicated by a
 * GeneralName object. Here an X.501 directory name (distinguished name) can be
 * indicated in the field directoryName, a URL address can be indicated in the
 * field uniformResourceIdentifier, and an object identifier can be indicated in
 * the field registeredId.
 * <li> The names of authorities which are responsible for the administration of
 * title registers are indicated in the data field namingAuthority. The name of
 * the authority can be identified by an object identifier in the field
 * namingAuthorityId, by means of a text string in the field
 * namingAuthorityText, by means of a URL address in the field
 * namingAuthorityUrl, or by a combination of them. For example, the text string
 * can contain the name of the authority, the country and the name of the title
 * register. The URL-option refers to a web page which contains lists with
 * �officially� registered professions (text and possibly OID) as well as
 * further information on these professions. Object identifiers for the
 * component namingAuthorityId are grouped under the OID-branch
 * id-isis-at-namingAuthorities and must be applied for.
 * <li>See
 * http://www.teletrust.de/anwend.asp?Id=30200&Sprache=E_&HomePG=0 for
 * an application form and http://www.teletrust.de/links.asp?id=30220,11
 * for an overview of registered naming authorities.
 * <li> By means of the data type ProfessionInfo certain professions,
 * specializations, disciplines, fields of activity, etc. are identified. A
 * profession is represented by one or more text strings, resp. profession OIDs
 * in the fields professionItems and professionOIDs and by a registration number
 * in the field registrationNumber. An indication in text form must always be
 * present, whereas the other indications are optional. The component
 * addProfessionInfo may contain additional applicationspecific information in
 * DER-encoded form.
 * </ul>
 * <p/>
 * By means of different namingAuthority-OIDs or profession OIDs hierarchies of
 * professions, specializations, disciplines, fields of activity, etc. can be
 * expressed. The issuing admission authority should always be indicated (field
 * admissionAuthority), whenever a registration number is presented. Still,
 * information on admissions can be given without indicating an admission or a
 * naming authority by the exclusive use of the component professionItems. In
 * this case the certification authority is responsible for the verification of
 * the admission information.
 * <p/>
 * <p/>
 * <p/>
 * This attribute is single-valued. Still, several admissions can be captured in
 * the sequence structure of the component contentsOfAdmissions of
 * AdmissionSyntax or in the component professionInfos of Admissions. The
 * component admissionAuthority of AdmissionSyntax serves as default value for
 * the component admissionAuthority of Admissions. Within the latter component
 * the default value can be overwritten, in case that another authority is
 * responsible. The component namingAuthority of Admissions serves as a default
 * value for the component namingAuthority of ProfessionInfo. Within the latter
 * component the default value can be overwritten, in case that another naming
 * authority needs to be recorded.
 * <p/>
 * The length of the string objects is limited to 128 characters. It is
 * recommended to indicate a namingAuthorityURL in all issued attribute
 * certificates. If a namingAuthorityURL is indicated, the field professionItems
 * of ProfessionInfo should contain only registered titles. If the field
 * professionOIDs exists, it has to contain the OIDs of the professions listed
 * in professionItems in the same order. In general, the field professionInfos
 * should contain only one entry, unless the admissions that are to be listed
 * are logically connected (e.g. they have been issued under the same admission
 * number).
 *
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.x509.Admissions
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.x509.ProfessionInfo
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.x509.NamingAuthority
 */
public class AdmissionSyntax
    extends ASN1Encodable
{

    private GeneralName admissionAuthority;

    private ASN1Sequence contentsOfAdmissions;

    public static AdmissionSyntax getInstance(Object obj)
    {
        if (obj == null || obj instanceof AdmissionSyntax)
        {
            return (AdmissionSyntax)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new AdmissionSyntax((ASN1Sequence)obj);
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
     *     AdmissionSyntax ::= SEQUENCE
     *     {
     *       admissionAuthority GeneralName OPTIONAL,
     *       contentsOfAdmissions SEQUENCE OF Admissions
     *     }
     * <p/>
     *     Admissions ::= SEQUENCE
     *     {
     *       admissionAuthority [0] EXPLICIT GeneralName OPTIONAL
     *       namingAuthority [1] EXPLICIT NamingAuthority OPTIONAL
     *       professionInfos SEQUENCE OF ProfessionInfo
     *     }
     * <p/>
     *     NamingAuthority ::= SEQUENCE
     *     {
     *       namingAuthorityId OBJECT IDENTIFIER OPTIONAL,
     *       namingAuthorityUrl IA5String OPTIONAL,
     *       namingAuthorityText DirectoryString(SIZE(1..128)) OPTIONAL
     *     }
     * <p/>
     *     ProfessionInfo ::= SEQUENCE
     *     {
     *       namingAuthority [0] EXPLICIT NamingAuthority OPTIONAL,
     *       professionItems SEQUENCE OF DirectoryString (SIZE(1..128)),
     *       professionOIDs SEQUENCE OF OBJECT IDENTIFIER OPTIONAL,
     *       registrationNumber PrintableString(SIZE(1..128)) OPTIONAL,
     *       addProfessionInfo OCTET STRING OPTIONAL
     *     }
     * </pre>
     *
     * @param seq The ASN.1 sequence.
     */
    private AdmissionSyntax(ASN1Sequence seq)
    {
        switch (seq.size())
        {
        case 1:
            contentsOfAdmissions = DERSequence.getInstance(seq.getObjectAt(0));
            break;
        case 2:
            admissionAuthority = GeneralName.getInstance(seq.getObjectAt(0));
            contentsOfAdmissions = DERSequence.getInstance(seq.getObjectAt(1));
            break;
        default:
            throw new IllegalArgumentException("Bad sequence size: " + seq.size());
        }
    }

    /**
     * Constructor from given details.
     *
     * @param admissionAuthority   The admission authority.
     * @param contentsOfAdmissions The admissions.
     */
    public AdmissionSyntax(GeneralName admissionAuthority, ASN1Sequence contentsOfAdmissions)
    {
        this.admissionAuthority = admissionAuthority;
        this.contentsOfAdmissions = contentsOfAdmissions;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *     AdmissionSyntax ::= SEQUENCE
     *     {
     *       admissionAuthority GeneralName OPTIONAL,
     *       contentsOfAdmissions SEQUENCE OF Admissions
     *     }
     * <p/>
     *     Admissions ::= SEQUENCE
     *     {
     *       admissionAuthority [0] EXPLICIT GeneralName OPTIONAL
     *       namingAuthority [1] EXPLICIT NamingAuthority OPTIONAL
     *       professionInfos SEQUENCE OF ProfessionInfo
     *     }
     * <p/>
     *     NamingAuthority ::= SEQUENCE
     *     {
     *       namingAuthorityId OBJECT IDENTIFIER OPTIONAL,
     *       namingAuthorityUrl IA5String OPTIONAL,
     *       namingAuthorityText DirectoryString(SIZE(1..128)) OPTIONAL
     *     }
     * <p/>
     *     ProfessionInfo ::= SEQUENCE
     *     {
     *       namingAuthority [0] EXPLICIT NamingAuthority OPTIONAL,
     *       professionItems SEQUENCE OF DirectoryString (SIZE(1..128)),
     *       professionOIDs SEQUENCE OF OBJECT IDENTIFIER OPTIONAL,
     *       registrationNumber PrintableString(SIZE(1..128)) OPTIONAL,
     *       addProfessionInfo OCTET STRING OPTIONAL
     *     }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        if (admissionAuthority != null)
        {
            vec.add(admissionAuthority);
        }
        vec.add(contentsOfAdmissions);
        return new DERSequence(vec);
    }

    /**
     * @return Returns the admissionAuthority if present, null otherwise.
     */
    public GeneralName getAdmissionAuthority()
    {
        return admissionAuthority;
    }

    /**
     * @return Returns the contentsOfAdmissions.
     */
    public Admissions[] getContentsOfAdmissions()
    {
        Admissions[] admissions = new Admissions[contentsOfAdmissions.size()];
        int count = 0;
        for (Enumeration e = contentsOfAdmissions.getObjects(); e.hasMoreElements();)
        {
            admissions[count++] = Admissions.getInstance(e.nextElement());
        }
        return admissions;
    }
}
