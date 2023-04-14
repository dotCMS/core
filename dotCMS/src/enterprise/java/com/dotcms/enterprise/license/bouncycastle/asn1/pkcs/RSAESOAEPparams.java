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

package com.dotcms.enterprise.license.bouncycastle.asn1.pkcs;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERNull;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEROctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class RSAESOAEPparams
    extends ASN1Encodable
{
    private AlgorithmIdentifier hashAlgorithm;
    private AlgorithmIdentifier maskGenAlgorithm;
    private AlgorithmIdentifier pSourceAlgorithm;
    
    public final static AlgorithmIdentifier DEFAULT_HASH_ALGORITHM = new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1, new DERNull());
    public final static AlgorithmIdentifier DEFAULT_MASK_GEN_FUNCTION = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_mgf1, DEFAULT_HASH_ALGORITHM);
    public final static AlgorithmIdentifier DEFAULT_P_SOURCE_ALGORITHM = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_pSpecified, new DEROctetString(new byte[0]));
    
    public static RSAESOAEPparams getInstance(
        Object  obj)
    {
        if (obj instanceof RSAESOAEPparams)
        {
            return (RSAESOAEPparams)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new RSAESOAEPparams((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }
    
    /**
     * The default version
     */
    public RSAESOAEPparams()
    {
        hashAlgorithm = DEFAULT_HASH_ALGORITHM;
        maskGenAlgorithm = DEFAULT_MASK_GEN_FUNCTION;
        pSourceAlgorithm = DEFAULT_P_SOURCE_ALGORITHM;
    }
    
    public RSAESOAEPparams(
        AlgorithmIdentifier hashAlgorithm,
        AlgorithmIdentifier maskGenAlgorithm,
        AlgorithmIdentifier pSourceAlgorithm)
    {
        this.hashAlgorithm = hashAlgorithm;
        this.maskGenAlgorithm = maskGenAlgorithm;
        this.pSourceAlgorithm = pSourceAlgorithm;
    }
    
    public RSAESOAEPparams(
        ASN1Sequence seq)
    {
        hashAlgorithm = DEFAULT_HASH_ALGORITHM;
        maskGenAlgorithm = DEFAULT_MASK_GEN_FUNCTION;
        pSourceAlgorithm = DEFAULT_P_SOURCE_ALGORITHM;
        
        for (int i = 0; i != seq.size(); i++)
        {
            ASN1TaggedObject    o = (ASN1TaggedObject)seq.getObjectAt(i);
            
            switch (o.getTagNo())
            {
            case 0:
                hashAlgorithm = AlgorithmIdentifier.getInstance(o, true);
                break;
            case 1:
                maskGenAlgorithm = AlgorithmIdentifier.getInstance(o, true);
                break;
            case 2:
                pSourceAlgorithm = AlgorithmIdentifier.getInstance(o, true);
                break;
            default:
                throw new IllegalArgumentException("unknown tag");
            }
        }
    }
    
    public AlgorithmIdentifier getHashAlgorithm()
    {
        return hashAlgorithm;
    }
    
    public AlgorithmIdentifier getMaskGenAlgorithm()
    {
        return maskGenAlgorithm;
    }
    
    public AlgorithmIdentifier getPSourceAlgorithm()
    {
        return pSourceAlgorithm;
    }
    
    /**
     * <pre>
     *  RSAES-OAEP-params ::= SEQUENCE {
     *     hashAlgorithm      [0] OAEP-PSSDigestAlgorithms     DEFAULT sha1,
     *     maskGenAlgorithm   [1] PKCS1MGFAlgorithms  DEFAULT mgf1SHA1,
     *     pSourceAlgorithm   [2] PKCS1PSourceAlgorithms  DEFAULT pSpecifiedEmpty
     *   }
     *  
     *   OAEP-PSSDigestAlgorithms    ALGORITHM-IDENTIFIER ::= {
     *     { OID id-sha1 PARAMETERS NULL   }|
     *     { OID id-sha256 PARAMETERS NULL }|
     *     { OID id-sha384 PARAMETERS NULL }|
     *     { OID id-sha512 PARAMETERS NULL },
     *     ...  -- Allows for future expansion --
     *   }
     *   PKCS1MGFAlgorithms    ALGORITHM-IDENTIFIER ::= {
     *     { OID id-mgf1 PARAMETERS OAEP-PSSDigestAlgorithms },
     *    ...  -- Allows for future expansion --
     *   }
     *   PKCS1PSourceAlgorithms    ALGORITHM-IDENTIFIER ::= {
     *     { OID id-pSpecified PARAMETERS OCTET STRING },
     *     ...  -- Allows for future expansion --
     *  }
     * </pre>
     * @return the asn1 primitive representing the parameters.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        
        if (!hashAlgorithm.equals(DEFAULT_HASH_ALGORITHM))
        {
            v.add(new DERTaggedObject(true, 0, hashAlgorithm));
        }
        
        if (!maskGenAlgorithm.equals(DEFAULT_MASK_GEN_FUNCTION))
        {
            v.add(new DERTaggedObject(true, 1, maskGenAlgorithm));
        }
        
        if (!pSourceAlgorithm.equals(DEFAULT_P_SOURCE_ALGORITHM))
        {
            v.add(new DERTaggedObject(true, 2, pSourceAlgorithm));
        }
        
        return new DERSequence(v);
    }
}
