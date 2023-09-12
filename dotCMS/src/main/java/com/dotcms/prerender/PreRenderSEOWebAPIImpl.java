package com.dotcms.prerender;

import com.dotcms.concurrent.ConditionalSubmitter;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.google.common.base.Predicate;
import com.liferay.util.StringPool;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;
import org.apache.http.util.EntityUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.google.common.collect.FluentIterable.from;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.HOST;

public class PreRenderSEOWebAPIImpl implements PreRenderSEOWebAPI {

    public static final String ESCAPED_FRAGMENT_KEY = "_escaped_fragment_";
    private final Map<String, CloseableHttpClient> httpClientByHostMap = new ConcurrentHashMap<>();
    private final HeaderGroup hopByHopHeaders = new HeaderGroup();
    private final Map<String, ConditionalSubmitter> conditionalSubmitterMap = new ConcurrentHashMap<>();

    public PreRenderSEOWebAPIImpl() {

        final  String[] headers = new String[]{
                "Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization",
                "TE", "Trailers", "Transfer-Encoding", "Upgrade"};

        Stream.of(headers).forEach(header ->
                this.hopByHopHeaders.addHeader(new BasicHeader(header, null)));
    }

    public CloseableHttpClient getHttpClient(final PrerenderConfig prerenderConfig) {

        final CloseableHttpClient client = this.httpClientByHostMap.computeIfAbsent(
                prerenderConfig.getHost().getIdentifier(),
                key -> createHttpClient(prerenderConfig));

        return client;
    }

    private CloseableHttpClient createHttpClient(final PrerenderConfig prerenderConfig) {

        final HttpClientBuilder builder = HttpClients.custom()
                .setConnectionManager(new PoolingHttpClientConnectionManager())
                .disableRedirectHandling();

        configureProxy(builder, prerenderConfig);
        configureTimeout(builder, prerenderConfig);
        return builder.build();
    }

    private HttpClientBuilder configureProxy(final HttpClientBuilder builder, final PrerenderConfig prerenderConfig) {

        final String proxy = prerenderConfig.getConfig().proxy;
        if (isNotBlank(proxy)) {

            final int proxyPort = prerenderConfig.getConfig().proxyPort;
            final DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(new HttpHost(proxy, proxyPort));
            builder.setRoutePlanner(routePlanner);
        }

        return builder;
    }

    private HttpClientBuilder configureTimeout(final HttpClientBuilder builder, final PrerenderConfig prerenderConfig) {

        final String socketTimeout = prerenderConfig.getConfig().socketTimeout;
        if (socketTimeout != null) {

            final RequestConfig config = RequestConfig.custom().setSocketTimeout(Integer.parseInt(socketTimeout)).build();
            builder.setDefaultRequestConfig(config);
        }

        return builder;
    }

    @Override
    public boolean  prerenderIfEligible(final HttpServletRequest request, final HttpServletResponse response) {

        return Try.of(()->handlePrerender(request, response)).getOrElse(false);
    }

    private boolean handlePrerender(final HttpServletRequest request, final HttpServletResponse response) {

        final Host host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        final Optional<AppConfig> appConfig = config(host);

        if (appConfig.isPresent()) {

            final PrerenderConfig prerenderConfig = new PrerenderConfig(appConfig.get(), host);

            final Supplier<Boolean> onAvailableSupplier = () -> {

                Logger.debug(this, ()->"Running prerender");

                final PreRenderEventHandler preRenderEventHandler = getEventHandler(appConfig.get());
                return beforeRender(request, response, preRenderEventHandler) ||
                        proxyPrerenderedPageResponse(request, response, preRenderEventHandler, prerenderConfig);
            };

            if (shouldShowPrerenderedPage(request, prerenderConfig)) {

                if (appConfig.get().maxRequestNumber > 0) {

                    final ConditionalSubmitter conditionalSubmitter =
                            this.getConditionalSubmitter(host.getIdentifier(), appConfig.get().maxRequestNumber);

                    return conditionalSubmitter.submit(onAvailableSupplier, () -> false);
                } else {

                    return onAvailableSupplier.get();
                }
            }
        }

        return false;
    }

