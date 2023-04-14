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
 * Converters for going from hex to binary and back. Note: this class assumes ASCII processing.
 */
public class HexTranslator
    implements Translator
{
    private static final byte[]   hexTable = 
        { 
            (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5', (byte)'6', (byte)'7',
            (byte)'8', (byte)'9', (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f'
        };

    /**
     * size of the output block on encoding produced by getDecodedBlockSize()
     * bytes.
     */
    public int getEncodedBlockSize()
    {
        return 2;
    }

    public int encode(
        byte[]  in,
        int     inOff,
        int     length,
        byte[]  out,
        int     outOff)
    {
        for (int i = 0, j = 0; i < length; i++, j += 2)
        {
            out[outOff + j] = hexTable[(in[inOff] >> 4) & 0x0f];
            out[outOff + j + 1] = hexTable[in[inOff] & 0x0f];

            inOff++;
        }

        return length * 2;
    }

    /**
     * size of the output block on decoding produced by getEncodedBlockSize()
     * bytes.
     */
    public int getDecodedBlockSize()
    {
        return 1;
    }

    public int decode(
        byte[]  in,
        int     inOff,
        int     length,
        byte[]  out,
        int     outOff)
    {
        int halfLength = length / 2;
        byte left, right;
        for (int i = 0; i < halfLength; i++)
        {
            left  = in[inOff + i * 2];
            right = in[inOff + i * 2 + 1];
            
            if (left < (byte)'a')
            {
                out[outOff] = (byte)((left - '0') << 4);
            }
            else
            {
                out[outOff] = (byte)((left - 'a' + 10) << 4);
            }
            if (right < (byte)'a')
            {
                out[outOff] += (byte)(right - '0');
            }
            else
            {
                out[outOff] += (byte)(right - 'a' + 10);
            }

            outOff++;
        }

        return halfLength;
    }
}
