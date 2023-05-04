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

package com.dotcms.enterprise.license.bouncycastle.asn1.ess;

import com.dotcms.enterprise.license.bouncycastle.asn1.*;
import com.dotcms.enterprise.license.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.IssuerSerial;

public class ESSCertIDv2
    extends ASN1Encodable
{
    private AlgorithmIdentifier hashAlgorithm;
    private byte[]              certHash;
    private IssuerSerial        issuerSerial;
    private static final AlgorithmIdentifier DEFAULT_ALG_ID = new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256);

    public static ESSCertIDv2 getInstance(
        Object o)
    {
        if (o == null || o instanceof ESSCertIDv2)
        {
            return (ESSCertIDv2) o;
        }
        else if (o instanceof ASN1Sequence)
        {
            return new ESSCertIDv2((ASN1Sequence) o);
        }

        throw new IllegalArgumentException(
                "unknown object in 'ESSCertIDv2' factory : "
                        + o.getClass().getName() + ".");
    }

    public ESSCertIDv2(
        ASN1Sequence seq)
    {
        if (seq.size() != 2 && seq.size() != 3)
        {
            throw new IllegalArgumentException("Bad sequence size: " + seq.size());
        }

        int count = 0;

        if (seq.getObjectAt(0) instanceof ASN1OctetString)
        {
            // Default value
            this.hashAlgorithm = DEFAULT_ALG_ID;
        }
        else
        {
            this.hashAlgorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(count++).getDERObject());
        }

        this.certHash = ASN1OctetString.getInstance(seq.getObjectAt(count++).getDERObject()).getOctets();

        if (seq.size() > count)
        {
            this.issuerSerial = new IssuerSerial(ASN1Sequence.getInstance(seq.getObjectAt(count).getDERObject()));
        }
    }

    public ESSCertIDv2(
        AlgorithmIdentifier algId,
        byte[]              certHash)
    {
        this(algId, certHash, null);
    }

    public ESSCertIDv2(
        AlgorithmIdentifier algId,
        byte[]              certHash,
        IssuerSerial        issuerSerial)
    {
        if (algId == null)
        {
            // Default value
            this.hashAlgorithm = DEFAULT_ALG_ID;
        }
        else
        {
            this.hashAlgorithm = algId;
        }

        this.certHash = certHash;
        this.issuerSerial = issuerSerial;
    }

    public AlgorithmIdentifier getHashAlgorithm()
    {
        return this.hashAlgorithm;
    }

    public byte[] getCertHash()
    {
        return certHash;
    }

    public IssuerSerial getIssuerSerial()
    {
        return issuerSerial;
    }

    /**
     * <pre>
     * ESSCertIDv2 ::=  SEQUENCE {
     *     hashAlgorithm     AlgorithmIdentifier
     *              DEFAULT {algorithm id-sha256},
     *     certHash          Hash,
     *     issuerSerial      IssuerSerial OPTIONAL
     * }
     *
     * Hash ::= OCTET STRING
     *
     * IssuerSerial ::= SEQUENCE {
     *     issuer         GeneralNames,
     *     serialNumber   CertificateSerialNumber
     * }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        if (!hashAlgorithm.equals(DEFAULT_ALG_ID))
        {
            v.add(hashAlgorithm);
        }

        v.add(new DEROctetString(certHash).toASN1Object());

        if (issuerSerial != null)
        {
            v.add(issuerSerial);
        }

        return new DERSequence(v);
    }

}
