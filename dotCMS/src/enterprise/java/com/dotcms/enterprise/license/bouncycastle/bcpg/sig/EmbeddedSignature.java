package com.dotcms.enterprise.license.bouncycastle.bcpg.sig;

import com.dotcms.enterprise.license.bouncycastle.bcpg.SignatureSubpacket;
import com.dotcms.enterprise.license.bouncycastle.bcpg.SignatureSubpacketTags;

/**
 * Packet embedded signature
 */
public class EmbeddedSignature
    extends SignatureSubpacket
{
    public EmbeddedSignature(
        boolean    critical,
        byte[]     data)
    {
        super(SignatureSubpacketTags.EMBEDDED_SIGNATURE, critical, data);
    }
}
