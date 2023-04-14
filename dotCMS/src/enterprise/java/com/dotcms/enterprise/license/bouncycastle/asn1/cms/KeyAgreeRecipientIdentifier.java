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

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Choice;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;

public class KeyAgreeRecipientIdentifier
    extends ASN1Encodable
    implements ASN1Choice
{
    private IssuerAndSerialNumber issuerSerial;
    private RecipientKeyIdentifier rKeyID;

    /**
     * return an KeyAgreeRecipientIdentifier object from a tagged object.
     *
     * @param obj the tagged object holding the object we want.
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the object held by the
     *          tagged object cannot be converted.
     */
    public static KeyAgreeRecipientIdentifier getInstance(
        ASN1TaggedObject    obj,
        boolean             explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }
    
    /**
     * return an KeyAgreeRecipientIdentifier object from the given object.
     *
     * @param obj the object we want converted.
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static KeyAgreeRecipientIdentifier getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof KeyAgreeRecipientIdentifier)
        {
            return (KeyAgreeRecipientIdentifier)obj;
        }
        
        if (obj instanceof ASN1Sequence)
        {
            return new KeyAgreeRecipientIdentifier(IssuerAndSerialNumber.getInstance(obj));
        }
        
        if (obj instanceof ASN1TaggedObject && ((ASN1TaggedObject)obj).getTagNo() == 0)
        {
            return new KeyAgreeRecipientIdentifier(RecipientKeyIdentifier.getInstance(
                (ASN1TaggedObject)obj, false));
        }
        
        throw new IllegalArgumentException("Invalid KeyAgreeRecipientIdentifier: " + obj.getClass().getName());
    } 

    public KeyAgreeRecipientIdentifier(
        IssuerAndSerialNumber issuerSerial)
    {
        this.issuerSerial = issuerSerial;
        this.rKeyID = null;
    }

    public KeyAgreeRecipientIdentifier(
         RecipientKeyIdentifier rKeyID)
    {
        this.issuerSerial = null;
        this.rKeyID = rKeyID;
    }

    public IssuerAndSerialNumber getIssuerAndSerialNumber()
    {
        return issuerSerial;
    }

    public RecipientKeyIdentifier getRKeyID()
    {
        return rKeyID;
    }

    /** 
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * KeyAgreeRecipientIdentifier ::= CHOICE {
     *     issuerAndSerialNumber IssuerAndSerialNumber,
     *     rKeyId [0] IMPLICIT RecipientKeyIdentifier
     * }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        if (issuerSerial != null)
        {
            return issuerSerial.toASN1Object();
        }

        return new DERTaggedObject(false, 0, rKeyID);
    }
}
