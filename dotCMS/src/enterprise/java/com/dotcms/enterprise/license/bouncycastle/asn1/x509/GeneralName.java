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

package com.dotcms.enterprise.license.bouncycastle.asn1.x509;

import java.util.StringTokenizer;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Choice;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Encodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1OctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1Sequence;
import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1TaggedObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEREncodable;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERIA5String;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObjectIdentifier;
import com.dotcms.enterprise.license.bouncycastle.asn1.DEROctetString;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERTaggedObject;
import com.dotcms.enterprise.license.bouncycastle.util.IPAddress;

/**
 * The GeneralName object.
 * <pre>
 * GeneralName ::= CHOICE {
 *      otherName                       [0]     OtherName,
 *      rfc822Name                      [1]     IA5String,
 *      dNSName                         [2]     IA5String,
 *      x400Address                     [3]     ORAddress,
 *      directoryName                   [4]     Name,
 *      ediPartyName                    [5]     EDIPartyName,
 *      uniformResourceIdentifier       [6]     IA5String,
 *      iPAddress                       [7]     OCTET STRING,
 *      registeredID                    [8]     OBJECT IDENTIFIER}
 *
 * OtherName ::= SEQUENCE {
 *      type-id    OBJECT IDENTIFIER,
 *      value      [0] EXPLICIT ANY DEFINED BY type-id }
 *
 * EDIPartyName ::= SEQUENCE {
 *      nameAssigner            [0]     DirectoryString OPTIONAL,
 *      partyName               [1]     DirectoryString }
 * 
 * Name ::= CHOICE { RDNSequence }
 * </pre>
 */
