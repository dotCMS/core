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
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERGeneralizedTime;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.GeneralName;

import java.util.Enumeration;

public class PKIHeader
    extends ASN1Encodable
{
    private DERInteger pvno;
    private GeneralName sender;
    private GeneralName recipient;
    private DERGeneralizedTime messageTime;
    private AlgorithmIdentifier protectionAlg;
    private ASN1OctetString senderKID;       // KeyIdentifier
    private ASN1OctetString recipKID;        // KeyIdentifier
    private ASN1OctetString transactionID;
    private ASN1OctetString senderNonce;
    private ASN1OctetString recipNonce;
    private PKIFreeText     freeText;
    private ASN1Sequence    generalInfo;

    private PKIHeader(ASN1Sequence seq)
    {
        Enumeration en = seq.getObjects();

        pvno = DERInteger.getInstance(en.nextElement());
        sender = GeneralName.getInstance(en.nextElement());
        recipient = GeneralName.getInstance(en.nextElement());

        while (en.hasMoreElements())
        {
            ASN1TaggedObject tObj = (ASN1TaggedObject)en.nextElement();

            switch (tObj.getTagNo())
            {
            case 0:
                messageTime = DERGeneralizedTime.getInstance(tObj, true);
                break;
            case 1:
                protectionAlg = AlgorithmIdentifier.getInstance(tObj, true);
                break;
            case 2:
                senderKID = ASN1OctetString.getInstance(tObj, true);
                break;
            case 3:
                recipKID = ASN1OctetString.getInstance(tObj, true);
                break;
            case 4:
                transactionID = ASN1OctetString.getInstance(tObj, true);
                break;
            case 5:
                senderNonce = ASN1OctetString.getInstance(tObj, true);
                break;
            case 6:
                recipNonce = ASN1OctetString.getInstance(tObj, true);
                break;
            case 7:
                freeText = PKIFreeText.getInstance(tObj, true);
                break;
            case 8:
                generalInfo = ASN1Sequence.getInstance(tObj, true);
                break;
            default:
                throw new IllegalArgumentException("unknown tag number: " + tObj.getTagNo());
            }
        }
    }

    public static PKIHeader getInstance(Object o)
    {
        if (o instanceof PKIHeader)
        {
            return (PKIHeader)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new PKIHeader((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public DERInteger getPvno()
    {
        return pvno;
    }

    public GeneralName getSender()
    {
        return sender;
    }

    public GeneralName getRecipient()
    {
        return recipient;
    }

    /**
     * <pre>
     *  PKIHeader ::= SEQUENCE {
     *            pvno                INTEGER     { cmp1999(1), cmp2000(2) },
     *            sender              GeneralName,
     *            -- identifies the sender
     *            recipient           GeneralName,
     *            -- identifies the intended recipient
     *            messageTime     [0] GeneralizedTime         OPTIONAL,
     *            -- time of production of this message (used when sender
     *            -- believes that the transport will be "suitable"; i.e.,
     *            -- that the time will still be meaningful upon receipt)
     *            protectionAlg   [1] AlgorithmIdentifier     OPTIONAL,
     *            -- algorithm used for calculation of protection bits
     *            senderKID       [2] KeyIdentifier           OPTIONAL,
     *            recipKID        [3] KeyIdentifier           OPTIONAL,
     *            -- to identify specific keys used for protection
     *            transactionID   [4] OCTET STRING            OPTIONAL,
     *            -- identifies the transaction; i.e., this will be the same in
     *            -- corresponding request, response, certConf, and PKIConf
     *            -- messages
     *            senderNonce     [5] OCTET STRING            OPTIONAL,
     *            recipNonce      [6] OCTET STRING            OPTIONAL,
     *            -- nonces used to provide replay protection, senderNonce
     *            -- is inserted by the creator of this message; recipNonce
     *            -- is a nonce previously inserted in a related message by
     *            -- the intended recipient of this message
     *            freeText        [7] PKIFreeText             OPTIONAL,
     *            -- this may be used to indicate context-specific instructions
     *            -- (this field is intended for human consumption)
     *            generalInfo     [8] SEQUENCE SIZE (1..MAX) OF
     *                                 InfoTypeAndValue     OPTIONAL
     *            -- this may be used to convey context-specific information
     *            -- (this field not primarily intended for human consumption)
     * }
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(pvno);
        v.add(sender);
        v.add(recipient);
        addOptional(v, 0, messageTime);
        addOptional(v, 1, protectionAlg);
        addOptional(v, 2, senderKID);
        addOptional(v, 3, recipKID);
        addOptional(v, 4, transactionID);
        addOptional(v, 5, senderNonce);
        addOptional(v, 6, recipNonce);
        addOptional(v, 7, freeText);
        addOptional(v, 8, generalInfo);

        return new DERSequence(v);
    }

    private void addOptional(ASN1EncodableVector v, int tagNo, ASN1Encodable obj)
    {
        if (obj != null)
        {
            v.add(new DERTaggedObject(true, tagNo, obj));
        }
    }
}
