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

import java.util.Enumeration;
import java.util.Vector;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;

public class NameConstraints
    extends ASN1Encodable
{
    private ASN1Sequence permitted, excluded;

    public NameConstraints(ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();
        while (e.hasMoreElements())
        {
            ASN1TaggedObject o = ASN1TaggedObject.getInstance(e.nextElement());
            switch (o.getTagNo())
            {
            case 0:
                permitted = ASN1Sequence.getInstance(o, false);
                break;
            case 1:
                excluded = ASN1Sequence.getInstance(o, false);
                break;
            }
        }
    }

    /**
     * Constructor from a given details.
     * 
     * <p>
     * permitted and excluded are Vectors of GeneralSubtree objects.
     * 
     * @param permitted
     *            Permitted subtrees
     * @param excluded
     *            Excludes subtrees
     */
    public NameConstraints(
        Vector permitted,
        Vector excluded)
    {
        if (permitted != null)
        {
            this.permitted = createSequence(permitted);
        }
        if (excluded != null)
        {
            this.excluded = createSequence(excluded);
        }
    }

    private DERSequence createSequence(Vector subtree)
    {
        ASN1EncodableVector vec = new ASN1EncodableVector();
        Enumeration e = subtree.elements(); 
        while (e.hasMoreElements())
        {
            vec.add((GeneralSubtree)e.nextElement());
        }
        
        return new DERSequence(vec);
    }

    public ASN1Sequence getPermittedSubtrees() 
    {
        return permitted;
    }

    public ASN1Sequence getExcludedSubtrees() 
    {
        return excluded;
    }

    /*
     * NameConstraints ::= SEQUENCE { permittedSubtrees [0] GeneralSubtrees
     * OPTIONAL, excludedSubtrees [1] GeneralSubtrees OPTIONAL }
     */
    public DERObject toASN1Object() 
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        if (permitted != null) 
        {
            v.add(new DERTaggedObject(false, 0, permitted));
        }

        if (excluded != null) 
        {
            v.add(new DERTaggedObject(false, 1, excluded));
        }

        return new DERSequence(v);
    }
}
