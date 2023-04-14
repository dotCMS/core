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

package com.dotcms.enterprise.license.bouncycastle.asn1.isismtt.ocsp;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Choice;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEROctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.X509CertificateStructure;

import java.io.IOException;

/**
 * ISIS-MTT-Optional: The certificate requested by the client by inserting the
 * RetrieveIfAllowed extension in the request, will be returned in this
 * extension.
 * <p/>
 * ISIS-MTT-SigG: The signature act allows publishing certificates only then,
 * when the certificate owner gives his explicit permission. Accordingly, there
 * may be �nondownloadable� certificates, about which the responder must provide
 * status information, but MUST NOT include them in the response. Clients may
 * get therefore the following three kind of answers on a single request
 * including the RetrieveIfAllowed extension:
 * <ul>
 * <li> a) the responder supports the extension and is allowed to publish the
 * certificate: RequestedCertificate returned including the requested
 * certificate
 * <li>b) the responder supports the extension but is NOT allowed to publish
 * the certificate: RequestedCertificate returned including an empty OCTET
 * STRING
 * <li>c) the responder does not support the extension: RequestedCertificate is
 * not included in the response
 * </ul>
 * Clients requesting RetrieveIfAllowed MUST be able to handle these cases. If
 * any of the OCTET STRING options is used, it MUST contain the DER encoding of
 * the requested certificate.
 * <p/>
 * <pre>
 *            RequestedCertificate ::= CHOICE {
 *              Certificate Certificate,
 *              publicKeyCertificate [0] EXPLICIT OCTET STRING,
 *              attributeCertificate [1] EXPLICIT OCTET STRING
 *            }
 * </pre>
 */
public class RequestedCertificate
    extends ASN1Encodable
    implements ASN1Choice
{
    public static final int certificate = -1;
    public static final int publicKeyCertificate = 0;
    public static final int attributeCertificate = 1;

    private X509CertificateStructure cert;
    private byte[] publicKeyCert;
    private byte[] attributeCert;

    public static RequestedCertificate getInstance(Object obj)
    {
        if (obj == null || obj instanceof RequestedCertificate)
        {
            return (RequestedCertificate)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new RequestedCertificate(X509CertificateStructure.getInstance(obj));
        }
        if (obj instanceof ASN1TaggedObject)
        {
            return new RequestedCertificate((ASN1TaggedObject)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }

    public static RequestedCertificate getInstance(ASN1TaggedObject obj, boolean explicit)
    {
        if (!explicit)
        {
            throw new IllegalArgumentException("choice item must be explicitly tagged");
        }

        return getInstance(obj.getObject());
    }

    private RequestedCertificate(ASN1TaggedObject tagged)
    {
        if (tagged.getTagNo() == publicKeyCertificate)
        {
            publicKeyCert = ASN1OctetString.getInstance(tagged, true).getOctets();
        }
        else if (tagged.getTagNo() == attributeCertificate)
        {
            attributeCert = ASN1OctetString.getInstance(tagged, true).getOctets();
        }
        else
        {
            throw new IllegalArgumentException("unknown tag number: " + tagged.getTagNo());
        }
    }

    /**
     * Constructor from a given details.
     * <p/>
     * Only one parameter can be given. All other must be <code>null</code>.
     *
     * @param certificate          Given as Certificate
     */
    public RequestedCertificate(X509CertificateStructure certificate)
    {
        this.cert = certificate;
    }

    public RequestedCertificate(int type, byte[] certificateOctets)
    {
        this(new DERTaggedObject(type, new DEROctetString(certificateOctets)));
    }

    public int getType()
    {
        if (cert != null)
        {
            return certificate;
        }
        if (publicKeyCert != null)
        {
            return publicKeyCertificate;
        }
        return attributeCertificate;
    }

    public byte[] getCertificateBytes()
    {
        if (cert != null)
        {
            try
            {
                return cert.getEncoded();
            }
            catch (IOException e)
            {
                throw new IllegalStateException("can't decode certificate: " + e);
            }
        }
        if (publicKeyCert != null)
        {
            return publicKeyCert;
        }
        return attributeCert;
    }
    
    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *            RequestedCertificate ::= CHOICE {
     *              Certificate Certificate,
     *              publicKeyCertificate [0] EXPLICIT OCTET STRING,
     *              attributeCertificate [1] EXPLICIT OCTET STRING
     *            }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        if (publicKeyCert != null)
        {
            return new DERTaggedObject(0, new DEROctetString(publicKeyCert));
        }
        if (attributeCert != null)
        {
            return new DERTaggedObject(1, new DEROctetString(attributeCert));
        }
        return cert.getDERObject();
    }
}
