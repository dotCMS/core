/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.tls;

public class TlsRuntimeException
    extends RuntimeException
{
    Throwable e;

    public TlsRuntimeException(String message, Throwable e)
    {
        super(message);

        this.e = e;
    }

    public TlsRuntimeException(String message)
    {
        super(message);
    }

    public Throwable getCause()
    {
        return e;
    }
}