public class GeneralName
    extends ASN1Encodable
    implements ASN1Choice
{
    public static final int otherName                     = 0;
    public static final int rfc822Name                    = 1;
    public static final int dNSName                       = 2;
    public static final int x400Address                   = 3;
    public static final int directoryName                 = 4;
    public static final int ediPartyName                  = 5;
    public static final int uniformResourceIdentifier     = 6;
    public static final int iPAddress                     = 7;
    public static final int registeredID                  = 8;

    DEREncodable      obj;
    int               tag;
   
    public GeneralName(
        X509Name  dirName)
    {
        this.obj = dirName;
        this.tag = 4;
    }

    /**
     * @deprecated this constructor seems the wrong way round! Use GeneralName(tag, name).
     */
    public GeneralName(
        DERObject name, int tag)
    {
        this.obj = name;
        this.tag = tag;
    }

    /**
     * When the subjectAltName extension contains an Internet mail address,
     * the address MUST be included as an rfc822Name. The format of an
     * rfc822Name is an "addr-spec" as defined in RFC 822 [RFC 822].
     *
     * When the subjectAltName extension contains a domain name service
     * label, the domain name MUST be stored in the dNSName (an IA5String).
     * The name MUST be in the "preferred name syntax," as specified by RFC
     * 1034 [RFC 1034].
     *
     * When the subjectAltName extension contains a URI, the name MUST be
     * stored in the uniformResourceIdentifier (an IA5String). The name MUST
     * be a non-relative URL, and MUST follow the URL syntax and encoding
     * rules specified in [RFC 1738].  The name must include both a scheme
     * (e.g., "http" or "ftp") and a scheme-specific-part.  The scheme-
     * specific-part must include a fully qualified domain name or IP
     * address as the host.
     *
     * When the subjectAltName extension contains a iPAddress, the address
     * MUST be stored in the octet string in "network byte order," as
     * specified in RFC 791 [RFC 791]. The least significant bit (LSB) of
     * each octet is the LSB of the corresponding byte in the network
     * address. For IP Version 4, as specified in RFC 791, the octet string
     * MUST contain exactly four octets.  For IP Version 6, as specified in
     * RFC 1883, the octet string MUST contain exactly sixteen octets [RFC
     * 1883].
     */
    public GeneralName(
        int           tag,
        ASN1Encodable name)
    {
        this.obj = name;
        this.tag = tag;
    }
    
    /**
     * Create a GeneralName for the given tag from the passed in String.
     * <p>
     * This constructor can handle:
     * <ul>
     * <li>rfc822Name
     * <li>iPAddress
     * <li>directoryName
     * <li>dNSName
     * <li>uniformResourceIdentifier
     * <li>registeredID
     * </ul>
     * For x400Address, otherName and ediPartyName there is no common string
     * format defined.
     * <p>
     * Note: A directory name can be encoded in different ways into a byte
     * representation. Be aware of this if the byte representation is used for
     * comparing results.
     *
     * @param tag tag number
     * @param name string representation of name
     * @throws IllegalArgumentException if the string encoding is not correct or     *             not supported.
     */
    public GeneralName(
        int       tag,
        String    name)
    {
        this.tag = tag;

        if (tag == rfc822Name || tag == dNSName || tag == uniformResourceIdentifier)
        {
            this.obj = new DERIA5String(name);
        }
        else if (tag == registeredID)
        {
            this.obj = new DERObjectIdentifier(name);
        }
        else if (tag == directoryName)
        {
            this.obj = new X509Name(name);
        }
        else if (tag == iPAddress)
        {
            byte[] enc = toGeneralNameEncoding(name);
            if (enc != null)
            {
                this.obj = new DEROctetString(enc);
            }
            else
            {
                throw new IllegalArgumentException("IP Address is invalid");
            }
        }
        else
        {
            throw new IllegalArgumentException("can't process String for tag: " + tag);
        }
    }
    
    public static GeneralName getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof GeneralName)
        {
            return (GeneralName)obj;
        }

        if (obj instanceof ASN1TaggedObject)
        {
            ASN1TaggedObject    tagObj = (ASN1TaggedObject)obj;
            int                 tag = tagObj.getTagNo();

            switch (tag)
            {
            case otherName:
                return new GeneralName(tag, ASN1Sequence.getInstance(tagObj, false));
            case rfc822Name:
                return new GeneralName(tag, DERIA5String.getInstance(tagObj, false));
            case dNSName:
                return new GeneralName(tag, DERIA5String.getInstance(tagObj, false));
            case x400Address:
                throw new IllegalArgumentException("unknown tag: " + tag);
            case directoryName:
                return new GeneralName(tag, X509Name.getInstance(tagObj, true));
            case ediPartyName:
                return new GeneralName(tag, ASN1Sequence.getInstance(tagObj, false));
            case uniformResourceIdentifier:
                return new GeneralName(tag, DERIA5String.getInstance(tagObj, false));
            case iPAddress:
                return new GeneralName(tag, ASN1OctetString.getInstance(tagObj, false));
            case registeredID:
                return new GeneralName(tag, DERObjectIdentifier.getInstance(tagObj, false));
            }
        }

        throw new IllegalArgumentException("unknown object in getInstance: " + obj.getClass().getName());
    }

    public static GeneralName getInstance(
        ASN1TaggedObject tagObj,
        boolean          explicit)
    {
        return GeneralName.getInstance(ASN1TaggedObject.getInstance(tagObj, true));
    }

    public int getTagNo()
    {
        return tag;
    }

    public DEREncodable getName()
    {
        return obj;
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();

        buf.append(tag);
        buf.append(": ");
        switch (tag)
        {
        case rfc822Name:
        case dNSName:
        case uniformResourceIdentifier:
            buf.append(DERIA5String.getInstance(obj).getString());
            break;
        case directoryName:
            buf.append(X509Name.getInstance(obj).toString());
            break;
        default:
            buf.append(obj.toString());
        }
        return buf.toString();
    }

    private byte[] toGeneralNameEncoding(String ip)
    {
        if (IPAddress.isValidIPv6WithNetmask(ip) || IPAddress.isValidIPv6(ip))
        {
            int    slashIndex = ip.indexOf('/');

            if (slashIndex < 0)
            {
                byte[] addr = new byte[16];
                int[]  parsedIp = parseIPv6(ip);
                copyInts(parsedIp, addr, 0);

                return addr;
            }
            else
            {
                byte[] addr = new byte[32];
                int[]  parsedIp = parseIPv6(ip.substring(0, slashIndex));
                copyInts(parsedIp, addr, 0);
                String mask = ip.substring(slashIndex + 1);
                if (mask.indexOf(':') > 0)
                {
                    parsedIp = parseIPv6(mask);
                }
                else
                {
                    parsedIp = parseMask(mask);
                }
                copyInts(parsedIp, addr, 16);

                return addr;
            }
        }
        else if (IPAddress.isValidIPv4WithNetmask(ip) || IPAddress.isValidIPv4(ip))
        {
            int    slashIndex = ip.indexOf('/');

            if (slashIndex < 0)
            {
                byte[] addr = new byte[4];

                parseIPv4(ip, addr, 0);

                return addr;
            }
            else
            {
                byte[] addr = new byte[8];

                parseIPv4(ip.substring(0, slashIndex), addr, 0);

                String mask = ip.substring(slashIndex + 1);
                if (mask.indexOf('.') > 0)
                {
                    parseIPv4(mask, addr, 4);
                }
                else
                {
                    parseIPv4Mask(mask, addr, 4);
                }

                return addr;
            }
        }

        return null;
    }

    private void parseIPv4Mask(String mask, byte[] addr, int offset)
    {
        int   maskVal = Integer.parseInt(mask);

        for (int i = 0; i != maskVal; i++)
        {
            addr[(i / 8) + offset] |= 1 << (i % 8);
        }
    }

    private void parseIPv4(String ip, byte[] addr, int offset)
    {
        StringTokenizer sTok = new StringTokenizer(ip, "./");
        int    index = 0;

        while (sTok.hasMoreTokens())
        {
            addr[offset + index++] = (byte)Integer.parseInt(sTok.nextToken());
        }
    }

    private int[] parseMask(String mask)
    {
        int[] res = new int[8];
        int   maskVal = Integer.parseInt(mask);

        for (int i = 0; i != maskVal; i++)
        {
            res[i / 16] |= 1 << (i % 16);
        }
        return res;
    }

    private void copyInts(int[] parsedIp, byte[] addr, int offSet)
    {
        for (int i = 0; i != parsedIp.length; i++)
        {
            addr[(i * 2) + offSet] = (byte)(parsedIp[i] >> 8);
            addr[(i * 2 + 1) + offSet] = (byte)parsedIp[i];
        }
    }

    private int[] parseIPv6(String ip)
    {
        StringTokenizer sTok = new StringTokenizer(ip, ":", true);
        int index = 0;
        int[] val = new int[8];

        if (ip.charAt(0) == ':' && ip.charAt(1) == ':')
        {
           sTok.nextToken(); // skip the first one
        }

        int doubleColon = -1;

        while (sTok.hasMoreTokens())
        {
            String e = sTok.nextToken();

            if (e.equals(":"))
            {
                doubleColon = index;
                val[index++] = 0;
            }
            else
            {
                if (e.indexOf('.') < 0)
                {
                    val[index++] = Integer.parseInt(e, 16);
                    if (sTok.hasMoreTokens())
                    {
                        sTok.nextToken();
                    }
                }
                else
                {
                    StringTokenizer eTok = new StringTokenizer(e, ".");

                    val[index++] = (Integer.parseInt(eTok.nextToken()) << 8) | Integer.parseInt(eTok.nextToken());
                    val[index++] = (Integer.parseInt(eTok.nextToken()) << 8) | Integer.parseInt(eTok.nextToken());
                }
            }
        }

        if (index != val.length)
        {
            System.arraycopy(val, doubleColon, val, val.length - (index - doubleColon), index - doubleColon);
            for (int i = doubleColon; i != val.length - (index - doubleColon); i++)
            {
                val[i] = 0;
            }
        }

        return val;
    }

    public DERObject toASN1Object()
    {
        if (tag == directoryName)       // directoryName is explicitly tagged as it is a CHOICE
        {
            return new DERTaggedObject(true, tag, obj);
        }
        else
        {
            return new DERTaggedObject(false, tag, obj);
        }
    }
}
