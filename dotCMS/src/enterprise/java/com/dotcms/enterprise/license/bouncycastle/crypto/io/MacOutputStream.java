/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.dotcms.enterprise.license.bouncycastle.crypto.Mac;

public class MacOutputStream
    extends FilterOutputStream
{
    protected Mac mac;

    public MacOutputStream(
        OutputStream stream,
        Mac          mac)
    {
        super(stream);
        this.mac = mac;
    }

    public void write(int b)
        throws IOException
    {
        mac.update((byte)b);
        out.write(b);
    }

    public void write(
        byte[] b,
        int off,
        int len)
        throws IOException
    {
        mac.update(b, off, len);
        out.write(b, off, len);
    }

    public Mac getMac()
    {
        return mac;
    }
}