    private ConditionalSubmitter getConditionalSubmitter(final String hostId, final int maxRequestNumber) {


        ConditionalSubmitter conditionalSubmitter =
                this.conditionalSubmitterMap.computeIfAbsent(hostId,
                        key -> DotConcurrentFactory.getInstance().createConditionalSubmitter(maxRequestNumber));

        // check if the maxRequestNumber has changed, if so destroy the current one and rebuilt the Conditional
        if (maxRequestNumber != conditionalSubmitter.slotsNumber()) {

            Logger.info(this, ()->"The maxRequestNumber has changed to: " + maxRequestNumber +
                    ", creating a new ConditionalSubmitter");
            this.conditionalSubmitterMap.remove(hostId);
            conditionalSubmitter = this.conditionalSubmitterMap.computeIfAbsent(hostId,
                    key -> DotConcurrentFactory.getInstance().createConditionalSubmitter(maxRequestNumber));
        }

        return conditionalSubmitter;
    }
    /**
     * Gets the secrets from the App - this will check the current host then the SYSTEMM_HOST for a
     * valid configuration. This lookup is low overhead and cached by dotCMS.
     *
     * @param host
     * @return
     */
    public Optional<AppConfig> config(final Host host) {

        // todo: should we catch this.
        final Optional<AppSecrets> appSecrets = Try.of(
                () -> APILocator.getAppsAPI().getSecrets(AppKeys.APP_KEY, true, host, APILocator.systemUser()))
                .getOrElse(Optional.empty());

        if (appSecrets.isEmpty()) {

            return Optional.empty();
        }

        final Map<String, Secret> secrets = appSecrets.get().getSecrets();
        final String forwardedURLHeader = Try.of(()->secrets
                .get(AppKeys.FORWARDED_URL_HEADER.key).getString()).getOrElse(StringPool.BLANK);
        final String prerenderToken = Try.of(()->secrets
                .get(AppKeys.PRE_RENDER_TOKEN.key).getString()).getOrElse(StringPool.BLANK);
        final String blacklist = Try.of(()->secrets
                .get(AppKeys.BLACK_LIST.key).getString()).getOrElse(StringPool.BLANK);
        final String preRenderEventHandler = Try.of(()->secrets
                .get(AppKeys.PRE_RENDER_EVENT_HANDLER.key).getString()).getOrElse(StringPool.BLANK);
        final String protocol = Try.of(()->secrets
                .get(AppKeys.PROTOCOL.key).getString()).getOrElse(StringPool.BLANK);
        final String crawlerUserAgents = Try.of(()->secrets
                .get(AppKeys.CRAWLER_USER_AGENTS.key).getString()).getOrElse(StringPool.BLANK);
        final String proxy = Try.of(()->secrets
                .get(AppKeys.PROXY.key).getString()).getOrElse(StringPool.BLANK);
        final String extensionToIgnore = Try.of(()->secrets
                .get(AppKeys.EXTENSIONS_TO_IGNORE.key).getString()).getOrElse(StringPool.BLANK);
        final int proxyPort = Try.of(()-> ConversionUtils.toInt(secrets
                .get(AppKeys.PROXY_PORT.key).getString(), -1) ).getOrElse(-1);
        final String whilelist = Try.of(()->secrets
                .get(AppKeys.WHILE_LIST.key).getString()).getOrElse(StringPool.BLANK);
        final String preRenderServiceUrl = Try.of(()->secrets
                .get(AppKeys.PRE_RENDER_SERVICE_URL.key).getString()).getOrElse(StringPool.BLANK);
        final String maxRequestNumber = Try.of(()->secrets
                .get(AppKeys.MAX_REQUEST_NUMBER.key).getString()).getOrElse("10");

        final AppConfig config = AppConfig.builder()
                .forwardedURLHeader(forwardedURLHeader)
                .prerenderToken(prerenderToken)
                .blacklist(blacklist)
                .whilelist(whilelist)
                .preRenderEventHandler(preRenderEventHandler)
                .protocol(protocol)
                .crawlerUserAgents(crawlerUserAgents)
                .proxy(proxy)
                .proxyPort(proxyPort)
                .extensionToIgnore(extensionToIgnore)
                .preRenderServiceUrl(preRenderServiceUrl)
                .maxRequestNumber(ConversionUtils.toInt(maxRequestNumber, 10))
                .build();

        return Optional.ofNullable(config);
    }

