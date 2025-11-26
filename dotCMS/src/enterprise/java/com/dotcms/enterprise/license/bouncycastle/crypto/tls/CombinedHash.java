/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
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
