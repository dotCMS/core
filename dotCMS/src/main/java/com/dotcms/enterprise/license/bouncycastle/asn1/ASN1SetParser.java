package com.dotcms.enterprise.license.bouncycastle.asn1;

import java.io.IOException;

public interface ASN1SetParser
    extends DEREncodable
{
    public DEREncodable readObject()
        throws IOException;
}
