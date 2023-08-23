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
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEROctetString;
import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.digests.SHA1Digest;

/**
 * The SubjectKeyIdentifier object.
 * <pre>
 * SubjectKeyIdentifier::= OCTET STRING
 * </pre>
 */
public class SubjectKeyIdentifier
    extends ASN1Encodable
{
    private byte[] keyidentifier;

    public static SubjectKeyIdentifier getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1OctetString.getInstance(obj, explicit));
    }

    public static SubjectKeyIdentifier getInstance(
        Object obj)
    {
        if (obj instanceof SubjectKeyIdentifier)
        {
            return (SubjectKeyIdentifier)obj;
        }
        
        if (obj instanceof SubjectPublicKeyInfo) 
        {
            return new SubjectKeyIdentifier((SubjectPublicKeyInfo)obj);
        }
        
        if (obj instanceof ASN1OctetString) 
        {
            return new SubjectKeyIdentifier((ASN1OctetString)obj);
        }

        if (obj instanceof X509Extension)
        {
            return getInstance(X509Extension.convertValueToObject((X509Extension)obj));
        }

        throw new IllegalArgumentException("Invalid SubjectKeyIdentifier: " + obj.getClass().getName());
    }
    
    public SubjectKeyIdentifier(
        byte[] keyid)
    {
        this.keyidentifier=keyid;
    }

    public SubjectKeyIdentifier(
        ASN1OctetString  keyid)
    {
        this.keyidentifier=keyid.getOctets();
    }

    /**
     * Calculates the keyidentifier using a SHA1 hash over the BIT STRING
     * from SubjectPublicKeyInfo as defined in RFC3280.
     *
     * @param spki the subject public key info.
     */
    public SubjectKeyIdentifier(
        SubjectPublicKeyInfo    spki)
    {
        this.keyidentifier = getDigest(spki);
    }

    public byte[] getKeyIdentifier()
    {
        return keyidentifier;
    }

    public DERObject toASN1Object()
    {
        return new DEROctetString(keyidentifier);
    }

    /**
     * Return a RFC 3280 type 1 key identifier. As in:
     * <pre>
     * (1) The keyIdentifier is composed of the 160-bit SHA-1 hash of the
     * value of the BIT STRING subjectPublicKey (excluding the tag,
     * length, and number of unused bits).
     * </pre>
     * @param keyInfo the key info object containing the subjectPublicKey field.
     * @return the key identifier.
     */
    public static SubjectKeyIdentifier createSHA1KeyIdentifier(SubjectPublicKeyInfo keyInfo)
    {
        return new SubjectKeyIdentifier(keyInfo);
    }

    /**
     * Return a RFC 3280 type 2 key identifier. As in:
     * <pre>
     * (2) The keyIdentifier is composed of a four bit type field with
     * the value 0100 followed by the least significant 60 bits of the
     * SHA-1 hash of the value of the BIT STRING subjectPublicKey.
     * </pre>
     * @param keyInfo the key info object containing the subjectPublicKey field.
     * @return the key identifier.
     */
    public static SubjectKeyIdentifier createTruncatedSHA1KeyIdentifier(SubjectPublicKeyInfo keyInfo)
    {
        byte[] dig = getDigest(keyInfo);
        byte[] id = new byte[8];

        System.arraycopy(dig, dig.length - 8, id, 0, id.length);

        id[0] &= 0x0f;
        id[0] |= 0x40;
        
        return new SubjectKeyIdentifier(id);
    }

    private static byte[] getDigest(SubjectPublicKeyInfo spki)
    {
        Digest digest = new SHA1Digest();
        byte[]  resBuf = new byte[digest.getDigestSize()];

        byte[] bytes = spki.getPublicKeyData().getBytes();
        digest.update(bytes, 0, bytes.length);
        digest.doFinal(resBuf, 0);
        return resBuf;
    }
}
