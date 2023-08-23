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
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

import java.util.Enumeration;

/**
 * Targets structure used in target information extension for attribute
 * certificates from RFC 3281.
 * 
 * <pre>
 *            Targets ::= SEQUENCE OF Target
 *           
 *            Target  ::= CHOICE {
 *              targetName          [0] GeneralName,
 *              targetGroup         [1] GeneralName,
 *              targetCert          [2] TargetCert
 *            }
 *           
 *            TargetCert  ::= SEQUENCE {
 *              targetCertificate    IssuerSerial,
 *              targetName           GeneralName OPTIONAL,
 *              certDigestInfo       ObjectDigestInfo OPTIONAL
 *            }
 * </pre>
 * 
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.x509.Target
 * @see com.dotcms.enterprise.license.bouncycastle.asn1.x509.TargetInformation
 */
public class Targets
    extends ASN1Encodable
{
    private ASN1Sequence targets;

    /**
     * Creates an instance of a Targets from the given object.
     * <p>
     * <code>obj</code> can be a Targets or a {@link ASN1Sequence}
     * 
     * @param obj The object.
     * @return A Targets instance.
     * @throws IllegalArgumentException if the given object cannot be
     *             interpreted as Target.
     */
    public static Targets getInstance(Object obj)
    {
        if (obj instanceof Targets)
        {
            return (Targets)obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new Targets((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: "
            + obj.getClass());
    }

    /**
     * Constructor from ASN1Sequence.
     * 
     * @param targets The ASN.1 SEQUENCE.
     * @throws IllegalArgumentException if the contents of the sequence are
     *             invalid.
     */
    private Targets(ASN1Sequence targets)
    {
        this.targets = targets;
    }

    /**
     * Constructor from given targets.
     * <p>
     * The vector is copied.
     * 
     * @param targets A <code>Vector</code> of {@link Target}s.
     * @see Target
     * @throws IllegalArgumentException if the vector contains not only Targets.
     */
    public Targets(Target[] targets)
    {
        this.targets = new DERSequence(targets);
    }

    /**
     * Returns the targets in a <code>Vector</code>.
     * <p>
     * The vector is cloned before it is returned.
     * 
     * @return Returns the targets.
     */
    public Target[] getTargets()
    {
        Target[] targs = new Target[targets.size()];
        int count = 0;
        for (Enumeration e = targets.getObjects(); e.hasMoreElements();)
        {
            targs[count++] = Target.getInstance(e.nextElement());
        }
        return targs;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * 
     * Returns:
     * 
     * <pre>
     *            Targets ::= SEQUENCE OF Target
     * </pre>
     * 
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        return targets;
    }
}
