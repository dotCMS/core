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
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;

import java.util.Enumeration;

/**
 * This class helps to support crossCerfificatePairs in a LDAP directory
 * according RFC 2587
 * 
 * <pre>
 *     crossCertificatePairATTRIBUTE::={
 *       WITH SYNTAX   CertificatePair
 *       EQUALITY MATCHING RULE certificatePairExactMatch
 *       ID joint-iso-ccitt(2) ds(5) attributeType(4) crossCertificatePair(40)}
 * </pre>
 * 
 * <blockquote> The forward elements of the crossCertificatePair attribute of a
 * CA's directory entry shall be used to store all, except self-issued
 * certificates issued to this CA. Optionally, the reverse elements of the
 * crossCertificatePair attribute, of a CA's directory entry may contain a
 * subset of certificates issued by this CA to other CAs. When both the forward
 * and the reverse elements are present in a single attribute value, issuer name
 * in one certificate shall match the subject name in the other and vice versa,
 * and the subject public key in one certificate shall be capable of verifying
 * the digital signature on the other certificate and vice versa.
 * 
 * When a reverse element is present, the forward element value and the reverse
 * element value need not be stored in the same attribute value; in other words,
 * they can be stored in either a single attribute value or two attribute
 * values. </blockquote>
 * 
 * <pre>
 *       CertificatePair ::= SEQUENCE {
 *         forward        [0]    Certificate OPTIONAL,
 *         reverse        [1]    Certificate OPTIONAL,
 *         -- at least one of the pair shall be present -- } 
 * </pre>
 */
public class CertificatePair
    extends ASN1Encodable
{
    private X509CertificateStructure forward;

    private X509CertificateStructure reverse;

    public static CertificatePair getInstance(Object obj)
    {
        if (obj == null || obj instanceof CertificatePair)
        {
            return (CertificatePair)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new CertificatePair((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }

    /**
     * Constructor from ASN1Sequence.
     * <p/>
     * The sequence is of type CertificatePair:
     * <p/>
     * <pre>
     *       CertificatePair ::= SEQUENCE {
     *         forward        [0]    Certificate OPTIONAL,
     *         reverse        [1]    Certificate OPTIONAL,
     *         -- at least one of the pair shall be present -- }
     * </pre>
     *
     * @param seq The ASN.1 sequence.
     */
    private CertificatePair(ASN1Sequence seq)
    {
        if (seq.size() != 1 && seq.size() != 2)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }

        Enumeration e = seq.getObjects();

        while (e.hasMoreElements())
        {
            ASN1TaggedObject o = ASN1TaggedObject.getInstance(e.nextElement());
            if (o.getTagNo() == 0)
            {
                forward = X509CertificateStructure.getInstance(o, true);
            }
            else if (o.getTagNo() == 1)
            {
                reverse = X509CertificateStructure.getInstance(o, true);
            }
            else
            {
                throw new IllegalArgumentException("Bad tag number: "
                    + o.getTagNo());
            }
        }
    }

    /**
     * Constructor from a given details.
     *
     * @param forward Certificates issued to this CA.
     * @param reverse Certificates issued by this CA to other CAs.
     */
    public CertificatePair(X509CertificateStructure forward, X509CertificateStructure reverse)
    {
        this.forward = forward;
        this.reverse = reverse;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *       CertificatePair ::= SEQUENCE {
     *         forward        [0]    Certificate OPTIONAL,
     *         reverse        [1]    Certificate OPTIONAL,
     *         -- at least one of the pair shall be present -- }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();

        if (forward != null)
        {
            vec.add(new DERTaggedObject(0, forward));
        }
        if (reverse != null)
        {
            vec.add(new DERTaggedObject(1, reverse));
        }

        return new DERSequence(vec);
    }

    /**
     * @return Returns the forward.
     */
    public X509CertificateStructure getForward()
    {
        return forward;
    }

    /**
     * @return Returns the reverse.
     */
    public X509CertificateStructure getReverse()
    {
        return reverse;
    }
}