    private PreRenderEventHandler getEventHandler(final AppConfig appConfig) {

        final String preRenderEventHandler = appConfig.preRenderEventHandler;
        if (isNotBlank(preRenderEventHandler)) {
            try {
                return (PreRenderEventHandler) ReflectionUtils.newInstance(preRenderEventHandler);
            } catch (Exception e) {
                Logger.error(this.getClass().getName(),
                        "PreRenderEventHandler class not find or can not new a instance", e);
            }
        }
        return null;
    }

    private boolean proxyPrerenderedPageResponse(final HttpServletRequest request, final HttpServletResponse response,
                                                 final PreRenderEventHandler preRenderEventHandler,
                                                 final PrerenderConfig prerenderConfig) {

        CloseableHttpResponse prerenderServerResponse = null;

        try {

            final String apiUrl = getApiUrl(getFullUrl(request, prerenderConfig), prerenderConfig);
            Logger.debug(this.getClass().getName(),
                    ()->String.format("Prerender proxy will send request to:%s", apiUrl));
            final HttpGet getMethod = getHttpGet(apiUrl);
            this.copyRequestHeaders(request, getMethod, prerenderConfig);
            this.withPrerenderToken(getMethod, prerenderConfig);
            prerenderServerResponse = getHttpClient(prerenderConfig).execute(getMethod);
            response.setStatus(prerenderServerResponse.getStatusLine().getStatusCode());
            copyResponseHeaders(prerenderServerResponse, response);
            String html = getResponseHtml(prerenderServerResponse);
            html = afterRender(request, response, prerenderServerResponse, html, preRenderEventHandler);
            responseEntity(html, response);
            return true;
        } catch (Exception e) {

            Logger.error(this.getClass().getName(),e.getMessage(), e);
        } finally {

            IOUtils.closeQuietly(prerenderServerResponse);
        }

        return false;
    }

    /**
     * Copy response body data (the entity) from the proxy to the servlet client.
     */
    private void responseEntity(final String html, final HttpServletResponse servletResponse)
            throws IOException {

        try (PrintWriter printWriter = servletResponse.getWriter()) {

            printWriter.write(html);
            printWriter.flush();
        }
    }

    /**
     * Get the charset used to encode the http entity.
     */
    private String getContentCharSet(final HttpEntity entity) throws ParseException {

        if (entity == null) {

            return null;
        }

        String charset = null;

        if (entity.getContentType() != null) {

            final HeaderElement values[] = entity.getContentType().getElements();
            if (values.length > 0) {

                final NameValuePair param = values[0].getParameterByName("charset");
                if (param != null) {

                    charset = param.getValue();
                }
            }
        }

        return charset;
    }

    /**
     * Copy proxied response headers back to the servlet client.
     */
    private void copyResponseHeaders(final HttpResponse proxyResponse, final HttpServletResponse servletResponse) {

        servletResponse.setCharacterEncoding(getContentCharSet(proxyResponse.getEntity()));
        from(Arrays.asList(proxyResponse.getAllHeaders())).filter(
                header -> !hopByHopHeaders.containsHeader(header.getName())).transform(header -> {
                    servletResponse.addHeader(header.getName(), header.getValue());
                    return true;
                }).toList();
    }

