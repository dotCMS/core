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

package com.dotcms.enterprise.license.bouncycastle.util.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class Streams
{
    private static int BUFFER_SIZE = 512;

    public static void drain(InputStream inStr)
        throws IOException
    {
        byte[] bs = new byte[BUFFER_SIZE];
        while (inStr.read(bs, 0, bs.length) >= 0)
        {
        }
    }

    public static byte[] readAll(InputStream inStr)
        throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        pipeAll(inStr, buf);
        return buf.toByteArray();
    }

    public static byte[] readAllLimited(InputStream inStr, int limit)
        throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        pipeAllLimited(inStr, limit, buf);
        return buf.toByteArray();
    }

    public static int readFully(InputStream inStr, byte[] buf)
        throws IOException
    {
        return readFully(inStr, buf, 0, buf.length);
    }

    public static int readFully(InputStream inStr, byte[] buf, int off, int len)
        throws IOException
    {
        int totalRead = 0;
        while (totalRead < len)
        {
            int numRead = inStr.read(buf, off + totalRead, len - totalRead);
            if (numRead < 0)
            {
                break;
            }
            totalRead += numRead;
        }
        return totalRead;
    }

    public static void pipeAll(InputStream inStr, OutputStream outStr)
        throws IOException
    {
        byte[] bs = new byte[BUFFER_SIZE];
        int numRead;
        while ((numRead = inStr.read(bs, 0, bs.length)) >= 0)
        {
            outStr.write(bs, 0, numRead);
        }
    }

    public static long pipeAllLimited(InputStream inStr, long limit, OutputStream outStr)
        throws IOException
    {
        long total = 0;
        byte[] bs = new byte[BUFFER_SIZE];
        int numRead;
        while ((numRead = inStr.read(bs, 0, bs.length)) >= 0)
        {
            total += numRead;
            if (total > limit)
            {
                throw new StreamOverflowException("Data Overflow");
            }
            outStr.write(bs, 0, numRead);
        }
        return total;
    }
}
