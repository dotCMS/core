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
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

/**
 * <code>NoticeReference</code> class, used in
 * <code>CertificatePolicies</code> X509 V3 extensions
 * (in policy qualifiers).
 * 
 * <pre>
 *  NoticeReference ::= SEQUENCE {
 *      organization     DisplayText,
 *      noticeNumbers    SEQUENCE OF INTEGER }
 *
 * </pre> 
 * 
 * @see PolicyQualifierInfo
 * @see PolicyInformation
 */
public class NoticeReference 
    extends ASN1Encodable
{
   private DisplayText organization;
   private ASN1Sequence noticeNumbers;

   /**
    * Creates a new <code>NoticeReference</code> instance.
    *
    * @param orgName a <code>String</code> value
    * @param numbers a <code>Vector</code> value
    */
   public NoticeReference(
       String orgName,
       Vector numbers) 
   {
      organization = new DisplayText(orgName);

      Object o = numbers.elementAt(0);

      ASN1EncodableVector av = new ASN1EncodableVector();
      if (o instanceof Integer)
      {
         Enumeration it = numbers.elements();

         while (it.hasMoreElements())
         {
            Integer nm = (Integer) it.nextElement();
               DERInteger di = new DERInteger(nm.intValue());
            av.add (di);
         }
      }

      noticeNumbers = new DERSequence(av);
   }

   /**
    * Creates a new <code>NoticeReference</code> instance.
    *
    * @param orgName a <code>String</code> value
    * @param numbers an <code>ASN1EncodableVector</code> value
    */
   public NoticeReference(
       String orgName, 
       ASN1Sequence numbers) 
   {
       organization = new DisplayText (orgName);
       noticeNumbers = numbers;
   }

   /**
    * Creates a new <code>NoticeReference</code> instance.
    *
    * @param displayTextType an <code>int</code> value
    * @param orgName a <code>String</code> value
    * @param numbers an <code>ASN1EncodableVector</code> value
    */
   public NoticeReference(
       int displayTextType,
       String orgName,
       ASN1Sequence numbers) 
   {
       organization = new DisplayText(displayTextType, 
                                     orgName);
       noticeNumbers = numbers;
   }

   /**
    * Creates a new <code>NoticeReference</code> instance.
    * <p>Useful for reconstructing a <code>NoticeReference</code>
    * instance from its encodable/encoded form. 
    *
    * @param as an <code>ASN1Sequence</code> value obtained from either
    * calling @{link toASN1Object()} for a <code>NoticeReference</code>
    * instance or from parsing it from a DER-encoded stream. 
    */
   public NoticeReference(
       ASN1Sequence as) 
   {
       if (as.size() != 2)
       {
            throw new IllegalArgumentException("Bad sequence size: "
                    + as.size());
       }

       organization = DisplayText.getInstance(as.getObjectAt(0));
       noticeNumbers = ASN1Sequence.getInstance(as.getObjectAt(1));
   }

   public static NoticeReference getInstance(
       Object as) 
   {
      if (as instanceof NoticeReference)
      {
          return (NoticeReference)as;
      }
      else if (as instanceof ASN1Sequence)
      {
          return new NoticeReference((ASN1Sequence)as);
      }

      throw new IllegalArgumentException("unknown object in getInstance.");
   }
   
   public DisplayText getOrganization()
   {
       return organization;
   }
   
   public ASN1Sequence getNoticeNumbers()
   {
       return noticeNumbers;
   }
   
   /**
    * Describe <code>toASN1Object</code> method here.
    *
    * @return a <code>DERObject</code> value
    */
   public DERObject toASN1Object() 
   {
      ASN1EncodableVector av = new ASN1EncodableVector();
      av.add (organization);
      av.add (noticeNumbers);
      return new DERSequence (av);
   }
}
