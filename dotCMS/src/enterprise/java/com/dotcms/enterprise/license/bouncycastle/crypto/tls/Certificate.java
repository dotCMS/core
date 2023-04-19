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

package com.dotcms.enterprise.license.bouncycastle.crypto.tls;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1InputStream;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERObject;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.X509CertificateStructure;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

/**
 * A representation for a certificate chain as used by an tls server.
 */
public class Certificate
{
    /**
     * The certificates.
     */
    protected X509CertificateStructure[] certs;

    /**
     * Parse the ServerCertificate message.
     *
     * @param is The stream where to parse from.
     * @return A Certificate object with the certs, the server has sended.
     * @throws IOException If something goes wrong during parsing.
     */
    protected static Certificate parse(InputStream is) throws IOException
    {
        X509CertificateStructure[] certs;
        int left = TlsUtils.readUint24(is);
        Vector tmp = new Vector();
        while (left > 0)
        {
            int size = TlsUtils.readUint24(is);
            left -= 3 + size;
            byte[] buf = new byte[size];
            TlsUtils.readFully(buf, is);
            ByteArrayInputStream bis = new ByteArrayInputStream(buf);
            ASN1InputStream ais = new ASN1InputStream(bis);
            DERObject o = ais.readObject();
            tmp.addElement(X509CertificateStructure.getInstance(o));
            if (bis.available() > 0)
            {
                throw new IllegalArgumentException("Sorry, there is garbage data left after the certificate");
            }
        }
        certs = new X509CertificateStructure[tmp.size()];
        for (int i = 0; i < tmp.size(); i++)
        {
            certs[i] = (X509CertificateStructure)tmp.elementAt(i);
        }
        return new Certificate(certs);
    }

    /**
     * Private constructure from an cert array.
     *
     * @param certs The certs the chain should contain.
     */
    private Certificate(X509CertificateStructure[] certs)
    {
        this.certs = certs;
    }

    /**
     * @return An array which contains the certs, this chain contains.
     */
    public X509CertificateStructure[] getCerts()
    {
        X509CertificateStructure[] result = new X509CertificateStructure[certs.length];
        System.arraycopy(certs, 0, result, 0, certs.length);
        return result;
    }

}
