/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.engines;

/**
 * An implementation of the SEED key wrapper based on RFC 4010/RFC 3394.
 * <p>
 * For further details see: <a href="http://www.ietf.org/rfc/rfc4010.txt">http://www.ietf.org/rfc/rfc4010.txt</a>.
 */
public class SEEDWrapEngine
    extends RFC3394WrapEngine
{
    public SEEDWrapEngine()
    {
        super(new SEEDEngine());
    }
}
