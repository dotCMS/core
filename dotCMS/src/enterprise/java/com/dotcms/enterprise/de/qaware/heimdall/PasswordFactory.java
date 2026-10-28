/*
* The MIT License (MIT)
*
* Copyright (c) 2015 QAware GmbH
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/
package com.dotcms.enterprise.de.qaware.heimdall;

import com.dotcms.enterprise.de.qaware.heimdall.algorithm.HashAlgorithm;
import com.dotcms.enterprise.de.qaware.heimdall.algorithm.HashAlgorithmRegistryImpl;
import com.dotcms.enterprise.de.qaware.heimdall.algorithm.PBKDF2;
import com.dotcms.enterprise.de.qaware.heimdall.algorithm.PBKDF2_256;
import com.dotcms.enterprise.de.qaware.heimdall.config.ConfigCoderImpl;
import com.dotcms.enterprise.de.qaware.heimdall.salt.SecureSaltProvider;

/**
 * Factory to create an instance of {@link Password}.
 */
public final class PasswordFactory {
    /** Legacy profile retained to verify existing 192-bit password records. */
    private static final HashAlgorithm LEGACY_PBKDF2 = new PBKDF2();

    /** Current profile used for newly generated password records. */
    private static final HashAlgorithm CURRENT_PBKDF2 = new PBKDF2_256();

    /** Singleton instance that can verify both profiles and writes the current one. */
    private static final Password password = new PasswordImpl(new SecureSaltProvider(),
            new ConfigCoderImpl(),
            new HashAlgorithmRegistryImpl(LEGACY_PBKDF2, CURRENT_PBKDF2), CURRENT_PBKDF2);

    /**
     * Static class - no instances allowed.
     */
    private PasswordFactory() {
    }

    /**
     * Creates an instance of {@link Password}.
     *
     * @return An instance of {@link Password}.
     */
    public static Password create() {
        return password;
    }
}
