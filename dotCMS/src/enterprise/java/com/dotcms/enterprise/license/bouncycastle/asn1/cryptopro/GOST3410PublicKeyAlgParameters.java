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

package com.dotcms.enterprise.license.bouncycastle.asn1.cryptopro;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

public class GOST3410PublicKeyAlgParameters
    extends ASN1Encodable
{
    private DERObjectIdentifier  publicKeyParamSet;
    private DERObjectIdentifier  digestParamSet;
    private DERObjectIdentifier  encryptionParamSet;
    
    public static GOST3410PublicKeyAlgParameters getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static GOST3410PublicKeyAlgParameters getInstance(
        Object obj)
    {
        if(obj == null || obj instanceof GOST3410PublicKeyAlgParameters)
        {
            return (GOST3410PublicKeyAlgParameters)obj;
        }

        if(obj instanceof ASN1Sequence)
        {
            return new GOST3410PublicKeyAlgParameters((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("Invalid GOST3410Parameter: " + obj.getClass().getName());
    }
    
    public GOST3410PublicKeyAlgParameters(
        DERObjectIdentifier  publicKeyParamSet,
        DERObjectIdentifier  digestParamSet)
    {
        this.publicKeyParamSet = publicKeyParamSet;
        this.digestParamSet = digestParamSet;
        this.encryptionParamSet = null;
    }

    public GOST3410PublicKeyAlgParameters(
        DERObjectIdentifier  publicKeyParamSet,
        DERObjectIdentifier  digestParamSet,
        DERObjectIdentifier  encryptionParamSet)
    {
        this.publicKeyParamSet = publicKeyParamSet;
        this.digestParamSet = digestParamSet;
        this.encryptionParamSet = encryptionParamSet;
    }

    public GOST3410PublicKeyAlgParameters(
        ASN1Sequence  seq)
    {
        this.publicKeyParamSet = (DERObjectIdentifier)seq.getObjectAt(0);
        this.digestParamSet = (DERObjectIdentifier)seq.getObjectAt(1);
        
        if (seq.size() > 2)
        {
            this.encryptionParamSet = (DERObjectIdentifier)seq.getObjectAt(2);
        }
    }

    public DERObjectIdentifier getPublicKeyParamSet()
    {
        return publicKeyParamSet;
    }

    public DERObjectIdentifier getDigestParamSet()
    {
        return digestParamSet;
    }

    public DERObjectIdentifier getEncryptionParamSet()
    {
        return encryptionParamSet;
    }

    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(publicKeyParamSet);
        v.add(digestParamSet);
        
        if (encryptionParamSet != null)
        {
            v.add(encryptionParamSet);
        }

        return new DERSequence(v);
    }
}
