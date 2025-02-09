/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.asn1.cms;

import com.dotcms.enterprise.license.bouncycastle.asn1.ASN1SequenceParser;
import com.dotcms.enterprise.license.bouncycastle.asn1.DERInteger;
import com.dotcms.enterprise.license.bouncycastle.asn1.x509.AlgorithmIdentifier;

import java.io.IOException;

/**
 * RFC 3274 - CMS Compressed Data.
 * <pre>
 * CompressedData ::= SEQUENCE {
 *  version CMSVersion,
 *  compressionAlgorithm CompressionAlgorithmIdentifier,
 *  encapContentInfo EncapsulatedContentInfo
 * }
 * </pre>
 */
public class CompressedDataParser
{
    private DERInteger _version;
    private AlgorithmIdentifier _compressionAlgorithm;
    private ContentInfoParser _encapContentInfo;

    public CompressedDataParser(
        ASN1SequenceParser seq)
        throws IOException
    {
        this._version = (DERInteger)seq.readObject();
        this._compressionAlgorithm = AlgorithmIdentifier.getInstance(seq.readObject().getDERObject());
        this._encapContentInfo = new ContentInfoParser((ASN1SequenceParser)seq.readObject());
    }

    public DERInteger getVersion()
    {
        return _version;
    }

    public AlgorithmIdentifier getCompressionAlgorithmIdentifier()
    {
        return _compressionAlgorithm;
    }

    public ContentInfoParser getEncapContentInfo()
    {
        return _encapContentInfo;
    }
}
