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

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Choice;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERBMPString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERIA5String;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERUTF8String;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERVisibleString;

/**
 * <code>DisplayText</code> class, used in
 * <code>CertificatePolicies</code> X509 V3 extensions (in policy qualifiers).
 *
 * <p>It stores a string in a chosen encoding. 
 * <pre>
 * DisplayText ::= CHOICE {
 *      ia5String        IA5String      (SIZE (1..200)),
 *      visibleString    VisibleString  (SIZE (1..200)),
 *      bmpString        BMPString      (SIZE (1..200)),
 *      utf8String       UTF8String     (SIZE (1..200)) }
 * </pre>
 * @see PolicyQualifierInfo
 * @see PolicyInformation
 */
public class DisplayText 
    extends ASN1Encodable
    implements ASN1Choice
{
   /**
    * Constant corresponding to ia5String encoding. 
    *
    */
   public static final int CONTENT_TYPE_IA5STRING = 0;
   /**
    * Constant corresponding to bmpString encoding. 
    *
    */
   public static final int CONTENT_TYPE_BMPSTRING = 1;
   /**
    * Constant corresponding to utf8String encoding. 
    *
    */
   public static final int CONTENT_TYPE_UTF8STRING = 2;
   /**
    * Constant corresponding to visibleString encoding. 
    *
    */
   public static final int CONTENT_TYPE_VISIBLESTRING = 3;

   /**
    * Describe constant <code>DISPLAY_TEXT_MAXIMUM_SIZE</code> here.
    *
    */
   public static final int DISPLAY_TEXT_MAXIMUM_SIZE = 200;
   
   int contentType;
   DERString contents;
   
   /**
    * Creates a new <code>DisplayText</code> instance.
    *
    * @param type the desired encoding type for the text. 
    * @param text the text to store. Strings longer than 200
    * characters are truncated. 
    */
   public DisplayText (int type, String text) 
   {
      if (text.length() > DISPLAY_TEXT_MAXIMUM_SIZE)
      {
         // RFC3280 limits these strings to 200 chars
         // truncate the string
         text = text.substring (0, DISPLAY_TEXT_MAXIMUM_SIZE);
      }
     
      contentType = type;
      switch (type)
      {
         case CONTENT_TYPE_IA5STRING:
            contents = (DERString)new DERIA5String (text);
            break;
         case CONTENT_TYPE_UTF8STRING:
            contents = (DERString)new DERUTF8String(text);
            break;
         case CONTENT_TYPE_VISIBLESTRING:
            contents = (DERString)new DERVisibleString(text);
            break;
         case CONTENT_TYPE_BMPSTRING:
            contents = (DERString)new DERBMPString(text);
            break;
         default:
            contents = (DERString)new DERUTF8String(text);
            break;
      }
   }
   
   /**
    * Creates a new <code>DisplayText</code> instance.
    *
    * @param text the text to encapsulate. Strings longer than 200
    * characters are truncated. 
    */
   public DisplayText (String text) 
   {
      // by default use UTF8String
      if (text.length() > DISPLAY_TEXT_MAXIMUM_SIZE)
      {
         text = text.substring(0, DISPLAY_TEXT_MAXIMUM_SIZE);
      }
      
      contentType = CONTENT_TYPE_UTF8STRING;
      contents = new DERUTF8String(text);
   }

   /**
    * Creates a new <code>DisplayText</code> instance.
    * <p>Useful when reading back a <code>DisplayText</code> class
    * from it's ASN1Encodable/DEREncodable form. 
    *
    * @param de a <code>DEREncodable</code> instance. 
    */
   public DisplayText(DERString de)
   {
      contents = de;
   }

   public static DisplayText getInstance(Object obj) 
   {
      if (obj instanceof DERString)
      {
          return new DisplayText((DERString)obj);
      }
      else if (obj instanceof DisplayText)
      {
          return (DisplayText)obj;
      }

      throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
   }

   public static DisplayText getInstance(
       ASN1TaggedObject obj,
       boolean          explicit)
   {
       return getInstance(obj.getObject()); // must be explicitly tagged
   }
   
   public DERObject toASN1Object() 
   {
      return (DERObject)contents;
   }

   /**
    * Returns the stored <code>String</code> object. 
    *
    * @return the stored text as a <code>String</code>. 
    */
   public String getString() 
   {
      return contents.getString();
   }   
}
