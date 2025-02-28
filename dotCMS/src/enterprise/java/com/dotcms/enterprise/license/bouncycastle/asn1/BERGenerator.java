/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BERGenerator
    extends ASN1Generator
{
    private boolean      _tagged = false;
    private boolean      _isExplicit;
    private int          _tagNo;
    
    protected BERGenerator(
        OutputStream out)
    {
        super(out);
    }

    public BERGenerator(
        OutputStream out,
        int tagNo,
        boolean isExplicit) 
    {
        super(out);
        
        _tagged = true;
        _isExplicit = isExplicit;
        _tagNo = tagNo;
    }

    public OutputStream getRawOutputStream()
    {
        return _out;
    }
    
    private void writeHdr(
        int tag)
        throws IOException
    {
        _out.write(tag);
        _out.write(0x80);
    }
    
    protected void writeBERHeader(
        int tag) 
        throws IOException
    {
        if (_tagged)
        {
            int tagNum = _tagNo | DERTags.TAGGED;

            if (_isExplicit)
            {
                writeHdr(tagNum | DERTags.CONSTRUCTED);
                writeHdr(tag);
            }
            else
            {   
                if ((tag & DERTags.CONSTRUCTED) != 0)
                {
                    writeHdr(tagNum | DERTags.CONSTRUCTED);
                }
                else
                {
                    writeHdr(tagNum);
                }
            }
        }
        else
        {
            writeHdr(tag);
        }
    }
    
    protected void writeBERBody(
        InputStream contentStream)
        throws IOException
    {
        int ch;
        
        while ((ch = contentStream.read()) >= 0)
        {
            _out.write(ch);
        }
    }

    protected void writeBEREnd()
        throws IOException
    {
        _out.write(0x00);
        _out.write(0x00);
        
        if (_tagged && _isExplicit)  // write extra end for tag header
        {
            _out.write(0x00);
            _out.write(0x00);
        }
    }
}
