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
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEREncodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEROctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERPrintableString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x500.DirectoryString;

import java.util.Enumeration;

/**
 * Professions, specializations, disciplines, fields of activity, etc.
 * 
 * <pre>
 *               ProfessionInfo ::= SEQUENCE 
 *               {
 *                 namingAuthority [0] EXPLICIT NamingAuthority OPTIONAL,
 *                 professionItems SEQUENCE OF DirectoryString (SIZE(1..128)),
 *                 professionOIDs SEQUENCE OF OBJECT IDENTIFIER OPTIONAL,
 *                 registrationNumber PrintableString(SIZE(1..128)) OPTIONAL,
 *                 addProfessionInfo OCTET STRING OPTIONAL 
 *               }
 * </pre>
 * 
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.x509.AdmissionSyntax
 */
public class ProfessionInfo extends ASN1Encodable
{

    /**
     * Rechtsanw�ltin
     */
    public static final DERObjectIdentifier Rechtsanwltin = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".1");

    /**
     * Rechtsanwalt
     */
    public static final DERObjectIdentifier Rechtsanwalt = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".2");

    /**
     * Rechtsbeistand
     */
    public static final DERObjectIdentifier Rechtsbeistand = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".3");

    /**
     * Steuerberaterin
     */
    public static final DERObjectIdentifier Steuerberaterin = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".4");

    /**
     * Steuerberater
     */
    public static final DERObjectIdentifier Steuerberater = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".5");

    /**
     * Steuerbevollm�chtigte
     */
    public static final DERObjectIdentifier Steuerbevollmchtigte = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".6");

    /**
     * Steuerbevollm�chtigter
     */
    public static final DERObjectIdentifier Steuerbevollmchtigter = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".7");

    /**
     * Notarin
     */
    public static final DERObjectIdentifier Notarin = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".8");

    /**
     * Notar
     */
    public static final DERObjectIdentifier Notar = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".9");

    /**
     * Notarvertreterin
     */
    public static final DERObjectIdentifier Notarvertreterin = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".10");

    /**
     * Notarvertreter
     */
    public static final DERObjectIdentifier Notarvertreter = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".11");

    /**
     * Notariatsverwalterin
     */
    public static final DERObjectIdentifier Notariatsverwalterin = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".12");

    /**
     * Notariatsverwalter
     */
    public static final DERObjectIdentifier Notariatsverwalter = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".13");

    /**
     * Wirtschaftspr�ferin
     */
    public static final DERObjectIdentifier Wirtschaftsprferin = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".14");

    /**
     * Wirtschaftspr�fer
     */
    public static final DERObjectIdentifier Wirtschaftsprfer = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".15");

    /**
     * Vereidigte Buchpr�ferin
     */
    public static final DERObjectIdentifier VereidigteBuchprferin = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".16");

    /**
     * Vereidigter Buchpr�fer
     */
    public static final DERObjectIdentifier VereidigterBuchprfer = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".17");

    /**
     * Patentanw�ltin
     */
    public static final DERObjectIdentifier Patentanwltin = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".18");

    /**
     * Patentanwalt
     */
    public static final DERObjectIdentifier Patentanwalt = new DERObjectIdentifier(
        NamingAuthority.id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern + ".19");

    private NamingAuthority namingAuthority;

    private ASN1Sequence professionItems;

    private ASN1Sequence professionOIDs;

    private String registrationNumber;

    private ASN1OctetString addProfessionInfo;

    public static ProfessionInfo getInstance(Object obj)
    {
        if (obj == null || obj instanceof ProfessionInfo)
        {
            return (ProfessionInfo)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new ProfessionInfo((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }

    /**
     * Constructor from ASN1Sequence.
     * <p/>
     * <p/>
     * <pre>
     *               ProfessionInfo ::= SEQUENCE
     *               {
     *                 namingAuthority [0] EXPLICIT NamingAuthority OPTIONAL,
     *                 professionItems SEQUENCE OF DirectoryString (SIZE(1..128)),
     *                 professionOIDs SEQUENCE OF OBJECT IDENTIFIER OPTIONAL,
     *                 registrationNumber PrintableString(SIZE(1..128)) OPTIONAL,
     *                 addProfessionInfo OCTET STRING OPTIONAL
     *               }
     * </pre>
     *
     * @param seq The ASN.1 sequence.
     */
    private ProfessionInfo(ASN1Sequence seq)
    {
        if (seq.size() > 5)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }

        Enumeration e = seq.getObjects();

        DEREncodable o = (DEREncodable)e.nextElement();

        if (o instanceof ASN1TaggedObject)
        {
            if (((ASN1TaggedObject)o).getTagNo() != 0)
            {
                throw new IllegalArgumentException("Bad tag number: "
                    + ((ASN1TaggedObject)o).getTagNo());
            }
            namingAuthority = NamingAuthority.getInstance((ASN1TaggedObject)o, true);
            o = (DEREncodable)e.nextElement();
        }

        professionItems = ASN1Sequence.getInstance(o);

        if (e.hasMoreElements())
        {
            o = (DEREncodable)e.nextElement();
            if (o instanceof ASN1Sequence)
            {
                professionOIDs = ASN1Sequence.getInstance(o);
            }
            else if (o instanceof DERPrintableString)
            {
                registrationNumber = DERPrintableString.getInstance(o).getString();
            }
            else if (o instanceof ASN1OctetString)
            {
                addProfessionInfo = ASN1OctetString.getInstance(o);
            }
            else
            {
                throw new IllegalArgumentException("Bad object encountered: "
                    + o.getClass());
            }
        }
        if (e.hasMoreElements())
        {
            o = (DEREncodable)e.nextElement();
            if (o instanceof DERPrintableString)
            {
                registrationNumber = DERPrintableString.getInstance(o).getString();
            }
            else if (o instanceof DEROctetString)
            {
                addProfessionInfo = (DEROctetString)o;
            }
            else
            {
                throw new IllegalArgumentException("Bad object encountered: "
                    + o.getClass());
            }
        }
        if (e.hasMoreElements())
        {
            o = (DEREncodable)e.nextElement();
            if (o instanceof DEROctetString)
            {
                addProfessionInfo = (DEROctetString)o;
            }
            else
            {
                throw new IllegalArgumentException("Bad object encountered: "
                    + o.getClass());
            }
        }

    }

    /**
     * Constructor from given details.
     * <p/>
     * <code>professionItems</code> is mandatory, all other parameters are
     * optional.
     *
     * @param namingAuthority    The naming authority.
     * @param professionItems    Directory strings of the profession.
     * @param professionOIDs     DERObjectIdentfier objects for the
     *                           profession.
     * @param registrationNumber Registration number.
     * @param addProfessionInfo  Additional infos in encoded form.
     */
    public ProfessionInfo(NamingAuthority namingAuthority,
                          DirectoryString[] professionItems, DERObjectIdentifier[] professionOIDs,
                          String registrationNumber, ASN1OctetString addProfessionInfo)
    {
        this.namingAuthority = namingAuthority;
        ASN1EncodableVector v = new ASN1EncodableVector();
        for (int i = 0; i != professionItems.length; i++)
        {
            v.add(professionItems[i]);
        }
        this.professionItems = new DERSequence(v);
        if (professionOIDs != null)
        {
            v = new ASN1EncodableVector();
            for (int i = 0; i != professionOIDs.length; i++)
            {
                v.add(professionOIDs[i]);
            }
            this.professionOIDs = new DERSequence(v);
        }
        this.registrationNumber = registrationNumber;
        this.addProfessionInfo = addProfessionInfo;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *               ProfessionInfo ::= SEQUENCE
     *               {
     *                 namingAuthority [0] EXPLICIT NamingAuthority OPTIONAL,
     *                 professionItems SEQUENCE OF DirectoryString (SIZE(1..128)),
     *                 professionOIDs SEQUENCE OF OBJECT IDENTIFIER OPTIONAL,
     *                 registrationNumber PrintableString(SIZE(1..128)) OPTIONAL,
     *                 addProfessionInfo OCTET STRING OPTIONAL
     *               }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        if (namingAuthority != null)
        {
            vec.add(new DERTaggedObject(true, 0, namingAuthority));
        }
        vec.add(professionItems);
        if (professionOIDs != null)
        {
            vec.add(professionOIDs);
        }
        if (registrationNumber != null)
        {
            vec.add(new DERPrintableString(registrationNumber, true));
        }
        if (addProfessionInfo != null)
        {
            vec.add(addProfessionInfo);
        }
        return new DERSequence(vec);
    }

    /**
     * @return Returns the addProfessionInfo.
     */
    public ASN1OctetString getAddProfessionInfo()
    {
        return addProfessionInfo;
    }

    /**
     * @return Returns the namingAuthority.
     */
    public NamingAuthority getNamingAuthority()
    {
        return namingAuthority;
    }

    /**
     * @return Returns the professionItems.
     */
    public DirectoryString[] getProfessionItems()
    {
        DirectoryString[] items = new DirectoryString[professionItems.size()];
        int count = 0;
        for (Enumeration e = professionItems.getObjects(); e.hasMoreElements();)
        {
            items[count++] = DirectoryString.getInstance(e.nextElement());
        }
        return items;
    }

    /**
     * @return Returns the professionOIDs.
     */
    public DERObjectIdentifier[] getProfessionOIDs()
    {
        if (professionOIDs == null)
        {
            return new DERObjectIdentifier[0];
        }
        DERObjectIdentifier[] oids = new DERObjectIdentifier[professionOIDs.size()];
        int count = 0;
        for (Enumeration e = professionOIDs.getObjects(); e.hasMoreElements();)
        {
            oids[count++] = DERObjectIdentifier.getInstance(e.nextElement());
        }
        return oids;
    }

    /**
     * @return Returns the registrationNumber.
     */
    public String getRegistrationNumber()
    {
        return registrationNumber;
    }
}
