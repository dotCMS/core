package com.dotcms.enterprise.license.bouncycastle.crypto.tls;

import com.dotcms.enterprise.license.bouncycastle.crypto.encodings.PKCS1Encoding;
import com.dotcms.enterprise.license.bouncycastle.crypto.engines.RSABlindedEngine;
import com.dotcms.enterprise.license.bouncycastle.crypto.signers.GenericSigner;

class TlsRSASigner
    extends GenericSigner
{
    TlsRSASigner()
    {
        super(new PKCS1Encoding(new RSABlindedEngine()), new CombinedHash());
    }
}
