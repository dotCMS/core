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
import com.dotcms.enterprise.license.bouncycastle.asn1.crmf.EncryptedValue;

public class CertOrEncCert
    extends ASN1Encodable
    implements ASN1Choice
{
    private CMPCertificate certificate;
    private EncryptedValue encryptedCert;

    private CertOrEncCert(ASN1TaggedObject tagged)
    {
        if (tagged.getTagNo() == 0)
        {
            certificate = CMPCertificate.getInstance(tagged.getObject());
        }
        else if (tagged.getTagNo() == 1)
        {
            encryptedCert = EncryptedValue.getInstance(tagged.getObject());
        }
        else
        {
            throw new IllegalArgumentException("unknown tag: " + tagged.getTagNo());
        }
    }

    public static CertOrEncCert getInstance(Object o)
    {
        if (o instanceof CertOrEncCert)
        {
            return (CertOrEncCert)o;
        }

        if (o instanceof ASN1TaggedObject)
        {
            return new CertOrEncCert((ASN1TaggedObject)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public CMPCertificate getCertificate()
    {
        return certificate;
    }

    public EncryptedValue getEncryptedCert()
    {
        return encryptedCert;
    }

    /**
     * <pre>
     * CertOrEncCert ::= CHOICE {
     *                      certificate     [0] CMPCertificate,
     *                      encryptedCert   [1] EncryptedValue
     *           }
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        if (certificate != null)
        {
            return new DERTaggedObject(true, 0, certificate);
        }

        return new DERTaggedObject(true, 1, encryptedCert);
    }
}
