package com.dotcms.enterprise.publishing.staticpublishing;

import java.io.Serializable;

/**
 * Encapsulates the AWSS3 configuration
 * @author jsanca
 */
public final class AWSS3Configuration implements Serializable {

    private final String accessKey;
    private final String secretKey;

    private AWSS3Configuration(final String accessKey, final String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public static final class Builder {

        private String accessKey;
        private String secretKey;

        public Builder accessKey(final String accessKey) {

            this.accessKey = accessKey;
            return this;
        }

        public Builder secretKey(final String secretKey) {

            this.secretKey = secretKey;
            return this;
        }

        public AWSS3Configuration build () {

            return new AWSS3Configuration(this.accessKey, this.secretKey);
        }
    }
} // E:O:F:AWSS3Configuration.
