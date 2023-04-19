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

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Choice;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.crmf.CertReqMessages;
import com.dotcms.enterprise.license.bouncycastle.asn1.pkcs.CertificationRequest;

public class PKIBody
    extends ASN1Encodable
    implements ASN1Choice
{
    private int tagNo;
    private ASN1Encodable body;

    public static PKIBody getInstance(Object o)
    {
        if (o instanceof PKIBody)
        {
            return (PKIBody)o;
        }

        if (o instanceof ASN1TaggedObject)
        {
            return new PKIBody((ASN1TaggedObject)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    private PKIBody(ASN1TaggedObject tagged)
    {
        tagNo = tagged.getTagNo();

        switch (tagged.getTagNo())
        {
        case 0:
            body = CertReqMessages.getInstance(tagged.getObject());
            break;
        case 1:
            body = CertRepMessage.getInstance(tagged.getObject());
            break;
        case 2:
            body = CertReqMessages.getInstance(tagged.getObject());
            break;
        case 3:
            body = CertRepMessage.getInstance(tagged.getObject());
            break;
        case 4:
            body = CertificationRequest.getInstance(tagged.getObject());
            break;
        case 5:
            body = POPODecKeyChallContent.getInstance(tagged.getObject());
            break;
        case 6:
            body = POPODecKeyRespContent.getInstance(tagged.getObject());
            break;
        case 7:
            body = CertReqMessages.getInstance(tagged.getObject());
            break;
        case 8:
            body = CertRepMessage.getInstance(tagged.getObject());
            break;
        case 9:
            body = CertReqMessages.getInstance(tagged.getObject());
            break;
        case 10:
            body = KeyRecRepContent.getInstance(tagged.getObject());
            break;
        case 11:
            body = RevReqContent.getInstance(tagged.getObject());
            break;
        case 12:
            body = RevRepContent.getInstance(tagged.getObject());
            break;
        case 13:
            body = CertReqMessages.getInstance(tagged.getObject());
            break;
        case 14:
            body = CertRepMessage.getInstance(tagged.getObject());
            break;
        case 15:
            body = CAKeyUpdAnnContent.getInstance(tagged.getObject());
            break;
        case 16:
            body = CMPCertificate.getInstance(tagged.getObject());  // CertAnnContent
            break;
        case 17:
            body = RevAnnContent.getInstance(tagged.getObject());
            break;
        case 18:
            body = CRLAnnContent.getInstance(tagged.getObject());
            break;
        case 19:
            body = PKIConfirmContent.getInstance(tagged.getObject());
            break;
        case 20:
            body = PKIMessages.getInstance(tagged.getObject()); // NestedMessageContent
            break;
        case 21:
            body = GenMsgContent.getInstance(tagged.getObject());
            break;
        case 22:
            body = GenRepContent.getInstance(tagged.getObject());
            break;
        case 23:
            body = ErrorMsgContent.getInstance(tagged.getObject());
            break;
        case 24:
            body = CertConfirmContent.getInstance(tagged.getObject());
            break;
        case 25:
            body = PollReqContent.getInstance(tagged.getObject());
            break;
        case 26:
            body = PollRepContent.getInstance(tagged.getObject());
            break;
        default:
            throw new IllegalArgumentException("unknown tag number: " + tagged.getTagNo());
        }
    }

    /**
     * <pre>
     * PKIBody ::= CHOICE {       -- message-specific body elements
     *        ir       [0]  CertReqMessages,        --Initialization Request
     *        ip       [1]  CertRepMessage,         --Initialization Response
     *        cr       [2]  CertReqMessages,        --Certification Request
     *        cp       [3]  CertRepMessage,         --Certification Response
     *        p10cr    [4]  CertificationRequest,   --imported from [PKCS10]
     *        popdecc  [5]  POPODecKeyChallContent, --pop Challenge
     *        popdecr  [6]  POPODecKeyRespContent,  --pop Response
     *        kur      [7]  CertReqMessages,        --Key Update Request
     *        kup      [8]  CertRepMessage,         --Key Update Response
     *        krr      [9]  CertReqMessages,        --Key Recovery Request
     *        krp      [10] KeyRecRepContent,       --Key Recovery Response
     *        rr       [11] RevReqContent,          --Revocation Request
     *        rp       [12] RevRepContent,          --Revocation Response
     *        ccr      [13] CertReqMessages,        --Cross-Cert. Request
     *        ccp      [14] CertRepMessage,         --Cross-Cert. Response
     *        ckuann   [15] CAKeyUpdAnnContent,     --CA Key Update Ann.
     *        cann     [16] CertAnnContent,         --Certificate Ann.
     *        rann     [17] RevAnnContent,          --Revocation Ann.
     *        crlann   [18] CRLAnnContent,          --CRL Announcement
     *        pkiconf  [19] PKIConfirmContent,      --Confirmation
     *        nested   [20] NestedMessageContent,   --Nested Message
     *        genm     [21] GenMsgContent,          --General Message
     *        genp     [22] GenRepContent,          --General Response
     *        error    [23] ErrorMsgContent,        --Error Message
     *        certConf [24] CertConfirmContent,     --Certificate confirm
     *        pollReq  [25] PollReqContent,         --Polling request
     *        pollRep  [26] PollRepContent          --Polling response
     * }
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        return new DERTaggedObject(true, tagNo, body);
    }
}
