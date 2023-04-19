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
import com.dotcms.enterprise.license.bouncycastle.asn1.DERIA5String;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERString;
import com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.ISISMTTObjectIdentifiers;
import com.dotcms.enterprise.license.bouncycastle.asn1.x500.DirectoryString;

import java.util.Enumeration;

/**
 * Names of authorities which are responsible for the administration of title
 * registers.
 * 
 * <pre>
 *             NamingAuthority ::= SEQUENCE 
 *             {
 *               namingAuthorityId OBJECT IDENTIFIER OPTIONAL,
 *               namingAuthorityUrl IA5String OPTIONAL,
 *               namingAuthorityText DirectoryString(SIZE(1..128)) OPTIONAL
 *             }
 * </pre>
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.x509.AdmissionSyntax
 * 
 */
public class NamingAuthority
    extends ASN1Encodable
{

    /**
     * Profession OIDs should always be defined under the OID branch of the
     * responsible naming authority. At the time of this writing, the work group
     * �Recht, Wirtschaft, Steuern� (�Law, Economy, Taxes�) is registered as the
     * first naming authority under the OID id-isismtt-at-namingAuthorities.
     */
    public static final DERObjectIdentifier id_isismtt_at_namingAuthorities_RechtWirtschaftSteuern =
        new DERObjectIdentifier(ISISMTTObjectIdentifiers.id_isismtt_at_namingAuthorities + ".1");

    private DERObjectIdentifier namingAuthorityId;
    private String namingAuthorityUrl;
    private DirectoryString namingAuthorityText;

    public static NamingAuthority getInstance(Object obj)
    {
        if (obj == null || obj instanceof NamingAuthority)
        {
            return (NamingAuthority)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new NamingAuthority((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }

    public static NamingAuthority getInstance(ASN1TaggedObject obj, boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    /**
     * Constructor from ASN1Sequence.
     * <p/>
     * <p/>
     * <pre>
     *             NamingAuthority ::= SEQUENCE
     *             {
     *               namingAuthorityId OBJECT IDENTIFIER OPTIONAL,
     *               namingAuthorityUrl IA5String OPTIONAL,
     *               namingAuthorityText DirectoryString(SIZE(1..128)) OPTIONAL
     *             }
     * </pre>
     *
     * @param seq The ASN.1 sequence.
     */
    private NamingAuthority(ASN1Sequence seq)
    {

        if (seq.size() > 3)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }

        Enumeration e = seq.getObjects();

        if (e.hasMoreElements())
        {
            DEREncodable o = (DEREncodable)e.nextElement();
            if (o instanceof DERObjectIdentifier)
            {
                namingAuthorityId = (DERObjectIdentifier)o;
            }
            else if (o instanceof DERIA5String)
            {
                namingAuthorityUrl = DERIA5String.getInstance(o).getString();
            }
            else if (o instanceof DERString)
            {
                namingAuthorityText = DirectoryString.getInstance(o);
            }
            else
            {
                throw new IllegalArgumentException("Bad object encountered: "
                    + o.getClass());
            }
        }
        if (e.hasMoreElements())
        {
            DEREncodable o = (DEREncodable)e.nextElement();
            if (o instanceof DERIA5String)
            {
                namingAuthorityUrl = DERIA5String.getInstance(o).getString();
            }
            else if (o instanceof DERString)
            {
                namingAuthorityText = DirectoryString.getInstance(o);
            }
            else
            {
                throw new IllegalArgumentException("Bad object encountered: "
                    + o.getClass());
            }
        }
        if (e.hasMoreElements())
        {
            DEREncodable o = (DEREncodable)e.nextElement();
            if (o instanceof DERString)
            {
                namingAuthorityText = DirectoryString.getInstance(o);
            }
            else
            {
                throw new IllegalArgumentException("Bad object encountered: "
                    + o.getClass());
            }

        }
    }

    /**
     * @return Returns the namingAuthorityId.
     */
    public DERObjectIdentifier getNamingAuthorityId()
    {
        return namingAuthorityId;
    }

    /**
     * @return Returns the namingAuthorityText.
     */
    public DirectoryString getNamingAuthorityText()
    {
        return namingAuthorityText;
    }

    /**
     * @return Returns the namingAuthorityUrl.
     */
    public String getNamingAuthorityUrl()
    {
        return namingAuthorityUrl;
    }

    /**
     * Constructor from given details.
     * <p/>
     * All parameters can be combined.
     *
     * @param namingAuthorityId   ObjectIdentifier for naming authority.
     * @param namingAuthorityUrl  URL for naming authority.
     * @param namingAuthorityText Textual representation of naming authority.
     */
    public NamingAuthority(DERObjectIdentifier namingAuthorityId,
                           String namingAuthorityUrl, DirectoryString namingAuthorityText)
    {
        this.namingAuthorityId = namingAuthorityId;
        this.namingAuthorityUrl = namingAuthorityUrl;
        this.namingAuthorityText = namingAuthorityText;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *             NamingAuthority ::= SEQUENCE
     *             {
     *               namingAuthorityId OBJECT IDENTIFIER OPTIONAL,
     *               namingAuthorityUrl IA5String OPTIONAL,
     *               namingAuthorityText DirectoryString(SIZE(1..128)) OPTIONAL
     *             }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        if (namingAuthorityId != null)
        {
            vec.add(namingAuthorityId);
        }
        if (namingAuthorityUrl != null)
        {
            vec.add(new DERIA5String(namingAuthorityUrl, true));
        }
        if (namingAuthorityText != null)
        {
            vec.add(namingAuthorityText);
        }
        return new DERSequence(vec);
    }
}
