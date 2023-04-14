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

import com.dotcms.enterprise.license.bouncycastle.crypto.Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.digests.MD5Digest;
import com.dotcms.enterprise.license.bouncycastle.crypto.digests.SHA1Digest;

/**
 * A combined hash, which implements md5(m) || sha1(m).
 */
public class CombinedHash implements Digest
{
    private Digest md5 = new MD5Digest();
    private Digest sha1 = new SHA1Digest();

    /**
     * @see com.dotcms.enterprise.license.bouncycastle.crypto.Digest#getAlgorithmName()
     */
    public String getAlgorithmName()
    {
        return md5.getAlgorithmName() + " and " + sha1.getAlgorithmName() + " for TLS 1.0";
    }

    /**
     * @see com.dotcms.enterprise.license.bouncycastle.crypto.Digest#getDigestSize()
     */
    public int getDigestSize()
    {
        return 16 + 20;
    }

    /**
     * @see com.dotcms.enterprise.license.bouncycastle.crypto.Digest#update(byte)
     */
    public void update(byte in)
    {
        md5.update(in);
        sha1.update(in);
    }

    /**
     * @see com.dotcms.enterprise.license.bouncycastle.crypto.Digest#update(byte[],int,int)
     */
    public void update(byte[] in, int inOff, int len)
    {
        md5.update(in, inOff, len);
        sha1.update(in, inOff, len);
    }

    /**
     * @see com.dotcms.enterprise.license.bouncycastle.crypto.Digest#doFinal(byte[],int)
     */
    public int doFinal(byte[] out, int outOff)
    {
        int i1 = md5.doFinal(out, outOff);
        int i2 = sha1.doFinal(out, outOff + 16);
        return i1 + i2;
    }

    /**
     * @see com.dotcms.enterprise.license.bouncycastle.crypto.Digest#reset()
     */
    public void reset()
    {
        md5.reset();
        sha1.reset();
    }

}
