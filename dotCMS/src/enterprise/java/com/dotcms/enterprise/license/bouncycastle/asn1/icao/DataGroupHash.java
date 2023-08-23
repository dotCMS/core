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
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERSequence;

/**
 * The DataGroupHash object.
 * <pre>
 * DataGroupHash  ::=  SEQUENCE {
 *      dataGroupNumber         DataGroupNumber,
 *      dataGroupHashValue     OCTET STRING }
 * 
 * DataGroupNumber ::= INTEGER {
 *         dataGroup1    (1),
 *         dataGroup1    (2),
 *         dataGroup1    (3),
 *         dataGroup1    (4),
 *         dataGroup1    (5),
 *         dataGroup1    (6),
 *         dataGroup1    (7),
 *         dataGroup1    (8),
 *         dataGroup1    (9),
 *         dataGroup1    (10),
 *         dataGroup1    (11),
 *         dataGroup1    (12),
 *         dataGroup1    (13),
 *         dataGroup1    (14),
 *         dataGroup1    (15),
 *         dataGroup1    (16) }
 * 
 * </pre>
 */
public class DataGroupHash 
    extends ASN1Encodable
{
    DERInteger dataGroupNumber;    
    ASN1OctetString    dataGroupHashValue;
    
    public static DataGroupHash getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof DataGroupHash)
        {
            return (DataGroupHash)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new DataGroupHash(ASN1Sequence.getInstance(obj));            
        }
        else
        {
            throw new IllegalArgumentException("unknown object in getInstance: " + obj.getClass().getName());
        }
    }                
            
    public DataGroupHash(ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();

        // dataGroupNumber
        dataGroupNumber = DERInteger.getInstance(e.nextElement());
        // dataGroupHashValue
        dataGroupHashValue = ASN1OctetString.getInstance(e.nextElement());   
    }
    
    public DataGroupHash(
        int dataGroupNumber,        
        ASN1OctetString     dataGroupHashValue)
    {
        this.dataGroupNumber = new DERInteger(dataGroupNumber);
        this.dataGroupHashValue = dataGroupHashValue; 
    }    

    public int getDataGroupNumber()
    {
        return dataGroupNumber.getValue().intValue();
    }
    
    public ASN1OctetString getDataGroupHashValue()
    {
        return dataGroupHashValue;
    }     
    
    public DERObject toASN1Object() 
    {
        ASN1EncodableVector seq = new ASN1EncodableVector();
        seq.add(dataGroupNumber);
        seq.add(dataGroupHashValue);  

        return new DERSequence(seq);
    }
}
