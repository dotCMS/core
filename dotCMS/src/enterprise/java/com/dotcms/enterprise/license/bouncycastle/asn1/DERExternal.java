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

package com.dotcms.enterprise.license.bouncycastle.asn1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Class representing the DER-type External
 */
public class DERExternal
    extends ASN1Object
{
    private DERObjectIdentifier directReference;
    private DERInteger indirectReference;
    private ASN1Object dataValueDescriptor;
    private int encoding;
    private DERObject externalContent;
    
    public DERExternal(ASN1EncodableVector vector)
    {
        int offset = 0;
        DERObject enc = vector.get(offset).getDERObject();
        if (enc instanceof DERObjectIdentifier)
        {
            directReference = (DERObjectIdentifier)enc;
            offset++;
            enc = vector.get(offset).getDERObject();
        }
        if (enc instanceof DERInteger)
        {
            indirectReference = (DERInteger) enc;
            offset++;
            enc = vector.get(offset).getDERObject();
        }
        if (!(enc instanceof DERTaggedObject))
        {
            dataValueDescriptor = (ASN1Object) enc;
            offset++;
            enc = vector.get(offset).getDERObject();
        }
        if (!(enc instanceof DERTaggedObject))
        {
            throw new IllegalArgumentException("No tagged object found in vector. Structure doesn't seem to be of type External");
        }
        DERTaggedObject obj = (DERTaggedObject)enc;
        setEncoding(obj.getTagNo());
        externalContent = obj.getObject();
    }

    /**
     * Creates a new instance of DERExternal
     * See X.690 for more informations about the meaning of these parameters
     * @param directReference The direct reference or <code>null</code> if not set.
     * @param indirectReference The indirect reference or <code>null</code> if not set.
     * @param dataValueDescriptor The data value descriptor or <code>null</code> if not set.
     * @param externalData The external data in its encoded form.
     */
    public DERExternal(DERObjectIdentifier directReference, DERInteger indirectReference, ASN1Object dataValueDescriptor, DERTaggedObject externalData)
    {
        this(directReference, indirectReference, dataValueDescriptor, externalData.getTagNo(), externalData.getDERObject());
    }

    /**
     * Creates a new instance of DERExternal.
     * See X.690 for more informations about the meaning of these parameters
     * @param directReference The direct reference or <code>null</code> if not set.
     * @param indirectReference The indirect reference or <code>null</code> if not set.
     * @param dataValueDescriptor The data value descriptor or <code>null</code> if not set.
     * @param encoding The encoding to be used for the external data
     * @param externalData The external data
     */
    public DERExternal(DERObjectIdentifier directReference, DERInteger indirectReference, ASN1Object dataValueDescriptor, int encoding, DERObject externalData)
    {
        setDirectReference(directReference);
        setIndirectReference(indirectReference);
        setDataValueDescriptor(dataValueDescriptor);
        setEncoding(encoding);
        setExternalContent(externalData.getDERObject());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        int ret = 0;
        if (directReference != null)
        {
            ret = directReference.hashCode();
        }
        if (indirectReference != null)
        {
            ret ^= indirectReference.hashCode();
        }
        if (dataValueDescriptor != null)
        {
            ret ^= dataValueDescriptor.hashCode();
        }
        ret ^= externalContent.hashCode();
        return ret;
    }

    /* (non-Javadoc)
     * @see org.bouncycastle.asn1.DERObject#encode(org.bouncycastle.asn1.DEROutputStream)
     */
    void encode(DEROutputStream out)
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (directReference != null)
        {
            baos.write(directReference.getDEREncoded());
        }
        if (indirectReference != null)
        {
            baos.write(indirectReference.getDEREncoded());
        }
        if (dataValueDescriptor != null)
        {
            baos.write(dataValueDescriptor.getDEREncoded());
        }
        DERTaggedObject obj = new DERTaggedObject(encoding, externalContent);
        baos.write(obj.getDEREncoded());
        out.writeEncoded(DERTags.CONSTRUCTED, DERTags.EXTERNAL, baos.toByteArray());
    }

    /* (non-Javadoc)
     * @see org.bouncycastle.asn1.ASN1Object#asn1Equals(org.bouncycastle.asn1.DERObject)
     */
    boolean asn1Equals(DERObject o)
    {
        if (!(o instanceof DERExternal))
        {
            return false;
        }
        if (this == o)
        {
            return true;
        }
        DERExternal other = (DERExternal)o;
        if (directReference != null)
        {
            if (other.directReference == null || !other.directReference.equals(directReference))  
            {
                return false;
            }
        }
        if (indirectReference != null)
        {
            if (other.indirectReference == null || !other.indirectReference.equals(indirectReference))
            {
                return false;
            }
        }
        if (dataValueDescriptor != null)
        {
            if (other.dataValueDescriptor == null || !other.dataValueDescriptor.equals(dataValueDescriptor))
            {
                return false;
            }
        }
        return externalContent.equals(other.externalContent);
    }

    /**
     * Returns the data value descriptor
     * @return The descriptor
     */
    public ASN1Object getDataValueDescriptor()
    {
        return dataValueDescriptor;
    }

    /**
     * Returns the direct reference of the external element
     * @return The reference
     */
    public DERObjectIdentifier getDirectReference()
    {
        return directReference;
    }

    /**
     * Returns the encoding of the content. Valid values are
     * <ul>
     * <li><code>0</code> single-ASN1-type</li>
     * <li><code>1</code> OCTET STRING</li>
     * <li><code>2</code> BIT STRING</li>
     * </ul>
     * @return The encoding
     */
    public int getEncoding()
    {
        return encoding;
    }
    
    /**
     * Returns the content of this element
     * @return The content
     */
    public DERObject getExternalContent()
    {
        return externalContent;
    }
    
    /**
     * Returns the indirect reference of this element
     * @return The reference
     */
    public DERInteger getIndirectReference()
    {
        return indirectReference;
    }
    
    /**
     * Sets the data value descriptor
     * @param dataValueDescriptor The descriptor
     */
    private void setDataValueDescriptor(ASN1Object dataValueDescriptor)
    {
        this.dataValueDescriptor = dataValueDescriptor;
    }

    /**
     * Sets the direct reference of the external element
     * @param directReferemce The reference
     */
    private void setDirectReference(DERObjectIdentifier directReferemce)
    {
        this.directReference = directReferemce;
    }
    
    /**
     * Sets the encoding of the content. Valid values are
     * <ul>
     * <li><code>0</code> single-ASN1-type</li>
     * <li><code>1</code> OCTET STRING</li>
     * <li><code>2</code> BIT STRING</li>
     * </ul>
     * @param encoding The encoding
     */
    private void setEncoding(int encoding)
    {
        if (encoding < 0 || encoding > 2)
        {
            throw new IllegalArgumentException("invalid encoding value: " + encoding);
        }
        this.encoding = encoding;
    }
    
    /**
     * Sets the content of this element
     * @param externalContent The content
     */
    private void setExternalContent(DERObject externalContent)
    {
        this.externalContent = externalContent;
    }
    
    /**
     * Sets the indirect reference of this element
     * @param indirectReference The reference
     */
    private void setIndirectReference(DERInteger indirectReference)
    {
        this.indirectReference = indirectReference;
    }
}
