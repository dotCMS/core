package com.dotcms.prerender;

/**
 * Encapsulates the config names for prerender
 * @author jsanca
 */
public enum AppKeys {
    PRE_RENDER_EVENT_HANDLER("preRenderEventHandler"),
    PROXY("proxy"),
    PROXY_PORT("proxyPort"),
    PRE_RENDER_TOKEN("prerenderToken"),
    FORWARDED_URL_HEADER("forwardedURLHeader"),
    CRAWLER_USER_AGENTS("crawlerUserAgents"),
    EXTENSIONS_TO_IGNORE("extensionsToIgnore"),
    WHILE_LIST("whitelist"),
    BLACK_LIST("blacklist"),
    PRE_RENDER_SERVICE_URL("preRenderServiceUrl"),
    PROTOCOL("protocol"),
    MAX_REQUEST_NUMBER("maxRequestNumber");

    final public String key;

    AppKeys(String key) {
        this.key = key;
    }

    public final static String APP_KEY = "dotPreRenderSEO-config";

    public final static String APP_YAML_NAME = APP_KEY + ".yml";
}
