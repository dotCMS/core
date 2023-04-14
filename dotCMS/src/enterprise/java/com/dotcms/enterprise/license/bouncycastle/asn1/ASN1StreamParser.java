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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ASN1StreamParser
{
    private final InputStream _in;
    private final int         _limit;

    private static int findLimit(InputStream in)
    {
        if (in instanceof DefiniteLengthInputStream)
        {
            return ((DefiniteLengthInputStream)in).getRemaining();
        }

        return Integer.MAX_VALUE;
    }

    public ASN1StreamParser(
        InputStream in)
    {
        this(in, findLimit(in));
    }

    public ASN1StreamParser(
        InputStream in,
        int         limit)
    {
        this._in = in;
        this._limit = limit;
    }

    public ASN1StreamParser(
        byte[] encoding)
    {
        this(new ByteArrayInputStream(encoding), encoding.length);
    }

    public DEREncodable readObject()
        throws IOException
    {
        int tag = _in.read();
        if (tag == -1)
        {
            return null;
        }

        //
        // turn of looking for "00" while we resolve the tag
        //
        set00Check(false);

        //
        // calculate tag number
        //
        int tagNo = ASN1InputStream.readTagNumber(_in, tag);

        boolean isConstructed = (tag & DERTags.CONSTRUCTED) != 0;

        //
        // calculate length
        //
        int length = ASN1InputStream.readLength(_in, _limit);

        if (length < 0) // indefinite length method
        {
            if (!isConstructed)
            {
                throw new IOException("indefinite length primitive encoding encountered");
            }

            IndefiniteLengthInputStream indIn = new IndefiniteLengthInputStream(_in);

            if ((tag & DERTags.APPLICATION) != 0)
            {
                ASN1StreamParser sp = new ASN1StreamParser(indIn, _limit);

                return new BERApplicationSpecificParser(tagNo, sp);
            }

            if ((tag & DERTags.TAGGED) != 0)
            {
                return new BERTaggedObjectParser(tag, tagNo, indIn);
            }

            ASN1StreamParser sp = new ASN1StreamParser(indIn, _limit);

            // TODO There are other tags that may be constructed (e.g. BIT_STRING)
            switch (tagNo)
            {
                case DERTags.OCTET_STRING:
                    return new BEROctetStringParser(sp);
                case DERTags.SEQUENCE:
                    return new BERSequenceParser(sp);
                case DERTags.SET:
                    return new BERSetParser(sp);
                case DERTags.EXTERNAL:{
                    return new DERExternalParser(sp);
                }
                default:
                    throw new IOException("unknown BER object encountered: 0x" + Integer.toHexString(tagNo));
            }
        }
        else
        {
            DefiniteLengthInputStream defIn = new DefiniteLengthInputStream(_in, length);

            if ((tag & DERTags.APPLICATION) != 0)
            {
                return new DERApplicationSpecific(isConstructed, tagNo, defIn.toByteArray());
            }

            if ((tag & DERTags.TAGGED) != 0)
            {
                return new BERTaggedObjectParser(tag, tagNo, defIn);
            }

            if (isConstructed)
            {
                // TODO There are other tags that may be constructed (e.g. BIT_STRING)
                switch (tagNo)
                {
                    case DERTags.OCTET_STRING:
                        //
                        // yes, people actually do this...
                        //
                        return new BEROctetStringParser(new ASN1StreamParser(defIn));
                    case DERTags.SEQUENCE:
                        return new DERSequenceParser(new ASN1StreamParser(defIn));
                    case DERTags.SET:
                        return new DERSetParser(new ASN1StreamParser(defIn));
                    case DERTags.EXTERNAL:
                        return new DERExternalParser(new ASN1StreamParser(defIn));
                    default:
                        // TODO Add DERUnknownTagParser class?
                        return new DERUnknownTag(true, tagNo, defIn.toByteArray());
                }
            }

            // Some primitive encodings can be handled by parsers too...
            switch (tagNo)
            {
                case DERTags.OCTET_STRING:
                    return new DEROctetStringParser(defIn);
            }

            return ASN1InputStream.createPrimitiveDERObject(tagNo, defIn.toByteArray());
        }
    }

    private void set00Check(boolean enabled)
    {
        if (_in instanceof IndefiniteLengthInputStream)
        {
            ((IndefiniteLengthInputStream)_in).setEofOn00(enabled);
        }
    }

    ASN1EncodableVector readVector() throws IOException
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        DEREncodable obj;
        while ((obj = readObject()) != null)
        {
            v.add(obj.getDERObject());
        }

        return v;
    }
}
