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
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.GeneralName;

import java.util.Enumeration;

/**
 * An Admissions structure.
 * <p/>
 * <pre>
 *            Admissions ::= SEQUENCE
 *            {
 *              admissionAuthority [0] EXPLICIT GeneralName OPTIONAL
 *              namingAuthority [1] EXPLICIT NamingAuthority OPTIONAL
 *              professionInfos SEQUENCE OF ProfessionInfo
 *            }
 * <p/>
 * </pre>
 *
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.x509.AdmissionSyntax
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.x509.ProfessionInfo
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.x509.NamingAuthority
 */
public class Admissions extends ASN1Encodable
{

    private GeneralName admissionAuthority;

    private NamingAuthority namingAuthority;

    private ASN1Sequence professionInfos;

    public static Admissions getInstance(Object obj)
    {
        if (obj == null || obj instanceof Admissions)
        {
            return (Admissions)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new Admissions((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * Constructor from ASN1Sequence.
     * <p/>
     * The sequence is of type ProcurationSyntax:
     * <p/>
     * <pre>
     *            Admissions ::= SEQUENCE
     *            {
     *              admissionAuthority [0] EXPLICIT GeneralName OPTIONAL
     *              namingAuthority [1] EXPLICIT NamingAuthority OPTIONAL
     *              professionInfos SEQUENCE OF ProfessionInfo
     *            }
     * </pre>
     *
     * @param seq The ASN.1 sequence.
     */
    private Admissions(ASN1Sequence seq)
    {
        if (seq.size() > 3)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }
        Enumeration e = seq.getObjects();

        DEREncodable o = (DEREncodable)e.nextElement();
        if (o instanceof ASN1TaggedObject)
        {
            switch (((ASN1TaggedObject)o).getTagNo())
            {
            case 0:
                admissionAuthority = GeneralName.getInstance((ASN1TaggedObject)o, true);
                break;
            case 1:
                namingAuthority = NamingAuthority.getInstance((ASN1TaggedObject)o, true);
                break;
            default:
                throw new IllegalArgumentException("Bad tag number: " + ((ASN1TaggedObject)o).getTagNo());
            }
            o = (DEREncodable)e.nextElement();
        }
        if (o instanceof ASN1TaggedObject)
        {
            switch (((ASN1TaggedObject)o).getTagNo())
            {
            case 1:
                namingAuthority = NamingAuthority.getInstance((ASN1TaggedObject)o, true);
                break;
            default:
                throw new IllegalArgumentException("Bad tag number: " + ((ASN1TaggedObject)o).getTagNo());
            }
            o = (DEREncodable)e.nextElement();
        }
        professionInfos = ASN1Sequence.getInstance(o);
        if (e.hasMoreElements())
        {
            throw new IllegalArgumentException("Bad object encountered: "
                + e.nextElement().getClass());
        }
    }

    /**
     * Constructor from a given details.
     * <p/>
     * Parameter <code>professionInfos</code> is mandatory.
     *
     * @param admissionAuthority The admission authority.
     * @param namingAuthority    The naming authority.
     * @param professionInfos    The profession infos.
     */
    public Admissions(GeneralName admissionAuthority,
                      NamingAuthority namingAuthority, ProfessionInfo[] professionInfos)
    {
        this.admissionAuthority = admissionAuthority;
        this.namingAuthority = namingAuthority;
        this.professionInfos = new DERSequence(professionInfos);
    }

    public GeneralName getAdmissionAuthority()
    {
        return admissionAuthority;
    }

    public NamingAuthority getNamingAuthority()
    {
        return namingAuthority;
    }

    public ProfessionInfo[] getProfessionInfos()
    {
        ProfessionInfo[] infos = new ProfessionInfo[professionInfos.size()];
        int count = 0;
        for (Enumeration e = professionInfos.getObjects(); e.hasMoreElements();)
        {
            infos[count++] = ProfessionInfo.getInstance(e.nextElement());
        }
        return infos;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *       Admissions ::= SEQUENCE
     *       {
     *         admissionAuthority [0] EXPLICIT GeneralName OPTIONAL
     *         namingAuthority [1] EXPLICIT NamingAuthority OPTIONAL
     *         professionInfos SEQUENCE OF ProfessionInfo
     *       }
     * <p/>
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        
        if (admissionAuthority != null)
        {
            vec.add(new DERTaggedObject(true, 0, admissionAuthority));
        }
        if (namingAuthority != null)
        {
            vec.add(new DERTaggedObject(true, 1, namingAuthority));
        }
        vec.add(professionInfos);

        return new DERSequence(vec);
    }
}
