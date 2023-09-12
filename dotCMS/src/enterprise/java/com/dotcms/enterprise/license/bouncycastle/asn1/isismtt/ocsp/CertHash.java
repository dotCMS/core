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

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEROctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * ISIS-MTT PROFILE: The responder may include this extension in a response to
 * send the hash of the requested certificate to the responder. This hash is
 * cryptographically bound to the certificate and serves as evidence that the
 * certificate is known to the responder (i.e. it has been issued and is present
 * in the directory). Hence, this extension is a means to provide a positive
 * statement of availability as described in T8.[8]. As explained in T13.[1],
 * clients may rely on this information to be able to validate signatures after
 * the expiry of the corresponding certificate. Hence, clients MUST support this
 * extension. If a positive statement of availability is to be delivered, this
 * extension syntax and OID MUST be used.
 * <p/>
 * <p/>
 * <pre>
 *     CertHash ::= SEQUENCE {
 *       hashAlgorithm AlgorithmIdentifier,
 *       certificateHash OCTET STRING
 *     }
 * </pre>
 */
public class CertHash
    extends ASN1Encodable
{

    private AlgorithmIdentifier hashAlgorithm;
    private byte[] certificateHash;

    public static CertHash getInstance(Object obj)
    {
        if (obj == null || obj instanceof CertHash)
        {
            return (CertHash)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new CertHash((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("illegal object in getInstance: "
            + obj.getClass().getName());
    }

    /**
     * Constructor from ASN1Sequence.
     * <p/>
     * The sequence is of type CertHash:
     * <p/>
     * <pre>
     *     CertHash ::= SEQUENCE {
     *       hashAlgorithm AlgorithmIdentifier,
     *       certificateHash OCTET STRING
     *     }
     * </pre>
     *
     * @param seq The ASN.1 sequence.
     */
    private CertHash(ASN1Sequence seq)
    {
        if (seq.size() != 2)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                + seq.size());
        }
        hashAlgorithm = AlgorithmIdentifier.getInstance(seq.getObjectAt(0));
        certificateHash = DEROctetString.getInstance(seq.getObjectAt(1)).getOctets();
    }

    /**
     * Constructor from a given details.
     *
     * @param hashAlgorithm   The hash algorithm identifier.
     * @param certificateHash The hash of the whole DER encoding of the certificate.
     */
    public CertHash(AlgorithmIdentifier hashAlgorithm, byte[] certificateHash)
    {
        this.hashAlgorithm = hashAlgorithm;
        this.certificateHash = new byte[certificateHash.length];
        System.arraycopy(certificateHash, 0, this.certificateHash, 0,
            certificateHash.length);
    }

    public AlgorithmIdentifier getHashAlgorithm()
    {
        return hashAlgorithm;
    }

    public byte[] getCertificateHash()
    {
        return certificateHash;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <p/>
     * Returns:
     * <p/>
     * <pre>
     *     CertHash ::= SEQUENCE {
     *       hashAlgorithm AlgorithmIdentifier,
     *       certificateHash OCTET STRING
     *     }
     * </pre>
     *
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        vec.add(hashAlgorithm);
        vec.add(new DEROctetString(certificateHash));
        return new DERSequence(vec);
    }
}
