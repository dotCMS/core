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

package com.dotcms.enterprise.license.bouncycastle.crypto.util;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1InputStream;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Object;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERBitString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEREncodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEROctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.nist.NISTNamedCurves;
import com.dotcms.enterprise.license.bouncycastle.asn1.oiw.ElGamalParameter;
import com.dotcms.enterprise.license.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import com.dotcms.enterprise.license.bouncycastle.asn1.pkcs.DHParameter;
import com.dotcms.enterprise.license.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.dotcms.enterprise.license.bouncycastle.asn1.sec.SECNamedCurves;
import com.dotcms.enterprise.license.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.DSAParameter;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.RSAPublicKeyStructure;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import com.dotcms.enterprise.license.bouncycastle.asn1.x9.X962NamedCurves;
import com.dotcms.enterprise.license.bouncycastle.asn1.x9.X962Parameters;
import com.dotcms.enterprise.license.bouncycastle.asn1.x9.X9ECParameters;
import com.dotcms.enterprise.license.bouncycastle.asn1.x9.X9ECPoint;
import com.dotcms.enterprise.license.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.AsymmetricKeyParameter;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.DHParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.DHPublicKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.DSAParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.DSAPublicKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ECDomainParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ECPublicKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ElGamalParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.ElGamalPublicKeyParameters;
import com.dotcms.enterprise.license.bouncycastle.crypto.params.RSAKeyParameters;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

/**
 * Factory to create asymmetric public key parameters for asymmetric ciphers
 * from range of ASN.1 encoded SubjectPublicKeyInfo objects.
 */
public class PublicKeyFactory
{
    /**
     * Create a public key from a SubjectPublicKeyInfo encoding
     * 
     * @param keyInfoData the SubjectPublicKeyInfo encoding
     * @return the appropriate key parameter
     * @throws IOException on an error decoding the key
     */
    public static AsymmetricKeyParameter createKey(
        byte[] keyInfoData)
        throws IOException
    {
        return createKey(
            SubjectPublicKeyInfo.getInstance(
                ASN1Object.fromByteArray(keyInfoData)));
    }

    /**
     * Create a public key from a SubjectPublicKeyInfo encoding read from a stream
     * 
     * @param inStr the stream to read the SubjectPublicKeyInfo encoding from
     * @return the appropriate key parameter
     * @throws IOException on an error decoding the key
     */
    public static AsymmetricKeyParameter createKey(
        InputStream inStr)
        throws IOException
    {
        return createKey(
            SubjectPublicKeyInfo.getInstance(
                new ASN1InputStream(inStr).readObject()));
    }

    /**
     * Create a public key from the passed in SubjectPublicKeyInfo
     * 
     * @param keyInfo the SubjectPublicKeyInfo containing the key data
     * @return the appropriate key parameter
     * @throws IOException on an error decoding the key
     */
    public static AsymmetricKeyParameter createKey(
        SubjectPublicKeyInfo    keyInfo)
        throws IOException
    {
        AlgorithmIdentifier     algId = keyInfo.getAlgorithmId();
        
        if (algId.getObjectId().equals(PKCSObjectIdentifiers.rsaEncryption)
            || algId.getObjectId().equals(X509ObjectIdentifiers.id_ea_rsa))
        {
            RSAPublicKeyStructure   pubKey = new RSAPublicKeyStructure((ASN1Sequence)keyInfo.getPublicKey());

            return new RSAKeyParameters(false, pubKey.getModulus(), pubKey.getPublicExponent());
        }
        else if (algId.getObjectId().equals(PKCSObjectIdentifiers.dhKeyAgreement)
                 || algId.getObjectId().equals(X9ObjectIdentifiers.dhpublicnumber))
        {
            DHParameter params = new DHParameter((ASN1Sequence)keyInfo.getAlgorithmId().getParameters());
            DERInteger  derY = (DERInteger)keyInfo.getPublicKey();
            
            BigInteger lVal = params.getL();
            int l = lVal == null ? 0 : lVal.intValue();
            DHParameters dhParams = new DHParameters(params.getP(), params.getG(), null, l);

            return new DHPublicKeyParameters(derY.getValue(), dhParams);
        }
        else if (algId.getObjectId().equals(OIWObjectIdentifiers.elGamalAlgorithm))
        {
            ElGamalParameter    params = new ElGamalParameter((ASN1Sequence)keyInfo.getAlgorithmId().getParameters());
            DERInteger          derY = (DERInteger)keyInfo.getPublicKey();

            return new ElGamalPublicKeyParameters(derY.getValue(), new ElGamalParameters(params.getP(), params.getG()));
        }
        else if (algId.getObjectId().equals(X9ObjectIdentifiers.id_dsa)
                 || algId.getObjectId().equals(OIWObjectIdentifiers.dsaWithSHA1))
        {
            DERInteger derY = (DERInteger)keyInfo.getPublicKey();
            DEREncodable de = keyInfo.getAlgorithmId().getParameters();

            DSAParameters parameters = null;
            if (de != null)
            {
                DSAParameter params = DSAParameter.getInstance(de.getDERObject());
                parameters = new DSAParameters(params.getP(), params.getQ(), params.getG());
            }

            return new DSAPublicKeyParameters(derY.getValue(), parameters);
        }
        else if (algId.getObjectId().equals(X9ObjectIdentifiers.id_ecPublicKey))
        {
            X962Parameters      params = new X962Parameters((DERObject)keyInfo.getAlgorithmId().getParameters());
            ECDomainParameters  dParams = null;
            
            if (params.isNamedCurve())
            {
                DERObjectIdentifier oid = (DERObjectIdentifier)params.getParameters();
                X9ECParameters      ecP = X962NamedCurves.getByOID(oid);

                if (ecP == null)
                {
                    ecP = SECNamedCurves.getByOID(oid);

                    if (ecP == null)
                    {
                        ecP = NISTNamedCurves.getByOID(oid);

                        if (ecP == null)
                        {
                            ecP = TeleTrusTNamedCurves.getByOID(oid);
                        }
                    }
                }

                dParams = new ECDomainParameters(
                                            ecP.getCurve(),
                                            ecP.getG(),
                                            ecP.getN(),
                                            ecP.getH(),
                                            ecP.getSeed());
            }
            else
            {
                X9ECParameters ecP = new X9ECParameters(
                            (ASN1Sequence)params.getParameters());
                dParams = new ECDomainParameters(
                                            ecP.getCurve(),
                                            ecP.getG(),
                                            ecP.getN(),
                                            ecP.getH(),
                                            ecP.getSeed());
            }

            DERBitString    bits = keyInfo.getPublicKeyData();
            byte[]          data = bits.getBytes();
            ASN1OctetString key = new DEROctetString(data);

            X9ECPoint       derQ = new X9ECPoint(dParams.getCurve(), key);
            
            return new ECPublicKeyParameters(derQ.getPoint(), dParams);
        }
        else
        {
            throw new RuntimeException("algorithm identifier in key not recognised");
        }
    }
}
