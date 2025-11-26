/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.util;

public class StoreException
    extends RuntimeException
{
    private Throwable _e;

    public StoreException(String s, Throwable e)
    {
        super(s);
        _e = e;
    }

    public Throwable getCause()
    {
        return _e;
    }
}
