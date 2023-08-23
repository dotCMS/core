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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1EncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Set;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEREncodableVector;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;

public class AttributeTable
{
    private Hashtable attributes = new Hashtable();

    public AttributeTable(
        Hashtable  attrs)
    {
        attributes = copyTable(attrs);
    }

    public AttributeTable(
        DEREncodableVector v)
    {
        for (int i = 0; i != v.size(); i++)
        {
            Attribute   a = Attribute.getInstance(v.get(i));

            addAttribute(a.getAttrType(), a);
        }
    }

    public AttributeTable(
        ASN1Set    s)
    {
        for (int i = 0; i != s.size(); i++)
        {
            Attribute   a = Attribute.getInstance(s.getObjectAt(i));

            addAttribute(a.getAttrType(), a);
        }
    }

    private void addAttribute(
        DERObjectIdentifier oid,
        Attribute           a)
    {
        Object value = attributes.get(oid);
        
        if (value == null)
        {
            attributes.put(oid, a);
        }
        else
        {
            Vector v;
            
            if (value instanceof Attribute)
            {
                v = new Vector();
                
                v.addElement(value);
                v.addElement(a);
            }
            else
            {
                v = (Vector)value;
            
                v.addElement(a);
            }
            
            attributes.put(oid, v);
        }
    }
    
    /**
     * Return the first attribute matching the OBJECT IDENTIFIER oid.
     * 
     * @param oid type of attribute required.
     * @return first attribute found of type oid.
     */
    public Attribute get(
        DERObjectIdentifier oid)
    {
        Object value = attributes.get(oid);
        
        if (value instanceof Vector)
        {
            return (Attribute)((Vector)value).elementAt(0);
        }
        
        return (Attribute)value;
    }

    /**
     * Return all the attributes matching the OBJECT IDENTIFIER oid. The vector will be 
     * empty if there are no attributes of the required type present.
     * 
     * @param oid type of attribute required.
     * @return a vector of all the attributes found of type oid.
     */
    public ASN1EncodableVector getAll(
        DERObjectIdentifier oid)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        
        Object value = attributes.get(oid);
        
        if (value instanceof Vector)
        {
            Enumeration e = ((Vector)value).elements();
            
            while (e.hasMoreElements())
            {
                v.add((Attribute)e.nextElement());
            }
        }
        else if (value != null)
        {
            v.add((Attribute)value);
        }
        
        return v;
    }
    
    public Hashtable toHashtable()
    {
        return copyTable(attributes);
    }
    
    public ASN1EncodableVector toASN1EncodableVector()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();
        Enumeration          e = attributes.elements();
        
        while (e.hasMoreElements())
        {
            Object value = e.nextElement();
            
            if (value instanceof Vector)
            {
                Enumeration en = ((Vector)value).elements();
                
                while (en.hasMoreElements())
                {
                    v.add(Attribute.getInstance(en.nextElement()));
                }
            }
            else
            {
                v.add(Attribute.getInstance(value));
            }
        }
        
        return v;
    }
    
    private Hashtable copyTable(
        Hashtable in)
    {
        Hashtable   out = new Hashtable();
        Enumeration e = in.keys();
        
        while (e.hasMoreElements())
        {
            Object key = e.nextElement();
            
            out.put(key, in.get(key));
        }
        
        return out;
    }
}
