package com.dotcms.enterprise.license.bouncycastle.asn1;

import java.io.InputStream;

public interface ASN1OctetStringParser
    extends DEREncodable
{
    public InputStream getOctetStream();
}
