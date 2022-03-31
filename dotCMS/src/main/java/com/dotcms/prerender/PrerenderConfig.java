package com.dotcms.prerender;

import com.dotmarketing.beans.Host;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Encapsulates the logic to retrieve from the configuration the values AND/OR default values for the prerender
 * @author jsanca
 */
public class PrerenderConfig {

    private final Host host;
    private final AppConfig config;
    public static final String PRERENDER_IO_SERVICE_URL = "http://service.prerender.io/";

    public PrerenderConfig(final AppConfig appConfig, final Host host) {
        this.config = appConfig;
        this.host   = host;
    }

    public Host getHost() {
        return host;
    }

    public AppConfig getConfig() {
        return config;
    }

    public String getPrerenderToken() {
        return config.prerenderToken;
    }

    public String getPrerenderServiceUrl() {

        final String prerenderServiceUrl = config.preRenderServiceUrl;
        return isNotBlank(prerenderServiceUrl) ? prerenderServiceUrl : getDefaultPrerenderIoServiceUrl();
    }

    private String getDefaultPrerenderIoServiceUrl() {
        final String prerenderServiceUrlInEnv = System.getProperty("PRERENDER_SERVICE_URL");
        return isNotBlank(prerenderServiceUrlInEnv) ? prerenderServiceUrlInEnv : PRERENDER_IO_SERVICE_URL;
    }

    public List<String> getWhitelist() {
        final String whitelist = config.whilelist;
        if (isNotBlank(whitelist)) {
            return Arrays.asList(whitelist.trim().split(","));
        }
        return null;
    }

    public List<String> getBlacklist() {
        final String blacklist = config.blacklist;
        if (isNotBlank(blacklist)) {
            return Arrays.asList(blacklist.trim().split(","));
        }
        return null;
    }

    public String getForwardedURLHeader() {

        return config.forwardedURLHeader;
    }

    public String getProtocol() {

        return config.protocol;
    }

    public List<String> getExtensionsToIgnore() {

        final List<String> extensionsToIgnore = Lists.newArrayList(".js", ".json", ".css", ".xml", ".less", ".png", ".jpg",
                ".jpeg", ".gif", ".pdf", ".doc", ".txt", ".ico", ".rss", ".zip", ".mp3", ".rar", ".exe", ".wmv",
                ".doc", ".avi", ".ppt", ".mpg", ".mpeg", ".tif", ".wav", ".mov", ".psd", ".ai", ".xls", ".mp4",
                ".m4a", ".swf", ".dat", ".dmg", ".iso", ".flv", ".m4v", ".torrent", ".woff", ".ttf");
        final String extensionsToIgnoreFromConfig = config.extensionToIgnore;
        if (isNotBlank(extensionsToIgnoreFromConfig)) {
            extensionsToIgnore.addAll(Arrays.asList(extensionsToIgnoreFromConfig.trim().split(",")));
        }

        return extensionsToIgnore;
    }

    public List<String> getCrawlerUserAgents() {

        final List<String> crawlerUserAgents = Lists.newArrayList("baiduspider",
                "facebookexternalhit", "twitterbot", "rogerbot", "linkedinbot", "embedly", "quora link preview"
                , "showyoubo", "outbrain", "pinterest", "developers.google.com/+/web/snippet", "slackbot", "vkShare",
                "W3C_Validator", "redditbot", "Applebot");
        final String crawlerUserAgentsFromConfig = config.crawlerUserAgents;
        if (isNotBlank(crawlerUserAgentsFromConfig)) {
            crawlerUserAgents.addAll(Arrays.asList(crawlerUserAgentsFromConfig.trim().split(",")));
        }

        return crawlerUserAgents;
    }

    public int getMaxRequestNumber() {

        return config.maxRequestNumber;
    }

}