    private String afterRender(final HttpServletRequest clientRequest,
                               final HttpServletResponse clientResponse,
                               final CloseableHttpResponse prerenderServerResponse,
                               final String responseHtml,
                               final PreRenderEventHandler preRenderEventHandler) {

        if (preRenderEventHandler != null) {

            return preRenderEventHandler.afterRender(clientRequest, clientResponse, prerenderServerResponse, responseHtml);
        }
        return responseHtml;
    }

    private String getResponseHtml(HttpResponse proxyResponse)
            throws IOException {

        final HttpEntity entity = proxyResponse.getEntity();
        return entity != null ? EntityUtils.toString(entity) : StringPool.BLANK;
    }

    private void withPrerenderToken(final HttpRequest proxyRequest, final PrerenderConfig prerenderConfig) {
        final String token = prerenderConfig.getPrerenderToken();
        //for new version prerender with token.
        if (isNotBlank(token)) {
            proxyRequest.addHeader("X-Prerender-Token", token);
        }
    }

    /**
     * Copy request headers from the servlet client to the proxy request.
     *
     * @throws URISyntaxException
     */
    private void copyRequestHeaders(final HttpServletRequest servletRequest, final HttpRequest proxyRequest, final PrerenderConfig prerenderConfig)
            throws URISyntaxException {

        // Get an Enumeration of all of the header names sent by the client
        final Enumeration<?> enumerationOfHeaderNames = servletRequest.getHeaderNames();
        while (enumerationOfHeaderNames.hasMoreElements()) {

            final String headerName = (String) enumerationOfHeaderNames.nextElement();
            //Instead the content-length is effectively set via InputStreamEntity
            if (!headerName.equalsIgnoreCase(CONTENT_LENGTH) && !hopByHopHeaders.containsHeader(headerName)) {

                final Enumeration<?> headers = servletRequest.getHeaders(headerName);
                while (headers.hasMoreElements()) { //sometimes more than one value

                    String headerValue = (String) headers.nextElement();
                    // In case the proxy host is running multiple virtual servers,
                    // rewrite the Host header to ensure that we get content from
                    // the correct virtual server
                    if (headerName.equalsIgnoreCase(HOST)) {

                        final HttpHost host = URIUtils.extractHost(new URI(prerenderConfig.getPrerenderServiceUrl()));
                        headerValue = host.getHostName();
                        if (host.getPort() != -1) {
                            headerValue += ":" + host.getPort();
                        }
                    }

                    proxyRequest.addHeader(headerName, headerValue);
                }
            }
        }
    }

    protected HttpGet getHttpGet(String apiUrl) {
        return new HttpGet(apiUrl);
    }

    private String getFullUrl(final HttpServletRequest request, final PrerenderConfig prerenderConfig) {

        final String url = getRequestURL(request, prerenderConfig);
        final String queryString = request.getQueryString();
        return isNotBlank(queryString) ? String.format("%s?%s", url, queryString) : url;
    }

    private String getApiUrl(final String url, final PrerenderConfig prerenderConfig) {

        final String prerenderServiceUrl = prerenderConfig.getPrerenderServiceUrl();
        return prerenderServiceUrl + url;
    }

    private boolean beforeRender(final HttpServletRequest request, final HttpServletResponse response,
                                 final PreRenderEventHandler preRenderEventHandler) {

        if (preRenderEventHandler != null) {

            final String html = preRenderEventHandler.beforeRender(request);

            if (isNotBlank(html)) {

                try(final PrintWriter writer = response.getWriter()) {

                    writer.write(html);
                    writer.flush();
                    return true;
                } catch (Exception e) {

                    Logger.error(this.getClass().getName(), e.getMessage(), e);
                }
            }
        }

        return false;
    }

