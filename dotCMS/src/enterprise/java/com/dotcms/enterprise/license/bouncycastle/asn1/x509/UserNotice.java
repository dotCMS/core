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
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

/**
 * <code>UserNotice</code> class, used in
 * <code>CertificatePolicies</code> X509 extensions (in policy
 * qualifiers).
 * <pre>
 * UserNotice ::= SEQUENCE {
 *      noticeRef        NoticeReference OPTIONAL,
 *      explicitText     DisplayText OPTIONAL}
 *
 * </pre>
 * 
 * @see PolicyQualifierId
 * @see PolicyInformation
 */
public class UserNotice 
    extends ASN1Encodable 
{
    private NoticeReference noticeRef;
    private DisplayText     explicitText;
   
    /**
     * Creates a new <code>UserNotice</code> instance.
     *
     * @param noticeRef a <code>NoticeReference</code> value
     * @param explicitText a <code>DisplayText</code> value
     */
    public UserNotice(
        NoticeReference noticeRef, 
        DisplayText explicitText) 
    {
        this.noticeRef = noticeRef;
        this.explicitText = explicitText;
    }

    /**
     * Creates a new <code>UserNotice</code> instance.
     *
     * @param noticeRef a <code>NoticeReference</code> value
     * @param str the explicitText field as a String. 
     */
    public UserNotice(
        NoticeReference noticeRef, 
        String str) 
    {
        this.noticeRef = noticeRef;
        this.explicitText = new DisplayText(str);
    }

    /**
     * Creates a new <code>UserNotice</code> instance.
     * <p>Useful from reconstructing a <code>UserNotice</code> instance
     * from its encodable/encoded form. 
     *
     * @param as an <code>ASN1Sequence</code> value obtained from either
     * calling @{link toASN1Object()} for a <code>UserNotice</code>
     * instance or from parsing it from a DER-encoded stream. 
     */
    public UserNotice(
       ASN1Sequence as) 
    {
       if (as.size() == 2)
       {
           noticeRef = NoticeReference.getInstance(as.getObjectAt(0));
           explicitText = DisplayText.getInstance(as.getObjectAt(1));
       }
       else if (as.size() == 1)
       {
           if (as.getObjectAt(0).getDERObject() instanceof ASN1Sequence)
           {
               noticeRef = NoticeReference.getInstance(as.getObjectAt(0));
           }
           else
           {
               explicitText = DisplayText.getInstance(as.getObjectAt(0));
           }
       }
       else
       {
           throw new IllegalArgumentException("Bad sequence size: " + as.size());
       }
    }
   
    public NoticeReference getNoticeRef()
    {
        return noticeRef;
    }
    
    public DisplayText getExplicitText()
    {
        return explicitText;
    }
    
    public DERObject toASN1Object() 
    {
        ASN1EncodableVector av = new ASN1EncodableVector();
      
        if (noticeRef != null)
        {
            av.add(noticeRef);
        }
        
        if (explicitText != null)
        {
            av.add(explicitText);
        }
         
        return new DERSequence(av);
    }
}
