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

package com.dotcms.enterprise.license.bouncycastle.asn1.icao;

import java.util.Enumeration;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * The LDSSecurityObject object.
 * <pre>
 * LDSSecurityObject ::= SEQUENCE {
 *   version                LDSSecurityObjectVersion,
 *   hashAlgorithm          DigestAlgorithmIdentifier,
 *   dataGroupHashValues    SEQUENCE SIZE (2..ub-DataGroups) OF DataHashGroup}
 *   
 * DigestAlgorithmIdentifier ::= AlgorithmIdentifier,
 * 
 * LDSSecurityObjectVersion :: INTEGER {V0(0)}
 * </pre>
 */

public class LDSSecurityObject 
    extends ASN1Encodable 
    implements ICAOObjectIdentifiers    
{
    
    public static final int ub_DataGroups = 16;
    
    DERInteger version = new DERInteger(0);
    AlgorithmIdentifier digestAlgorithmIdentifier; 
    DataGroupHash[] datagroupHash;            

    public static LDSSecurityObject getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof LDSSecurityObject)
        {
            return (LDSSecurityObject)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new LDSSecurityObject(ASN1Sequence.getInstance(obj));            
        }
        
        throw new IllegalArgumentException("unknown object in getInstance: " + obj.getClass().getName());
    }    
    
    public LDSSecurityObject(
        ASN1Sequence seq)
    {
        if (seq == null || seq.size() == 0)
        {
            throw new IllegalArgumentException("null or empty sequence passed.");
        }
        
        Enumeration e = seq.getObjects();

        // version
        version = DERInteger.getInstance(e.nextElement());
        // digestAlgorithmIdentifier
        digestAlgorithmIdentifier = AlgorithmIdentifier.getInstance(e.nextElement());
      
        ASN1Sequence datagroupHashSeq = ASN1Sequence.getInstance(e.nextElement());

        checkDatagroupHashSeqSize(datagroupHashSeq.size());        
        
        datagroupHash = new DataGroupHash[datagroupHashSeq.size()];
        for (int i= 0; i< datagroupHashSeq.size();i++)
        {
            datagroupHash[i] = DataGroupHash.getInstance(datagroupHashSeq.getObjectAt(i));
        } 
        
    }

    public LDSSecurityObject(
        AlgorithmIdentifier digestAlgorithmIdentifier, 
        DataGroupHash[]       datagroupHash)
    {
        this.digestAlgorithmIdentifier = digestAlgorithmIdentifier;
        this.datagroupHash = datagroupHash;
        
        checkDatagroupHashSeqSize(datagroupHash.length);                      
    }    
        
    private void checkDatagroupHashSeqSize(int size)
    {
        if ((size < 2) || (size > ub_DataGroups))
        {
               throw new IllegalArgumentException("wrong size in DataGroupHashValues : not in (2.."+ ub_DataGroups +")");
        }
    }  
    
    public AlgorithmIdentifier getDigestAlgorithmIdentifier()
    {
        return digestAlgorithmIdentifier;
    }
    
    public DataGroupHash[] getDatagroupHash()
    {
        return datagroupHash;
    }

    public DERObject toASN1Object() 
    {
        ASN1EncodableVector seq = new ASN1EncodableVector();
        
        seq.add(version);
        seq.add(digestAlgorithmIdentifier);
                
        ASN1EncodableVector seqname = new ASN1EncodableVector();
        for (int i = 0; i < datagroupHash.length; i++) 
        {
            seqname.add(datagroupHash[i]);
        }            
        seq.add(new DERSequence(seqname));                   
        
        return new DERSequence(seq);
    }          
}