    private boolean shouldShowPrerenderedPage(final HttpServletRequest request,
                                              final PrerenderConfig prerenderConfig) {

        final String userAgent = request.getHeader("User-Agent");
        final String url = getRequestURL(request, prerenderConfig);
        final String referer = request.getHeader("Referer");

        Logger.debug(this.getClass().getName(),
                String.format("checking request for %s from User-Agent %s and referer %s", url, userAgent, referer));

        if (!HttpGet.METHOD_NAME.equals(request.getMethod())) {

            Logger.debug(this.getClass().getName(),"Request is not HTTP GET; intercept: no");
            return false;
        }

        if (isInResources(url, prerenderConfig)) {

            Logger.debug(this.getClass().getName(),"request is for a (static) resource; intercept: no");
            return false;
        }

        String prerenderHeader = request.getHeader("X-Prerender");
        if (StringUtils.isNotEmpty(prerenderHeader)) {

            return false;
        }

        final List<String> whiteList = prerenderConfig.getWhitelist();
        if (whiteList != null && !isInWhiteList(url, whiteList)) {

            Logger.debug(this.getClass().getName(),"Whitelist is enabled, but this request is not listed; intercept: no");
            return false;
        }

        final List<String> blacklist = prerenderConfig.getBlacklist();
        if (blacklist != null && isInBlackList(url, referer, blacklist)) {

            Logger.debug(this.getClass().getName(),"Blacklist is enabled, and this request is listed; intercept: no");
            return false;
        }

        if (hasEscapedFragment(request)) {

            Logger.debug(this.getClass().getName(),"Request Has _escaped_fragment_; intercept: yes");
            return true;
        }

        if (StringUtils.isBlank(userAgent)) {

            Logger.debug(this.getClass().getName(),"Request has blank userAgent; intercept: no");
            return false;
        }

        if (!isInSearchUserAgent(userAgent, prerenderConfig)) {

            Logger.debug(this.getClass().getName(),"Request User-Agent is not a search bot; intercept: no");
            return false;
        }

        Logger.debug(this.getClass().getName(), String.format("Defaulting to request intercept(user-agent=%s): yes", userAgent));
        return true;
    }

    private String getRequestURL(final HttpServletRequest request, final PrerenderConfig prerenderConfig) {
        if (StringUtils.isNotEmpty(prerenderConfig.getForwardedURLHeader())) {
            String url = request.getHeader(prerenderConfig.getForwardedURLHeader());
            if (StringUtils.isNotEmpty(url)) {
                return url;
            }
        }
        if (StringUtils.isNotEmpty(prerenderConfig.getProtocol())) {
            String url = request.getRequestURL().toString();
            return url.replace(request.getScheme(), prerenderConfig.getProtocol());
        }
        return request.getRequestURL().toString();
    }

    private boolean isInResources(final String url, final PrerenderConfig prerenderConfig) {
        return from(prerenderConfig.getExtensionsToIgnore()).anyMatch(
                (Predicate<String>) item -> (url.indexOf('?') >= 0 ? url.substring(0, url.indexOf('?')) : url)
                .toLowerCase().endsWith(item));
    }

    private boolean isInWhiteList(final String url, final List<String> whitelist) {
        return from(whitelist).anyMatch(regex -> Pattern.compile(regex).matcher(url).matches());
    }

    private boolean isInBlackList(final String url, final String referer, final List<String> blacklist) {
        return from(blacklist).anyMatch(regex -> {
            final Pattern pattern = Pattern.compile(regex);
            return pattern.matcher(url).matches() ||
                    (!StringUtils.isBlank(referer) && pattern.matcher(referer).matches());
        });
    }

    private boolean hasEscapedFragment(final HttpServletRequest request) {
        return request.getParameterMap().containsKey(ESCAPED_FRAGMENT_KEY);
    }

    private boolean isInSearchUserAgent(final String userAgent, final PrerenderConfig prerenderConfig) {
        return from(prerenderConfig.getCrawlerUserAgents()).anyMatch(
                (Predicate<String>) item -> userAgent.toLowerCase().contains(item.toLowerCase()));
    }
}
