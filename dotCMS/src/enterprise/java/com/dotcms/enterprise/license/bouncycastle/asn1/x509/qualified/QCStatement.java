/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
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
