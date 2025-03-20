/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.publishing.staticpublishing;

import java.io.Serializable;
import java.net.URL;
import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.control.Try;

/**
 * Encapsulates the AWSS3 configuration
 * @author jsanca
 */
public final class AWSS3Configuration implements Serializable {

    private final String accessKey;
    private final String secretKey;
    private final URL endPoint;
    private final String region;

    private AWSS3Configuration(final String accessKey, final String secretKey, final String endPoint,
            final String region) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.endPoint = Try.of(()-> new URL(endPoint)).getOrElse((URL) null);
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getEndPoint() {
        return Try.of(()-> endPoint.toString()).getOrElse((String) null);
    }

    public String getRegion() {
        return region;
    }

    public static final class Builder {

        private String accessKey;
        private String secretKey;
        private  String endPoint;
        private  String region;

        public Builder accessKey(final String accessKey) {

            this.accessKey = accessKey;
            return this;
        }

        public Builder secretKey(final String secretKey) {

            this.secretKey = secretKey;
            return this;
        }

        public Builder endPoint(final String endPoint) {

            this.endPoint = endPoint;
            return this;
        }

        public Builder region(final String region) {

            this.region = region;
            return this;
        }

        public AWSS3Configuration build () {

            return new AWSS3Configuration(this.accessKey, this.secretKey, this.endPoint, this.region);
        }
    }
} // E:O:F:AWSS3Configuration.
