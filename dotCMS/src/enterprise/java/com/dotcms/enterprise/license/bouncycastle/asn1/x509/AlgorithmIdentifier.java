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
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEREncodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

public class AlgorithmIdentifier
    extends ASN1Encodable
{
    private DERObjectIdentifier objectId;
    private DEREncodable        parameters;
    private boolean             parametersDefined = false;

    public static AlgorithmIdentifier getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }
    
    public static AlgorithmIdentifier getInstance(
        Object  obj)
    {
        if (obj== null || obj instanceof AlgorithmIdentifier)
        {
            return (AlgorithmIdentifier)obj;
        }
        
        if (obj instanceof DERObjectIdentifier)
        {
            return new AlgorithmIdentifier((DERObjectIdentifier)obj);
        }

        if (obj instanceof String)
        {
            return new AlgorithmIdentifier((String)obj);
        }

        if (obj instanceof ASN1Sequence)
        {
            return new AlgorithmIdentifier((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public AlgorithmIdentifier(
        DERObjectIdentifier     objectId)
    {
        this.objectId = objectId;
    }

    public AlgorithmIdentifier(
        String     objectId)
    {
        this.objectId = new DERObjectIdentifier(objectId);
    }

    public AlgorithmIdentifier(
        DERObjectIdentifier     objectId,
        DEREncodable            parameters)
    {
        parametersDefined = true;
        this.objectId = objectId;
        this.parameters = parameters;
    }

    public AlgorithmIdentifier(
        ASN1Sequence   seq)
    {
        if (seq.size() < 1 || seq.size() > 2)
        {
            throw new IllegalArgumentException("Bad sequence size: "
                    + seq.size());
        }
        
        objectId = DERObjectIdentifier.getInstance(seq.getObjectAt(0));

        if (seq.size() == 2)
        {
            parametersDefined = true;
            parameters = seq.getObjectAt(1);
        }
        else
        {
            parameters = null;
        }
    }

    public DERObjectIdentifier getObjectId()
    {
        return objectId;
    }

    public DEREncodable getParameters()
    {
        return parameters;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     *      AlgorithmIdentifier ::= SEQUENCE {
     *                            algorithm OBJECT IDENTIFIER,
     *                            parameters ANY DEFINED BY algorithm OPTIONAL }
     * </pre>
     */
    public DERObject toASN1Object()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(objectId);

        if (parametersDefined)
        {
            v.add(parameters);
        }

        return new DERSequence(v);
    }
}
