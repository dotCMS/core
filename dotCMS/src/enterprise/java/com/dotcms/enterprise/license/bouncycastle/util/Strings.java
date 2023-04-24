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

package com.dotcms.enterprise.license.bouncycastle.util;

import java.io.ByteArrayOutputStream;
import java.util.Vector;

public final class Strings
{
    public static String fromUTF8ByteArray(byte[] bytes)
    {
        int i = 0;
        int length = 0;

        while (i < bytes.length)
        {
            length++;
            if ((bytes[i] & 0xf0) == 0xf0)
            {
                // surrogate pair
                length++;
                i += 4;
            }
            else if ((bytes[i] & 0xe0) == 0xe0)
            {
                i += 3;
            }
            else if ((bytes[i] & 0xc0) == 0xc0)
            {
                i += 2;
            }
            else
            {
                i += 1;
            }
        }

        char[] cs = new char[length];

        i = 0;
        length = 0;

        while (i < bytes.length)
        {
            char ch;

            if ((bytes[i] & 0xf0) == 0xf0)
            {
                int codePoint = ((bytes[i] & 0x03) << 18) | ((bytes[i+1] & 0x3F) << 12) | ((bytes[i+2] & 0x3F) << 6) | (bytes[i+3] & 0x3F);
                int U = codePoint - 0x10000;
                char W1 = (char)(0xD800 | (U >> 10));
                char W2 = (char)(0xDC00 | (U & 0x3FF));
                cs[length++] = W1;
                ch = W2;
                i += 4;
            }
            else if ((bytes[i] & 0xe0) == 0xe0)
            {
                ch = (char)(((bytes[i] & 0x0f) << 12)
                        | ((bytes[i + 1] & 0x3f) << 6) | (bytes[i + 2] & 0x3f));
                i += 3;
            }
            else if ((bytes[i] & 0xd0) == 0xd0)
            {
                ch = (char)(((bytes[i] & 0x1f) << 6) | (bytes[i + 1] & 0x3f));
                i += 2;
            }
            else if ((bytes[i] & 0xc0) == 0xc0)
            {
                ch = (char)(((bytes[i] & 0x1f) << 6) | (bytes[i + 1] & 0x3f));
                i += 2;
            }
            else
            {
                ch = (char)(bytes[i] & 0xff);
                i += 1;
            }

            cs[length++] = ch;
        }

        return new String(cs);
    }
    
    public static byte[] toUTF8ByteArray(String string)
    {
        return toUTF8ByteArray(string.toCharArray());
    }

    public static byte[] toUTF8ByteArray(char[] string)
    {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        char[] c = string;
        int i = 0;

        while (i < c.length)
        {
            char ch = c[i];

            if (ch < 0x0080)
            {
                bOut.write(ch);
            }
            else if (ch < 0x0800)
            {
                bOut.write(0xc0 | (ch >> 6));
                bOut.write(0x80 | (ch & 0x3f));
            }
            // surrogate pair
            else if (ch >= 0xD800 && ch <= 0xDFFF)
            {
                // in error - can only happen, if the Java String class has a
                // bug.
                if (i + 1 >= c.length)
                {
                    throw new IllegalStateException("invalid UTF-16 codepoint");
                }
                char W1 = ch;
                ch = c[++i];
                char W2 = ch;
                // in error - can only happen, if the Java String class has a
                // bug.
                if (W1 > 0xDBFF)
                {
                    throw new IllegalStateException("invalid UTF-16 codepoint");
                }
                int codePoint = (((W1 & 0x03FF) << 10) | (W2 & 0x03FF)) + 0x10000;
                bOut.write(0xf0 | (codePoint >> 18));
                bOut.write(0x80 | ((codePoint >> 12) & 0x3F));
                bOut.write(0x80 | ((codePoint >> 6) & 0x3F));
                bOut.write(0x80 | (codePoint & 0x3F));
            }
            else
            {
                bOut.write(0xe0 | (ch >> 12));
                bOut.write(0x80 | ((ch >> 6) & 0x3F));
                bOut.write(0x80 | (ch & 0x3F));
            }

            i++;
        }
        
        return bOut.toByteArray();
    }
    
    /**
     * A locale independent version of toUpperCase.
     * 
     * @param string input to be converted
     * @return a US Ascii uppercase version
     */
    public static String toUpperCase(String string)
    {
        boolean changed = false;
        char[] chars = string.toCharArray();
        
        for (int i = 0; i != chars.length; i++)
        {
            char ch = chars[i];
            if ('a' <= ch && 'z' >= ch)
            {
                changed = true;
                chars[i] = (char)(ch - 'a' + 'A');
            }
        }
        
        if (changed)
        {
            return new String(chars);
        }
        
        return string;
    }
    
    /**
     * A locale independent version of toLowerCase.
     * 
     * @param string input to be converted
     * @return a US ASCII lowercase version
     */
    public static String toLowerCase(String string)
    {
        boolean changed = false;
        char[] chars = string.toCharArray();
        
        for (int i = 0; i != chars.length; i++)
        {
            char ch = chars[i];
            if ('A' <= ch && 'Z' >= ch)
            {
                changed = true;
                chars[i] = (char)(ch - 'A' + 'a');
            }
        }
        
        if (changed)
        {
            return new String(chars);
        }
        
        return string;
    }

    public static byte[] toByteArray(String string)
    {
        byte[] bytes = new byte[string.length()];

        for (int i = 0; i != bytes.length; i++)
        {
            char ch = string.charAt(i);

            bytes[i] = (byte)ch;
        }

        return bytes;
    }

    public static String[] split(String input, char delimiter)
    {
        Vector           v = new Vector();
        boolean moreTokens = true;
        String subString;

        while (moreTokens)
        {
            int tokenLocation = input.indexOf(delimiter);
            if (tokenLocation > 0)
            {
                subString = input.substring(0, tokenLocation);
                v.addElement(subString);
                input = input.substring(tokenLocation + 1);
            }
            else
            {
                moreTokens = false;
                v.addElement(input);
            }
        }

        String[] res = new String[v.size()];

        for (int i = 0; i != res.length; i++)
        {
            res[i] = (String)v.elementAt(i);
        }
        return res;
    }
}
