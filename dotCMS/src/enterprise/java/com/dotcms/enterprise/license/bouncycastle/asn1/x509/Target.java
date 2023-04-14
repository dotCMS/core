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

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Choice;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;

/**
 * Target structure used in target information extension for attribute
 * certificates from RFC 3281.
 * 
 * <pre>
 *     Target  ::= CHOICE {
 *       targetName          [0] GeneralName,
 *       targetGroup         [1] GeneralName,
 *       targetCert          [2] TargetCert
 *     }
 * </pre>
 * 
 * <p>
 * The targetCert field is currently not supported and must not be used
 * according to RFC 3281.
 */
public class Target
    extends ASN1Encodable
    implements ASN1Choice
{
    public static final int targetName = 0;
    public static final int targetGroup = 1;

    private GeneralName targName;
    private GeneralName targGroup;

    /**
     * Creates an instance of a Target from the given object.
     * <p>
     * <code>obj</code> can be a Target or a {@link ASN1TaggedObject}
     * 
     * @param obj The object.
     * @return A Target instance.
     * @throws IllegalArgumentException if the given object cannot be
     *             interpreted as Target.
     */
    public static Target getInstance(Object obj)
    {
        if (obj instanceof Target)
        {
            return (Target) obj;
        }
        else if (obj instanceof ASN1TaggedObject)
        {
            return new Target((ASN1TaggedObject)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: "
            + obj.getClass());
    }

    /**
     * Constructor from ASN1TaggedObject.
     * 
     * @param tagObj The tagged object.
     * @throws IllegalArgumentException if the encoding is wrong.
     */
    private Target(ASN1TaggedObject tagObj)
    {
        switch (tagObj.getTagNo())
        {
        case targetName:     // GeneralName is already a choice so explicit
            targName = GeneralName.getInstance(tagObj, true);
            break;
        case targetGroup:
            targGroup = GeneralName.getInstance(tagObj, true);
            break;
        default:
            throw new IllegalArgumentException("unknown tag: " + tagObj.getTagNo());
        }
    }

    /**
     * Constructor from given details.
     * <p>
     * Exactly one of the parameters must be not <code>null</code>.
     *
     * @param type the choice type to apply to the name.
     * @param name the general name.
     * @throws IllegalArgumentException if type is invalid.
     */
    public Target(int type, GeneralName name)
    {
        this(new DERTaggedObject(type, name));
    }

    /**
     * @return Returns the targetGroup.
     */
    public GeneralName getTargetGroup()
    {
        return targGroup;
    }

    /**
     * @return Returns the targetName.
     */
    public GeneralName getTargetName()
    {
        return targName;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * 
     * Returns:
     * 
     * <pre>
     *     Target  ::= CHOICE {
     *       targetName          [0] GeneralName,
     *       targetGroup         [1] GeneralName,
     *       targetCert          [2] TargetCert
     *     }
     * </pre>
     * 
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        // GeneralName is a choice already so most be explicitly tagged
        if (targName != null)
        {
            return new DERTaggedObject(true, 0, targName);
        }
        else
        {
            return new DERTaggedObject(true, 1, targGroup);
        }
    }
}
