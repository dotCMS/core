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
import com.dotcms.enterprise.license.bouncycastle.asn1.DEREncodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;

public class RecipientInfo
    extends ASN1Encodable
    implements ASN1Choice
{
    DEREncodable    info;

    public RecipientInfo(
        KeyTransRecipientInfo info)
    {
        this.info = info;
    }

    public RecipientInfo(
        KeyAgreeRecipientInfo info)
    {
        this.info = new DERTaggedObject(false, 1, info);
    }

    public RecipientInfo(
        KEKRecipientInfo info)
    {
        this.info = new DERTaggedObject(false, 2, info);
    }

    public RecipientInfo(
        PasswordRecipientInfo info)
    {
        this.info = new DERTaggedObject(false, 3, info);
    }

    public RecipientInfo(
        OtherRecipientInfo info)
    {
        this.info = new DERTaggedObject(false, 4, info);
    }

    public RecipientInfo(
        DERObject   info)
    {
        this.info = info;
    }

    public static RecipientInfo getInstance(
        Object  o)
    {
        if (o == null || o instanceof RecipientInfo)
        {
            return (RecipientInfo)o;
        }
        else if (o instanceof ASN1Sequence)
        {
            return new RecipientInfo((ASN1Sequence)o);
        }
        else if (o instanceof ASN1TaggedObject)
        {
            return new RecipientInfo((ASN1TaggedObject)o);
        }

        throw new IllegalArgumentException("unknown object in factory: "
                                                    + o.getClass().getName());
    }

    public DERInteger getVersion()
    {
        if (info instanceof ASN1TaggedObject)
        {
            ASN1TaggedObject o = (ASN1TaggedObject)info;

            switch (o.getTagNo())
            {
            case 1:
                return KeyAgreeRecipientInfo.getInstance(o, false).getVersion();
            case 2:
                return getKEKInfo(o).getVersion();
            case 3:
                return PasswordRecipientInfo.getInstance(o, false).getVersion();
            case 4:
                return new DERInteger(0);    // no syntax version for OtherRecipientInfo
            default:
                throw new IllegalStateException("unknown tag");
            }
        }

        return KeyTransRecipientInfo.getInstance(info).getVersion();
    }

    public boolean isTagged()
    {
        return (info instanceof ASN1TaggedObject);
    }

    public DEREncodable getInfo()
    {
        if (info instanceof ASN1TaggedObject)
        {
            ASN1TaggedObject o = (ASN1TaggedObject)info;

            switch (o.getTagNo())
            {
            case 1:
                return KeyAgreeRecipientInfo.getInstance(o, false);
            case 2:
                return getKEKInfo(o);
            case 3:
                return PasswordRecipientInfo.getInstance(o, false);
            case 4:
                return OtherRecipientInfo.getInstance(o, false);
            default:
                throw new IllegalStateException("unknown tag");
            }
        }

        return KeyTransRecipientInfo.getInstance(info);
    }

    private KEKRecipientInfo getKEKInfo(ASN1TaggedObject o)
    {
        if (o.isExplicit())
        {                        // compatibilty with erroneous version
            return KEKRecipientInfo.getInstance(o, true);
        }
        else
        {
            return KEKRecipientInfo.getInstance(o, false);
        }
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * RecipientInfo ::= CHOICE {
     *     ktri KeyTransRecipientInfo,
     *     kari [1] KeyAgreeRecipientInfo,
     *     kekri [2] KEKRecipientInfo,
     *     pwri [3] PasswordRecipientInfo,
     *     ori [4] OtherRecipientInfo }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        return info.getDERObject();
    }
}
