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

package com.dotcms.enterprise.license.bouncycastle.util.encoders;


/**
 * a buffering class to allow translation from one format to another to
 * be done in discrete chunks.
 */
public class BufferedEncoder
{
    protected byte[]        buf;
    protected int           bufOff;

    protected Translator    translator;

    /**
     * @param translator the translator to use.
     * @param bufSize amount of input to buffer for each chunk.
     */
    public BufferedEncoder(
        Translator  translator,
        int         bufSize)
    {
        this.translator = translator;

        if ((bufSize % translator.getEncodedBlockSize()) != 0)
        {
            throw new IllegalArgumentException("buffer size not multiple of input block size");
        }

        buf = new byte[bufSize];
        bufOff = 0;
    }

    public int processByte(
        byte        in,
        byte[]      out,
        int         outOff)
    {
        int         resultLen = 0;

        buf[bufOff++] = in;

        if (bufOff == buf.length)
        {
            resultLen = translator.encode(buf, 0, buf.length, out, outOff);
            bufOff = 0;
        }

        return resultLen;
    }

    public int processBytes(
        byte[]      in,
        int         inOff,
        int         len,
        byte[]      out,
        int         outOff)
    {
        if (len < 0)
        {
            throw new IllegalArgumentException("Can't have a negative input length!");
        }

        int resultLen = 0;
        int gapLen = buf.length - bufOff;

        if (len > gapLen)
        {
            System.arraycopy(in, inOff, buf, bufOff, gapLen);

            resultLen += translator.encode(buf, 0, buf.length, out, outOff);

            bufOff = 0;

            len -= gapLen;
            inOff += gapLen;
            outOff += resultLen;

            int chunkSize = len - (len % buf.length);

            resultLen += translator.encode(in, inOff, chunkSize, out, outOff);

            len -= chunkSize;
            inOff += chunkSize;
        }

        if (len != 0)
        {
            System.arraycopy(in, inOff, buf, bufOff, len);

            bufOff += len;
        }

        return resultLen;
    }
}
