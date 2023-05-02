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

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Choice;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERString;
import com.dotcms.enterprise.license.bouncycastle.asn1.x500.DirectoryString;

import java.util.Enumeration;

/**
 * Structure for a name or pseudonym.
 * 
 * <pre>
 *       NameOrPseudonym ::= CHOICE {
 *            surAndGivenName SEQUENCE {
 *              surName DirectoryString,
 *              givenName SEQUENCE OF DirectoryString 
 *         },
 *            pseudonym DirectoryString 
 *       }
 * </pre>
 * 
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.x509.sigi.PersonalData
 * 
 */
public class NameOrPseudonym
    extends ASN1Encodable
    implements ASN1Choice
{
    private DirectoryString pseudonym;

    private DirectoryString surname;

    private ASN1Sequence givenName;

    public static NameOrPseudonym getInstance(Object obj)
    {
        if (obj == null || obj instanceof NameOrPseudonym)
        {
            return (NameOrPseudonym)obj;
        }

        if (obj instanceof DERString)
        {
            return new NameOrPseudonym(DirectoryString.getInstance(obj));
        }

        if (obj instanceof ASN1Sequence)
        {
            return new NameOrPseudonym((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }

    /**
     * Constructor from DERString.
     * <p/>
     * The sequence is of type NameOrPseudonym:
     * <p/>
     * <pre>
     *       NameOrPseudonym ::= CHOICE {
     *            surAndGivenName SEQUENCE {
     *              surName DirectoryString,
     *              givenName SEQUENCE OF DirectoryString
     *         },
     *            pseudonym DirectoryString
     *       }
     * </pre>
     * @param pseudonym pseudonym value to use.
     */
    public NameOrPseudonym(DirectoryString pseudonym)
    {
        this.pseudonym = pseudonym;
    }

    /**
     * Constructor from ASN1Sequence.
     * <p/>
     * The sequence is of type NameOrPseudonym:
     * <p/>
     * <pre>
     *       NameOrPseudonym ::= CHOICE {
     *            surAndGivenName SEQUENCE {
     *              surName DirectoryString,
     *              givenName SEQUENCE OF DirectoryString
     *         },
     *            pseudonym DirectoryString
     *       }
     * </pre>
     *
     * @param seq The ASN.1 sequence.
     */
    private NameOrPseudonym(ASN1Sequence seq)
    {
        if (seq.size() != 2)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }

        if (!(seq.getObjectAt(0) instanceof DERString))
        {
            throw new IllegalArgumentException("Bad object encountered: "
                + seq.getObjectAt(0).getClass());
        }

        surname = DirectoryString.getInstance(seq.getObjectAt(0));
        givenName = ASN1Sequence.getInstance(seq.getObjectAt(1));
    }

    /**
     * Constructor from a given details.
     *
     * @param pseudonym The pseudonym.
     */
    public NameOrPseudonym(String pseudonym)
    {
        this(new DirectoryString(pseudonym));
    }

    /**
     * Constructor from a given details.
     *
     * @param surname   The surname.
     * @param givenName A sequence of directory strings making up the givenName
     */
    public NameOrPseudonym(DirectoryString surname, ASN1Sequence givenName)
    {
        this.surname = surname;
        this.givenName = givenName;
    }

    public DirectoryString getPseudonym()
    {
        return pseudonym;
    }

    public DirectoryString getSurname()
    {
        return surname;
    }

    public DirectoryString[] getGivenName()
    {
        DirectoryString[] items = new DirectoryString[givenName.size()];
        int count = 0;
        for (Enumeration e = givenName.getObjects(); e.hasMoreElements();)
        {
            items[count++] = DirectoryString.getInstance(e.nextElement());
        }
        return items;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *       NameOrPseudonym ::= CHOICE {
     *            surAndGivenName SEQUENCE {
     *              surName DirectoryString,
     *              givenName SEQUENCE OF DirectoryString
     *         },
     *            pseudonym DirectoryString
     *       }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        if (pseudonym != null)
        {
            return pseudonym.toASN1Object();
        }
        else
        {
            ASN1EncodableVector vec1 = new ASN1EncodableVector();
            vec1.add(surname);
            vec1.add(givenName);
            return new DERSequence(vec1);
        }
    }
}
