/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.staticpublishing;

import com.dotcms.publishing.PublisherConfig;

/**
 * Encapsulates the PublisherConfig for a {@link AWSS3Publisher}
 * @author jsanca
 */
public class StaticConfig extends PublisherConfig {

    private AWSS3Configuration awss3Configuration;

    public AWSS3Configuration getAwss3Configuration() {
        return awss3Configuration;
    }

    public void setAwss3Configuration(AWSS3Configuration awss3Configuration) {
        this.awss3Configuration = awss3Configuration;
    }
} // E:O:F:StaticConfig.
