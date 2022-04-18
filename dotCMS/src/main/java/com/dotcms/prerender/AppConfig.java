package com.dotcms.prerender;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;

/**
 * Encapsulates the prerender configuration
 * @author jsanca
 */
@JsonDeserialize(builder = AppConfig.Builder.class)
public class AppConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    public final String preRenderEventHandler;
    public final String proxy;
    public final int proxyPort;
    public final String socketTimeout;
    public final String prerenderToken;
    public final String forwardedURLHeader;
    public final String protocol;
    public final String blacklist;
    public final String crawlerUserAgents;
    public final String extensionToIgnore;
    public final String whilelist;
    public final String preRenderServiceUrl;
    public final int maxRequestNumber;

    private AppConfig(Builder builder) {
        this.preRenderEventHandler = builder.preRenderEventHandler;
        this.proxy = builder.proxy;
        this.proxyPort = builder.proxyPort;
        this.socketTimeout = builder.socketTimeout;
        this.prerenderToken = builder.prerenderToken;
        this.forwardedURLHeader = builder.forwardedURLHeader;
        this.protocol = builder.protocol;
        this.blacklist = builder.blacklist;
        this.crawlerUserAgents = builder.crawlerUserAgents;
        this.extensionToIgnore = builder.extensionToIgnore;
        this.whilelist = builder.whilelist;
        this.preRenderServiceUrl = builder.preRenderServiceUrl;
        this.maxRequestNumber = builder.maxRequestNumber;
    }


    /**
     * Creates builder to build {@link AppConfig}.
     * 
     * @return created builder
     */

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder to build {@link AppConfig} and initialize it with the given object.
     * 
     * @param appConfig to initialize the builder with
     * @return created builder
     */

    public static Builder from(AppConfig appConfig) {
        return new Builder(appConfig);
    }

    /**
     * Builder to build {@link AppConfig}.
     */

    public static final class Builder {

        public String preRenderEventHandler;
        public String proxy;
        public int proxyPort;
        public String socketTimeout;
        public String prerenderToken;
        public String forwardedURLHeader;
        public String protocol;
        public String blacklist;
        public String whilelist;
        public String crawlerUserAgents;
        public String extensionToIgnore;
        public String preRenderServiceUrl;
        private int maxRequestNumber = -1;

        private Builder() {}

        private Builder(final AppConfig appConfig) {

            this.preRenderEventHandler = appConfig.preRenderEventHandler;
            this.proxy = appConfig.proxy;
            this.proxyPort = appConfig.proxyPort;
            this.socketTimeout = appConfig.socketTimeout;
            this.prerenderToken = appConfig.prerenderToken;
            this.forwardedURLHeader = appConfig.forwardedURLHeader;
            this.protocol = appConfig.protocol;
            this.blacklist = appConfig.blacklist;
            this.crawlerUserAgents = appConfig.crawlerUserAgents;
            this.extensionToIgnore = appConfig.extensionToIgnore;
            this.whilelist = appConfig.whilelist;
            this.preRenderServiceUrl = appConfig.preRenderServiceUrl;
        }

        public Builder preRenderEventHandler(String preRenderEventHandler) {
            this.preRenderEventHandler = preRenderEventHandler;
            return this;
        }


        public Builder proxy(String proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder proxyPort(int proxyPort) {
            this.proxyPort = proxyPort;
            return this;
        }

        public Builder socketTimeout(String socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public Builder prerenderToken(String prerenderToken) {
            this.prerenderToken = prerenderToken;
            return this;
        }

        public Builder forwardedURLHeader(String forwardedURLHeader) {
            this.forwardedURLHeader = forwardedURLHeader;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder blacklist(String blacklist) {
            this.blacklist = blacklist;
            return this;
        }

        public Builder crawlerUserAgents(String crawlerUserAgents) {
            this.crawlerUserAgents = crawlerUserAgents;
            return this;
        }

        public AppConfig build() {
            return new AppConfig(this);
        }


        public Builder extensionToIgnore(String extensionToIgnore) {
            this.extensionToIgnore = extensionToIgnore;
            return this;
        }

        public Builder whilelist(String whilelist) {
            this.whilelist = whilelist;
            return this;
        }

        public Builder preRenderServiceUrl(String preRenderServiceUrl) {
            this.preRenderServiceUrl = preRenderServiceUrl;
            return this;
        }

        public Builder maxRequestNumber(final int maxRequestNumber) {
            this.maxRequestNumber = maxRequestNumber;
            return this;
        }
    }
}
