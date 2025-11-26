/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.license.bouncycastle.crypto.params;

import com.dotcms.enterprise.license.bouncycastle.crypto.KeyGenerationParameters;

import java.security.SecureRandom;

public class GOST3410KeyGenerationParameters
        extends KeyGenerationParameters
{
        private GOST3410Parameters    params;

        public GOST3410KeyGenerationParameters(
            SecureRandom    random,
            GOST3410Parameters   params)
        {
            super(random, params.getP().bitLength() - 1);

            this.params = params;
        }

        public GOST3410Parameters getParameters()
        {
            return params;
        }
}
