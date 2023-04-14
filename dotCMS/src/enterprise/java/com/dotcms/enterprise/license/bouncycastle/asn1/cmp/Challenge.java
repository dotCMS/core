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

package com.dotcms.enterprise.license.bouncycastle.asn1.cmp;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class Challenge
    extends ASN1Encodable
{
    private AlgorithmIdentifier owf;
    private ASN1OctetString witness;
    private ASN1OctetString challenge;

    private Challenge(ASN1Sequence seq)
    {
        int index = 0;

        if (seq.size() == 3)
        {
            owf = AlgorithmIdentifier.getInstance(seq.getObjectAt(index++));
        }

        witness = ASN1OctetString.getInstance(seq.getObjectAt(index++));
        challenge = ASN1OctetString.getInstance(seq.getObjectAt(index));
    }

    public static Challenge getInstance(Object o)
    {
        if (o instanceof Challenge)
        {
            return (Challenge)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new Challenge((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public AlgorithmIdentifier getOwf()
    {
        return owf;
    }

    /**
     * <pre>
     * Challenge ::= SEQUENCE {
     *                 owf                 AlgorithmIdentifier  OPTIONAL,
     *
     *                 -- MUST be present in the first Challenge; MAY be omitted in
     *                 -- any subsequent Challenge in POPODecKeyChallContent (if
     *                 -- omitted, then the owf used in the immediately preceding
     *                 -- Challenge is to be used).
     *
     *                 witness             OCTET STRING,
     *                 -- the result of applying the one-way function (owf) to a
     *                 -- randomly-generated INTEGER, A.  [Note that a different
     *                 -- INTEGER MUST be used for each Challenge.]
     *                 challenge           OCTET STRING
     *                 -- the encryption (under the public key for which the cert.
     *                 -- request is being made) of Rand, where Rand is specified as
     *                 --   Rand ::= SEQUENCE {
     *                 --      int      INTEGER,
     *                 --       - the randomly-generated INTEGER A (above)
     *                 --      sender   GeneralName
     *                 --       - the sender's name (as included in PKIHeader)
     *                 --   }
     *      }
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        addOptional(v, owf);
        v.add(witness);
        v.add(challenge);

        return new DERSequence(v);
    }

    private void addOptional(ASN1EncodableVector v, ASN1Encodable obj)
    {
        if (obj != null)
        {
            v.add(obj);
        }
    }
}
