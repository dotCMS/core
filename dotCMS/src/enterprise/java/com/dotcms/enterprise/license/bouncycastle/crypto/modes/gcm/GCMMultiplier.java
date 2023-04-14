package com.dotcms.enterprise.license.bouncycastle.crypto.modes.gcm;

public interface GCMMultiplier
{
    void init(byte[] H);
    void multiplyH(byte[] x);
}
