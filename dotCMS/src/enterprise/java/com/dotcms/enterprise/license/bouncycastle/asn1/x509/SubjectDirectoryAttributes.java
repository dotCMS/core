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

import java.util.Enumeration;
import java.util.Vector;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

/**
 * This extension may contain further X.500 attributes of the subject. See also
 * RFC 3039.
 * 
 * <pre>
 *     SubjectDirectoryAttributes ::= Attributes
 *     Attributes ::= SEQUENCE SIZE (1..MAX) OF Attribute
 *     Attribute ::= SEQUENCE 
 *     {
 *       type AttributeType 
 *       values SET OF AttributeValue 
 *     }
 *     
 *     AttributeType ::= OBJECT IDENTIFIER
 *     AttributeValue ::= ANY DEFINED BY AttributeType
 * </pre>
 * 
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.x509.X509Name for AttributeType ObjectIdentifiers.
 */
public class SubjectDirectoryAttributes 
    extends ASN1Encodable
{
    private Vector attributes = new Vector();

    public static SubjectDirectoryAttributes getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof SubjectDirectoryAttributes)
        {
            return (SubjectDirectoryAttributes)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new SubjectDirectoryAttributes((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * Constructor from ASN1Sequence.
     * 
     * The sequence is of type SubjectDirectoryAttributes:
     * 
     * <pre>
     *      SubjectDirectoryAttributes ::= Attributes
     *      Attributes ::= SEQUENCE SIZE (1..MAX) OF Attribute
     *      Attribute ::= SEQUENCE 
     *      {
     *        type AttributeType 
     *        values SET OF AttributeValue 
     *      }
     *      
     *      AttributeType ::= OBJECT IDENTIFIER
     *      AttributeValue ::= ANY DEFINED BY AttributeType
     * </pre>
     * 
     * @param seq
     *            The ASN.1 sequence.
     */
    public SubjectDirectoryAttributes(ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();

        while (e.hasMoreElements())
        {
            ASN1Sequence s = ASN1Sequence.getInstance(e.nextElement());
            attributes.addElement(new Attribute(s));
        }
    }

    /**
     * Constructor from a vector of attributes.
     * 
     * The vector consists of attributes of type {@link Attribute Attribute}
     * 
     * @param attributes
     *            The attributes.
     * 
     */
    public SubjectDirectoryAttributes(Vector attributes)
    {
        Enumeration e = attributes.elements();

        while (e.hasMoreElements())
        {
            this.attributes.addElement(e.nextElement());
        }
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * 
     * Returns:
     * 
     * <pre>
     *      SubjectDirectoryAttributes ::= Attributes
     *      Attributes ::= SEQUENCE SIZE (1..MAX) OF Attribute
     *      Attribute ::= SEQUENCE 
     *      {
     *        type AttributeType 
     *        values SET OF AttributeValue 
     *      }
     *      
     *      AttributeType ::= OBJECT IDENTIFIER
     *      AttributeValue ::= ANY DEFINED BY AttributeType
     * </pre>
     * 
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        Enumeration e = attributes.elements();

        while (e.hasMoreElements())
        {

            vec.add((Attribute)e.nextElement());
        }

        return new DERSequence(vec);
    }

    /**
     * @return Returns the attributes.
     */
    public Vector getAttributes()
    {
        return attributes;
    }
}
