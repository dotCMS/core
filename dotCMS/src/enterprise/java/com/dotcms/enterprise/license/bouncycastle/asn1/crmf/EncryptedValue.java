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

package com.dotcms.enterprise.license.bouncycastle.asn1.crmf;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERBitString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class EncryptedValue
    extends ASN1Encodable
{
    private AlgorithmIdentifier intendedAlg;
    private AlgorithmIdentifier symmAlg;
    private DERBitString        encSymmKey;
    private AlgorithmIdentifier keyAlg;
    private ASN1OctetString     valueHint;
    private DERBitString        encValue;

    private EncryptedValue(ASN1Sequence seq)
    {
        int index = 0;
        while (seq.getObjectAt(index) instanceof ASN1TaggedObject)
        {
            ASN1TaggedObject tObj = (ASN1TaggedObject)seq.getObjectAt(index);

            switch (tObj.getTagNo())
            {
            case 0:
                intendedAlg = AlgorithmIdentifier.getInstance(tObj, false);
                break;
            case 1:
                symmAlg = AlgorithmIdentifier.getInstance(tObj, false);
                break;
            case 2:
                encSymmKey = DERBitString.getInstance(tObj, false);
                break;
            case 3:
                keyAlg = AlgorithmIdentifier.getInstance(tObj, false);
                break;
            case 4:
                valueHint = ASN1OctetString.getInstance(tObj, false);
                break;
            }
            index++;
        }

        encValue = DERBitString.getInstance(seq.getObjectAt(index));
    }

    public static EncryptedValue getInstance(Object o)
    {
        if (o instanceof EncryptedValue)
        {
            return (EncryptedValue)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new EncryptedValue((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    /**
     * <pre>
     * EncryptedValue ::= SEQUENCE {
     *                     intendedAlg   [0] AlgorithmIdentifier  OPTIONAL,
     *                     -- the intended algorithm for which the value will be used
     *                     symmAlg       [1] AlgorithmIdentifier  OPTIONAL,
     *                     -- the symmetric algorithm used to encrypt the value
     *                     encSymmKey    [2] BIT STRING           OPTIONAL,
     *                     -- the (encrypted) symmetric key used to encrypt the value
     *                     keyAlg        [3] AlgorithmIdentifier  OPTIONAL,
     *                     -- algorithm used to encrypt the symmetric key
     *                     valueHint     [4] OCTET STRING         OPTIONAL,
     *                     -- a brief description or identifier of the encValue content
     *                     -- (may be meaningful only to the sending entity, and used only
     *                     -- if EncryptedValue might be re-examined by the sending entity
     *                     -- in the future)
     *                     encValue       BIT STRING }
     *                     -- the encrypted value itself
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        addOptional(v, 0, intendedAlg);
        addOptional(v, 1, symmAlg);
        addOptional(v, 2, encSymmKey);
        addOptional(v, 3, keyAlg);
        addOptional(v, 4, valueHint);

        v.add(encValue);

        return new DERSequence(v);
    }

    private void addOptional(ASN1EncodableVector v, int tagNo, ASN1Encodable obj)
    {
        if (obj != null)
        {
            v.add(new DERTaggedObject(false, tagNo, obj));
        }
    }
}
