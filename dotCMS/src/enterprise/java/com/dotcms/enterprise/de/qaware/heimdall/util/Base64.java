/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.de.qaware.heimdall.util;

import javax.xml.bind.DatatypeConverter;

/**
 * Base64 encoding.
 */
public final class Base64 {
    /**
     * No instances allowed.
     */
    private Base64() {
    }

    /**
     * Decodes the given base64 string into a byte array.
     *
     * @param base64 Base64 string.
     * @return Byte array.
     */
    public static byte[] decode(String base64) {
        Preconditions.checkNotNull(base64, "base64");

        return DatatypeConverter.parseBase64Binary(base64);
    }

    /**
     * Encodes the given byte array into a base64 string.
     *
     * @param data Byte array.
     * @return Base64 string.
     */
    public static String encode(byte[] data) {
        Preconditions.checkNotNull(data, "data");

        return DatatypeConverter.printBase64Binary(data);
    }
}
