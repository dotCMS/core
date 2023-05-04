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

package com.dotcms.enterprise.license.bouncycastle.asn1.crmf;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

public class POPOSigningKeyInput
    extends ASN1Encodable
{
    private ASN1Encodable        authInfo;
    private SubjectPublicKeyInfo publicKey;

    private POPOSigningKeyInput(ASN1Sequence seq)
    {
        authInfo = (ASN1Encodable)seq.getObjectAt(0);
        publicKey = SubjectPublicKeyInfo.getInstance(seq.getObjectAt(1));
    }

    public static POPOSigningKeyInput getInstance(Object o)
    {
        if (o instanceof POPOSigningKeyInput)
        {
            return (POPOSigningKeyInput)o;
        }

        if (o instanceof ASN1Sequence)
        {
            return new POPOSigningKeyInput((ASN1Sequence)o);
        }

        throw new IllegalArgumentException("Invalid object: " + o.getClass().getName());
    }

    public SubjectPublicKeyInfo getPublicKey()
    {
        return publicKey;
    }

    /**
     * <pre>
     * POPOSigningKeyInput ::= SEQUENCE {
     *        authInfo             CHOICE {
     *                                 sender              [0] GeneralName,
     *                                 -- used only if an authenticated identity has been
     *                                 -- established for the sender (e.g., a DN from a
     *                                 -- previously-issued and currently-valid certificate
     *                                 publicKeyMAC        PKMACValue },
     *                                 -- used if no authenticated GeneralName currently exists for
     *                                 -- the sender; publicKeyMAC contains a password-based MAC
     *                                 -- on the DER-encoded value of publicKey
     *        publicKey           SubjectPublicKeyInfo }  -- from CertTemplate
     * </pre>
     * @return a basic ASN.1 object representation.
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(authInfo);
        v.add(publicKey);

        return new DERSequence(v);
    }
}
