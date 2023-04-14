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

import java.io.IOException;
import java.io.InputStream;

public class BERTaggedObjectParser
    implements ASN1TaggedObjectParser
{
    private int _baseTag;
    private int _tagNumber;
    private InputStream _contentStream;

    private boolean _indefiniteLength;

    protected BERTaggedObjectParser(
        int         baseTag,
        int         tagNumber,
        InputStream contentStream)
    {
        _baseTag = baseTag;
        _tagNumber = tagNumber;
        _contentStream = contentStream;
        _indefiniteLength = contentStream instanceof IndefiniteLengthInputStream;
    }

    public boolean isConstructed()
    {
        return (_baseTag & DERTags.CONSTRUCTED) != 0;
    }

    public int getTagNo()
    {
        return _tagNumber;
    }
    
    public DEREncodable getObjectParser(
        int     tag,
        boolean isExplicit)
        throws IOException
    {
        if (isExplicit)
        {
            return new ASN1StreamParser(_contentStream).readObject();
        }

        switch (tag)
        {
            case DERTags.SET:
                if (_indefiniteLength)
                {
                    return new BERSetParser(new ASN1StreamParser(_contentStream));
                }
                else
                {
                    return new DERSetParser(new ASN1StreamParser(_contentStream));
                }
            case DERTags.SEQUENCE:
                if (_indefiniteLength)
                {
                    return new BERSequenceParser(new ASN1StreamParser(_contentStream));
                }
                else
                {
                    return new DERSequenceParser(new ASN1StreamParser(_contentStream));
                }
            case DERTags.OCTET_STRING:
                // TODO Is the handling of definite length constructed encodings correct?
                if (_indefiniteLength || this.isConstructed())
                {
                    return new BEROctetStringParser(new ASN1StreamParser(_contentStream));
                }
                else
                {
                    return new DEROctetStringParser((DefiniteLengthInputStream)_contentStream);
                }
        }

        throw new RuntimeException("implicit tagging not implemented");
    }

    private ASN1EncodableVector rLoadVector(InputStream in)
    {
        try
        {
            return new ASN1StreamParser(in).readVector();
        }
        catch (IOException e)
        {
            throw new ASN1ParsingException(e.getMessage(), e);
        }
    }

    public DERObject getDERObject()
    {
        if (_indefiniteLength)
        {
            ASN1EncodableVector v = rLoadVector(_contentStream);

            return v.size() == 1
                ?   new BERTaggedObject(true, _tagNumber, v.get(0))
                :   new BERTaggedObject(false, _tagNumber, BERFactory.createSequence(v));
        }

        if (this.isConstructed())
        {
            ASN1EncodableVector v = rLoadVector(_contentStream);

            return v.size() == 1
                ?   new DERTaggedObject(true, _tagNumber, v.get(0))
                :   new DERTaggedObject(false, _tagNumber, DERFactory.createSequence(v));
        }

        try
        {
            DefiniteLengthInputStream defIn = (DefiniteLengthInputStream)_contentStream;
            return new DERTaggedObject(false, _tagNumber, new DEROctetString(defIn.toByteArray()));
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e.getMessage(),e);
        }
    }
}
