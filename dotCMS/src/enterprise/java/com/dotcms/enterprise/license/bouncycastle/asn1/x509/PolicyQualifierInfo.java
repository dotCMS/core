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
import com.dotcms.enterprise.license.bouncycastle.asn1.DEREncodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERIA5String;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

/**
 * Policy qualifiers, used in the X509V3 CertificatePolicies
 * extension.
 * 
 * <pre>
 *   PolicyQualifierInfo ::= SEQUENCE {
 *       policyQualifierId  PolicyQualifierId,
 *       qualifier          ANY DEFINED BY policyQualifierId }
 * </pre>
 */
public class PolicyQualifierInfo
    extends ASN1Encodable
{
   private DERObjectIdentifier policyQualifierId;
   private DEREncodable        qualifier;

   /**
    * Creates a new <code>PolicyQualifierInfo</code> instance.
    *
    * @param policyQualifierId a <code>PolicyQualifierId</code> value
    * @param qualifier the qualifier, defined by the above field.
    */
   public PolicyQualifierInfo(
       DERObjectIdentifier policyQualifierId,
       DEREncodable qualifier) 
   {
      this.policyQualifierId = policyQualifierId;
      this.qualifier = qualifier;
   }

   /**
    * Creates a new <code>PolicyQualifierInfo</code> containing a
    * cPSuri qualifier.
    *
    * @param cps the CPS (certification practice statement) uri as a
    * <code>String</code>.
    */
   public PolicyQualifierInfo(
       String cps) 
   {
      policyQualifierId = PolicyQualifierId.id_qt_cps;
      qualifier = new DERIA5String (cps);
   }

   /**
    * Creates a new <code>PolicyQualifierInfo</code> instance.
    *
    * @param as <code>PolicyQualifierInfo</code> X509 structure
    * encoded as an ASN1Sequence. 
    */
   public PolicyQualifierInfo(
       ASN1Sequence as)
   {
        if (as.size() != 2)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                    + as.size());
        }

        policyQualifierId = DERObjectIdentifier.getInstance(as.getObjectAt(0));
        qualifier = as.getObjectAt(1);
   }

   public static PolicyQualifierInfo getInstance(
       Object as) 
   {
        if (as instanceof PolicyQualifierInfo)
        {
            return (PolicyQualifierInfo)as;
        }
        else if (as instanceof ASN1Sequence)
        {
            return new PolicyQualifierInfo((ASN1Sequence)as);
        }

        throw new IllegalArgumentException("unknown object in getInstance.");
   }


   public DERObjectIdentifier getPolicyQualifierId()
   {
       return policyQualifierId;
   }

   public DEREncodable getQualifier()
   {
       return qualifier;
   }
   
   /**
    * Returns a DER-encodable representation of this instance. 
    *
    * @return a <code>DERObject</code> value
    */
   public DERObject toASN1Object() 
   {
      ASN1EncodableVector dev = new ASN1EncodableVector();
      dev.add(policyQualifierId);
      dev.add(qualifier);

      return new DERSequence(dev);
   }
}
