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
import com.dotcms.enterprise.license.bouncycastle.asn1.DERBitString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERGeneralizedTime;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSet;

/**
 * Generator for Version 2 AttributeCertificateInfo
 * <pre>
 * AttributeCertificateInfo ::= SEQUENCE {
 *       version              AttCertVersion -- version is v2,
 *       holder               Holder,
 *       issuer               AttCertIssuer,
 *       signature            AlgorithmIdentifier,
 *       serialNumber         CertificateSerialNumber,
 *       attrCertValidityPeriod   AttCertValidityPeriod,
 *       attributes           SEQUENCE OF Attribute,
 *       issuerUniqueID       UniqueIdentifier OPTIONAL,
 *       extensions           Extensions OPTIONAL
 * }
 * </pre>
 *
 */
public class V2AttributeCertificateInfoGenerator
{
    private DERInteger version;
    private Holder holder;
    private AttCertIssuer issuer;
    private AlgorithmIdentifier signature;
    private DERInteger serialNumber;
    private ASN1EncodableVector attributes;
    private DERBitString issuerUniqueID;
    private X509Extensions extensions;

    // Note: validity period start/end dates stored directly
    //private AttCertValidityPeriod attrCertValidityPeriod;
    private DERGeneralizedTime startDate, endDate; 

    public V2AttributeCertificateInfoGenerator()
    {
        this.version = new DERInteger(1);
        attributes = new ASN1EncodableVector();
    }
    
    public void setHolder(Holder holder)
    {
        this.holder = holder;
    }
    
    public void addAttribute(String oid, ASN1Encodable value) 
    {
        attributes.add(new Attribute(new DERObjectIdentifier(oid), new DERSet(value)));
    }

    /**
     * @param attribute
     */
    public void addAttribute(Attribute attribute)
    {
        attributes.add(attribute);
    }
    
    public void setSerialNumber(
        DERInteger  serialNumber)
    {
        this.serialNumber = serialNumber;
    }

    public void setSignature(
        AlgorithmIdentifier    signature)
    {
        this.signature = signature;
    }

    public void setIssuer(
        AttCertIssuer    issuer)
    {
        this.issuer = issuer;
    }

    public void setStartDate(
        DERGeneralizedTime startDate)
    {
        this.startDate = startDate;
    }

    public void setEndDate(
        DERGeneralizedTime endDate)
    {
        this.endDate = endDate;
    }

    public void setIssuerUniqueID(
        DERBitString    issuerUniqueID)
    {
        this.issuerUniqueID = issuerUniqueID;
    }

    public void setExtensions(
        X509Extensions    extensions)
    {
        this.extensions = extensions;
    }

    public AttributeCertificateInfo generateAttributeCertificateInfo()
    {
        if ((serialNumber == null) || (signature == null)
            || (issuer == null) || (startDate == null) || (endDate == null)
            || (holder == null) || (attributes == null))
        {
            throw new IllegalStateException("not all mandatory fields set in V2 AttributeCertificateInfo generator");
        }

        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(version);
        v.add(holder);
        v.add(issuer);
        v.add(signature);
        v.add(serialNumber);
    
        //
        // before and after dates => AttCertValidityPeriod
        //
        AttCertValidityPeriod validity = new AttCertValidityPeriod(startDate, endDate);
        v.add(validity);
        
        // Attributes
        v.add(new DERSequence(attributes));
        
        if (issuerUniqueID != null)
        {
            v.add(issuerUniqueID);
        }
    
        if (extensions != null)
        {
            v.add(extensions);
        }

        return new AttributeCertificateInfo(new DERSequence(v));
    }
}
