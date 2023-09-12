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
 * Target information extension for attributes certificates according to RFC
 * 3281.
 * 
 * <pre>
 *           SEQUENCE OF Targets
 * </pre>
 * 
 */
public class TargetInformation
    extends ASN1Encodable
{
    private ASN1Sequence targets;

    /**
     * Creates an instance of a TargetInformation from the given object.
     * <p>
     * <code>obj</code> can be a TargetInformation or a {@link ASN1Sequence}
     * 
     * @param obj The object.
     * @return A TargetInformation instance.
     * @throws IllegalArgumentException if the given object cannot be
     *             interpreted as TargetInformation.
     */
    public static TargetInformation getInstance(Object obj)
    {
        if (obj instanceof TargetInformation)
        {
            return (TargetInformation) obj;
        }
        else if (obj instanceof ASN1Sequence)
        {
            return new TargetInformation((ASN1Sequence) obj);
        }

        throw new IllegalArgumentException("unknown object in factory: "
            + obj.getClass());
    }

    /**
     * Constructor from a ASN1Sequence.
     * 
     * @param seq The ASN1Sequence.
     * @throws IllegalArgumentException if the sequence does not contain
     *             correctly encoded Targets elements.
     */
    private TargetInformation(ASN1Sequence seq)
    {
        targets = seq;
    }

    /**
     * Returns the targets in this target information extension.
     * 
     * @return Returns the targets.
     */
    public Targets[] getTargetsObjects()
    {
        Targets[] copy = new Targets[targets.size()];
        int count = 0;
        for (Enumeration e = targets.getObjects(); e.hasMoreElements();)
        {
            copy[count++] = Targets.getInstance(e.nextElement());
        }
        return copy;
    }

    /**
     * Constructs a target information from a single targets element. 
     * According to RFC 3281 only one targets element must be produced.
     * 
     * @param targets A Targets instance.
     */
    public TargetInformation(Targets targets)
    {
        this.targets = new DERSequence(targets);
    }

    /**
     * According to RFC 3281 only one targets element must be produced. If
     * multiple targets are given they must be merged in
     * into one targets element.
     *
     * @param targets An array with {@link Targets}.
     */
    public TargetInformation(Target[] targets)
    {
        this(new Targets(targets));
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * 
     * Returns:
     * 
     * <pre>
     *          SEQUENCE OF Targets
     * </pre>
     * 
     * <p>
     * According to RFC 3281 only one targets element must be produced. If
     * multiple targets are given in the constructor they are merged into one
     * targets element. If this was produced from a
     * {@link com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence} the encoding is kept.
     * 
     * @return a DERObject
     */
    public DERObject toASN1Object()
    {
        return targets;
    }
}
