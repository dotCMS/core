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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Convert binary data to and from UrlBase64 encoding.  This is identical to
 * Base64 encoding, except that the padding character is "." and the other 
 * non-alphanumeric characters are "-" and "_" instead of "+" and "/".
 * <p>
 * The purpose of UrlBase64 encoding is to provide a compact encoding of binary
 * data that is safe for use as an URL parameter. Base64 encoding does not
 * produce encoded values that are safe for use in URLs, since "/" can be 
 * interpreted as a path delimiter; "+" is the encoded form of a space; and
 * "=" is used to separate a name from the corresponding value in an URL 
 * parameter.
 */
public class UrlBase64
{
    private static final Encoder encoder = new UrlBase64Encoder();
    
    /**
     * Encode the input data producing a URL safe base 64 encoded byte array.
     *
     * @return a byte array containing the URL safe base 64 encoded data.
     */
    public static byte[] encode(
        byte[]    data)
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        
        try
        {
            encoder.encode(data, 0, data.length, bOut);
        }
        catch (IOException e)
        {
            throw new RuntimeException("exception encoding URL safe base64 string: " + e);
        }
        
        return bOut.toByteArray();
    }

    /**
     * Encode the byte data writing it to the given output stream.
     *
     * @return the number of bytes produced.
     */
    public static int encode(
        byte[]                data,
        OutputStream    out)
        throws IOException
    {
        return encoder.encode(data, 0, data.length, out);
    }
    
    /**
     * Decode the URL safe base 64 encoded input data - white space will be ignored.
     *
     * @return a byte array representing the decoded data.
     */
    public static byte[] decode(
        byte[]    data)
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        
        try
        {
            encoder.decode(data, 0, data.length, bOut);
        }
        catch (IOException e)
        {
            throw new RuntimeException("exception decoding URL safe base64 string: " + e);
        }
        
        return bOut.toByteArray();
    }
    
    /**
     * decode the URL safe base 64 encoded byte data writing it to the given output stream,
     * whitespace characters will be ignored.
     *
     * @return the number of bytes produced.
     */
    public static int decode(
        byte[]                data,
        OutputStream    out)
        throws IOException
    {
        return encoder.decode(data, 0, data.length, out);
    }
    
    /**
     * decode the URL safe base 64 encoded String data - whitespace will be ignored.
     *
     * @return a byte array representing the decoded data.
     */
    public static byte[] decode(
        String    data)
    {
        ByteArrayOutputStream    bOut = new ByteArrayOutputStream();
        
        try
        {
            encoder.decode(data, bOut);
        }
        catch (IOException e)
        {
            throw new RuntimeException("exception decoding URL safe base64 string: " + e);
        }
        
        return bOut.toByteArray();
    }
    
    /**
     * Decode the URL safe base 64 encoded String data writing it to the given output stream,
     * whitespace characters will be ignored.
     *
     * @return the number of bytes produced.
     */
    public static int decode(
        String                data,
        OutputStream    out)
        throws IOException
    {
        return encoder.decode(data, out);
    }
}
