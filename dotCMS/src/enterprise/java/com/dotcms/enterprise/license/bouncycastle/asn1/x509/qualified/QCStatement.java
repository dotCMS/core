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

package com.dotcms.enterprise.license.bouncycastle.asn1.x509.qualified;

import java.util.Enumeration;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

/**
 * The QCStatement object.
 * <pre>
 * QCStatement ::= SEQUENCE {
 *   statementId        OBJECT IDENTIFIER,
 *   statementInfo      ANY DEFINED BY statementId OPTIONAL} 
 * </pre>
 */

public class QCStatement 
    extends ASN1Encodable 
    implements ETSIQCObjectIdentifiers, RFC3739QCObjectIdentifiers
{
    DERObjectIdentifier qcStatementId;
    ASN1Encodable       qcStatementInfo;

    public static QCStatement getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof QCStatement)
        {
            return (QCStatement)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new QCStatement(ASN1Sequence.getInstance(obj));            
        }
        
        throw new IllegalArgumentException("unknown object in getInstance");
    }    
    
    public QCStatement(
        ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();

        // qcStatementId
        qcStatementId = DERObjectIdentifier.getInstance(e.nextElement());
        // qcstatementInfo
        if (e.hasMoreElements())
        {
            qcStatementInfo = (ASN1Encodable) e.nextElement();
        }
    }    
    
    public QCStatement(
        DERObjectIdentifier qcStatementId)
    {
        this.qcStatementId = qcStatementId;
        this.qcStatementInfo = null;
    }
    
    public QCStatement(
        DERObjectIdentifier qcStatementId, 
        ASN1Encodable       qcStatementInfo)
    {
        this.qcStatementId = qcStatementId;
        this.qcStatementInfo = qcStatementInfo;
    }    
        
    public DERObjectIdentifier getStatementId()
    {
        return qcStatementId;
    }
    
    public ASN1Encodable getStatementInfo()
    {
        return qcStatementInfo;
    }

    public DERObject toASN1Object() 
    {
        ASN1EncodableVector seq = new ASN1EncodableVector();
        seq.add(qcStatementId);       
        
        if (qcStatementInfo != null)
        {
            seq.add(qcStatementInfo);
        }

        return new DERSequence(seq);
    }
}
