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

package com.dotcms.enterprise.license.bouncycastle.asn1.cms;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class KeyAgreeRecipientInfo
    extends ASN1Encodable
{
    private DERInteger                  version;
    private OriginatorIdentifierOrKey   originator;
    private ASN1OctetString             ukm;
    private AlgorithmIdentifier         keyEncryptionAlgorithm;
    private ASN1Sequence                recipientEncryptedKeys;
    
    public KeyAgreeRecipientInfo(
        OriginatorIdentifierOrKey   originator,
        ASN1OctetString             ukm,
        AlgorithmIdentifier         keyEncryptionAlgorithm,
        ASN1Sequence                recipientEncryptedKeys)
    {
        this.version = new DERInteger(3);
        this.originator = originator;
        this.ukm = ukm;
        this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
        this.recipientEncryptedKeys = recipientEncryptedKeys;
    }
    
    public KeyAgreeRecipientInfo(
        ASN1Sequence seq)
    {
        int index = 0;
        
        version = (DERInteger)seq.getObjectAt(index++);
        originator = OriginatorIdentifierOrKey.getInstance(
                            (ASN1TaggedObject)seq.getObjectAt(index++), true);

        if (seq.getObjectAt(index) instanceof ASN1TaggedObject)
        {
            ukm = ASN1OctetString.getInstance(
                            (ASN1TaggedObject)seq.getObjectAt(index++), true);
        }

        keyEncryptionAlgorithm = AlgorithmIdentifier.getInstance(
                                                seq.getObjectAt(index++));

        recipientEncryptedKeys = (ASN1Sequence)seq.getObjectAt(index++);
    }
    
    /**
     * return a KeyAgreeRecipientInfo object from a tagged object.
     *
     * @param obj the tagged object holding the object we want.
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the object held by the
     *          tagged object cannot be converted.
     */
    public static KeyAgreeRecipientInfo getInstance(
        ASN1TaggedObject    obj,
        boolean             explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }
    
    /**
     * return a KeyAgreeRecipientInfo object from the given object.
     *
     * @param obj the object we want converted.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static KeyAgreeRecipientInfo getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof KeyAgreeRecipientInfo)
        {
            return (KeyAgreeRecipientInfo)obj;
        }
        
        if (obj instanceof ASN1Sequence)
        {
            return new KeyAgreeRecipientInfo((ASN1Sequence)obj);
        }
        
        throw new IllegalArgumentException(
        "Illegal object in KeyAgreeRecipientInfo: " + obj.getClass().getName());

    } 

    public DERInteger getVersion()
    {
        return version;
    }

    public OriginatorIdentifierOrKey getOriginator()
    {
        return originator;
    }

    public ASN1OctetString getUserKeyingMaterial()
    {
        return ukm;
    }

    public AlgorithmIdentifier getKeyEncryptionAlgorithm()
    {
        return keyEncryptionAlgorithm;
    }

    public ASN1Sequence getRecipientEncryptedKeys()
    {
        return recipientEncryptedKeys;
    }

    /** 
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * KeyAgreeRecipientInfo ::= SEQUENCE {
     *     version CMSVersion,  -- always set to 3
     *     originator [0] EXPLICIT OriginatorIdentifierOrKey,
     *     ukm [1] EXPLICIT UserKeyingMaterial OPTIONAL,
     *     keyEncryptionAlgorithm KeyEncryptionAlgorithmIdentifier,
     *     recipientEncryptedKeys RecipientEncryptedKeys 
     * }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(version);
        v.add(new DERTaggedObject(true, 0, originator));
        
        if (ukm != null)
        {
            v.add(new DERTaggedObject(true, 1, ukm));
        }
        
        v.add(keyEncryptionAlgorithm);
        v.add(recipientEncryptedKeys);

        return new DERSequence(v);
    }
}
