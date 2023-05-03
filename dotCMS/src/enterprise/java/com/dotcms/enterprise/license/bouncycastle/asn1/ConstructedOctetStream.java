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

import java.io.InputStream;
import java.io.IOException;

class ConstructedOctetStream
    extends InputStream
{
    private final ASN1StreamParser _parser;

    private boolean                _first = true;
    private InputStream            _currentStream;

    ConstructedOctetStream(
        ASN1StreamParser parser)
    {
        _parser = parser;
    }

    public int read(byte[] b, int off, int len) throws IOException
    {
        if (_currentStream == null)
        {
            if (!_first)
            {
                return -1;
            }

            ASN1OctetStringParser s = (ASN1OctetStringParser)_parser.readObject();

            if (s == null)
            {
                return -1;
            }

            _first = false;
            _currentStream = s.getOctetStream();
        }

        int totalRead = 0;

        for (;;)
        {
            int numRead = _currentStream.read(b, off + totalRead, len - totalRead);

            if (numRead >= 0)
            {
                totalRead += numRead;

                if (totalRead == len)
                {
                    return totalRead;
                }
            }
            else
            {
                ASN1OctetStringParser aos = (ASN1OctetStringParser)_parser.readObject();

                if (aos == null)
                {
                    _currentStream = null;
                    return totalRead < 1 ? -1 : totalRead;
                }

                _currentStream = aos.getOctetStream();
            }
        }
    }

    public int read()
        throws IOException
    {
        if (_currentStream == null)
        {
            if (!_first)
            {
                return -1;
            }

            ASN1OctetStringParser s = (ASN1OctetStringParser)_parser.readObject();
    
            if (s == null)
            {
                return -1;
            }
    
            _first = false;
            _currentStream = s.getOctetStream();
        }

        for (;;)
        {
            int b = _currentStream.read();

            if (b >= 0)
            {
                return b;
            }

            ASN1OctetStringParser s = (ASN1OctetStringParser)_parser.readObject();

            if (s == null)
            {
                _currentStream = null;
                return -1;
            }

            _currentStream = s.getOctetStream();
        }
    }
}
