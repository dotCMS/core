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

package com.dotcms.enterprise.license.bouncycastle.bcpg;

import com.dotcms.enterprise.license.bouncycastle.bcpg.sig.Exportable;
import com.dotcms.enterprise.license.bouncycastle.bcpg.sig.IssuerKeyID;
import com.dotcms.enterprise.license.bouncycastle.bcpg.sig.KeyExpirationTime;
import com.dotcms.enterprise.license.bouncycastle.bcpg.sig.KeyFlags;
import com.dotcms.enterprise.license.bouncycastle.bcpg.sig.NotationData;
import com.dotcms.enterprise.license.bouncycastle.bcpg.sig.PreferredAlgorithms;
import com.dotcms.enterprise.license.bouncycastle.bcpg.sig.PrimaryUserID;
import com.dotcms.enterprise.license.bouncycastle.bcpg.sig.Revocable;
import com.dotcms.enterprise.license.bouncycastle.bcpg.sig.SignatureCreationTime;
import com.dotcms.enterprise.license.bouncycastle.bcpg.sig.SignatureExpirationTime;
import com.dotcms.enterprise.license.bouncycastle.bcpg.sig.SignerUserID;
import com.dotcms.enterprise.license.bouncycastle.bcpg.sig.TrustSignature;
import com.dotcms.enterprise.license.bouncycastle.util.io.Streams;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * reader for signature sub-packets
 */
public class SignatureSubpacketInputStream
    extends InputStream implements SignatureSubpacketTags
{
    InputStream    in;
    
    public SignatureSubpacketInputStream(
        InputStream    in)
    {
        this.in = in;
    }
    
    public int available()
        throws IOException
    {
        return in.available();
    }
    
    public int read()
        throws IOException
    {
        return in.read();
    }

    public SignatureSubpacket readPacket()
        throws IOException
    {
        int            l = this.read();
        int            bodyLen = 0;
        
        if (l < 0)
        {
            return null;
        }

        if (l < 192)
        {
            bodyLen = l;
        }
        else if (l <= 223)
        {
            bodyLen = ((l - 192) << 8) + (in.read()) + 192;
        }
        else if (l == 255)
        {
            bodyLen = (in.read() << 24) | (in.read() << 16) |  (in.read() << 8)  | in.read();
        }
        else
        {
            // TODO Error?
        }

        int        tag = in.read();

        if (tag < 0)
        {
               throw new EOFException("unexpected EOF reading signature sub packet");
        }
       
        byte[]    data = new byte[bodyLen - 1];
        if (Streams.readFully(in, data) < data.length)
        {
            throw new EOFException();
        }
       
        boolean   isCritical = ((tag & 0x80) != 0);
        int       type = tag & 0x7f;

        switch (type)
        {
        case CREATION_TIME:
            return new SignatureCreationTime(isCritical, data);
        case KEY_EXPIRE_TIME:
            return new KeyExpirationTime(isCritical, data);
        case EXPIRE_TIME:
            return new SignatureExpirationTime(isCritical, data);
        case REVOCABLE:
            return new Revocable(isCritical, data);
        case EXPORTABLE:
            return new Exportable(isCritical, data);
        case ISSUER_KEY_ID:
            return new IssuerKeyID(isCritical, data);
        case TRUST_SIG:
            return new TrustSignature(isCritical, data);
        case PREFERRED_COMP_ALGS:
        case PREFERRED_HASH_ALGS:
        case PREFERRED_SYM_ALGS:
            return new PreferredAlgorithms(type, isCritical, data);
        case KEY_FLAGS:
            return new KeyFlags(isCritical, data);
        case PRIMARY_USER_ID:
            return new PrimaryUserID(isCritical, data);
        case SIGNER_USER_ID:
            return new SignerUserID(isCritical, data);
        case NOTATION_DATA:
            return new NotationData(isCritical, data);
        }

        return new SignatureSubpacket(type, isCritical, data);
    }
}
