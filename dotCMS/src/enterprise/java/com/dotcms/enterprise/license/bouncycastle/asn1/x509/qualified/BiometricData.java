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

package com.dotcms.enterprise.license.bouncycastle.asn1.x509.qualified;

import java.util.Enumeration;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERIA5String;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * The BiometricData object.
 * <pre>
 * BiometricData  ::=  SEQUENCE {
 *       typeOfBiometricData  TypeOfBiometricData,
 *       hashAlgorithm        AlgorithmIdentifier,
 *       biometricDataHash    OCTET STRING,
 *       sourceDataUri        IA5String OPTIONAL  }
 * </pre>
 */
public class BiometricData 
    extends ASN1Encodable
{
    TypeOfBiometricData typeOfBiometricData;
    AlgorithmIdentifier hashAlgorithm;
    ASN1OctetString     biometricDataHash;
    DERIA5String        sourceDataUri;    
    
    public static BiometricData getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof BiometricData)
        {
            return (BiometricData)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new BiometricData(ASN1Sequence.getInstance(obj));            
        }
        else
        {
            throw new IllegalArgumentException("unknown object in getInstance");
        }
    }                
            
    public BiometricData(ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();

        // typeOfBiometricData
        typeOfBiometricData = TypeOfBiometricData.getInstance(e.nextElement());
        // hashAlgorithm
        hashAlgorithm = AlgorithmIdentifier.getInstance(e.nextElement());
        // biometricDataHash
        biometricDataHash = ASN1OctetString.getInstance(e.nextElement());
        // sourceDataUri
        if (e.hasMoreElements())
        {
            sourceDataUri = DERIA5String.getInstance(e.nextElement());
        }
    }
    
    public BiometricData(
        TypeOfBiometricData typeOfBiometricData,
        AlgorithmIdentifier hashAlgorithm,
        ASN1OctetString     biometricDataHash,
        DERIA5String        sourceDataUri)
    {
        this.typeOfBiometricData = typeOfBiometricData;
        this.hashAlgorithm = hashAlgorithm;
        this.biometricDataHash = biometricDataHash;
        this.sourceDataUri = sourceDataUri;
    }
    
    public BiometricData(
        TypeOfBiometricData typeOfBiometricData,
        AlgorithmIdentifier hashAlgorithm,
        ASN1OctetString     biometricDataHash)
    {
        this.typeOfBiometricData = typeOfBiometricData;
        this.hashAlgorithm = hashAlgorithm;
        this.biometricDataHash = biometricDataHash;
        this.sourceDataUri = null;
    }

    public TypeOfBiometricData getTypeOfBiometricData()
    {
        return typeOfBiometricData;
    }
    
    public AlgorithmIdentifier getHashAlgorithm()
    {
        return hashAlgorithm;
    }
    
    public ASN1OctetString getBiometricDataHash()
    {
        return biometricDataHash;
    }
    
    public DERIA5String getSourceDataUri()
    {
        return sourceDataUri;
    }
    
    public DERObject toASN1Object() 
    {
        ASN1EncodableVector seq = new ASN1EncodableVector();
        seq.add(typeOfBiometricData);
        seq.add(hashAlgorithm);
        seq.add(biometricDataHash); 
        
        if (sourceDataUri != null)
        {
            seq.add(sourceDataUri);
        }

        return new DERSequence(seq);
    }
}
