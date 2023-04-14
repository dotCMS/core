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

package com.dotcms.enterprise.license.bouncycastle.asn1.misc;

import com.dotcms.enterprise.license.bouncycastle.asn1.*;

/**
 * The NetscapeCertType object.
 * <pre>
 *    NetscapeCertType ::= BIT STRING {
 *         SSLClient               (0),
 *         SSLServer               (1),
 *         S/MIME                  (2),
 *         Object Signing          (3),
 *         Reserved                (4),
 *         SSL CA                  (5),
 *         S/MIME CA               (6),
 *         Object Signing CA       (7) }
 * </pre>
 */
public class NetscapeCertType
    extends DERBitString
{
    public static final int        sslClient        = (1 << 7); 
    public static final int        sslServer        = (1 << 6);
    public static final int        smime            = (1 << 5);
    public static final int        objectSigning    = (1 << 4);
    public static final int        reserved         = (1 << 3);
    public static final int        sslCA            = (1 << 2);
    public static final int        smimeCA          = (1 << 1);
    public static final int        objectSigningCA  = (1 << 0);

    /**
     * Basic constructor.
     * 
     * @param usage - the bitwise OR of the Key Usage flags giving the
     * allowed uses for the key.
     * e.g. (X509NetscapeCertType.sslCA | X509NetscapeCertType.smimeCA)
     */
    public NetscapeCertType(
        int usage)
    {
        super(getBytes(usage), getPadBits(usage));
    }

    public NetscapeCertType(
        DERBitString usage)
    {
        super(usage.getBytes(), usage.getPadBits());
    }

    public String toString()
    {
        return "NetscapeCertType: 0x" + Integer.toHexString(data[0] & 0xff);
    }
}
