package com.dotcms.ema;

import com.dotcms.ema.proxy.MockHttpCaptureResponse;
import com.dotcms.ema.proxy.ProxyResponse;
import com.dotcms.ema.proxy.ProxyTool;
import com.dotcms.ema.resolver.EMAConfigStrategy;
import com.dotcms.ema.resolver.EMAConfigStrategyResolver;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableMap;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Intercepts a content managers's request to dotCMS EDIT_MODE in the admin of dotCMS,
 * transparently POSTs the dotCMS page API data to the remote site/server (hosted elsewhere) and
 * then proxies the remote response back to the dotCMS admin, which allows dotCMS
 * to render the EDIT_MODE request in context.
 *
 * More info on how EMA works:
 * <a href="https://github.com/dotcms-plugins/com.dotcms.ema#dotcms-edit-mode-anywhere---ema">dotCMS Edit Mode Anywhere - EMA</a>
 */
public class EMAWebInterceptor implements WebInterceptor {

    public static final String PROXY_EDIT_MODE_URL_VAR = "proxyEditModeURL";
    /**
     * @deprecated This property is now inside the JSON value set via the {@code proxyEditModeURL} field
     */
    @Deprecated(forRemoval = true)
    public static final String INCLUDE_RENDERED_VAR = "includeRendered";
    public static final String AUTHENTICATION_TOKEN_VAR = "authenticationToken";
    public static final String PAGE_DATA_PARAM = "dotPageData";
    public static final String DEPTH_PARAM = "depth";
    private static final String API_CALL = "/api/v1/page/render";
    public static final String EMA_APP_CONFIG_KEY = "dotema-config";
    private static final ProxyTool proxy = new ProxyTool();

    public static final String EMA_REQUEST_ATTR = "EMA_REQUEST";
    public static final String EMA_AUTH_HEADER = "X-EMA-AUTH";

    @Override
    public String[] getFilters() {
        return new String[] {
                API_CALL + "*"
        };
    }

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) {
        final Host currentSite = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

        if (!this.existsConfiguration(currentSite.getIdentifier())) {
            return Result.NEXT;
        }

        final Optional<String> proxyUrl = proxyUrl(currentSite, request);
        final PageMode mode             = PageMode.get(request);

        if (proxyUrl.isEmpty() || mode == PageMode.LIVE) {
            return Result.NEXT;
        }

        Logger.info(this.getClass(), "GOT AN EMA Call --> " + request.getRequestURI());
        request.setAttribute(EMA_REQUEST_ATTR, true);
        final Optional<EMAConfigurationEntry> emaConfig = this.getEmaConfigByURL(currentSite, request.getRequestURI());
        final String depth =
                emaConfig.map(emaConfigurationEntry -> emaConfigurationEntry.getHeader(DEPTH_PARAM)).orElse(null);
        if (UtilMethods.isSet(depth)) {
            request.setAttribute(DEPTH_PARAM, depth);
        }
        return new Result.Builder().wrap(new MockHttpCaptureResponse(response)).next().build();
    }

    @Override
    public boolean afterIntercept(final HttpServletRequest request, final HttpServletResponse response) {
        final Host currentSite = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
        try {
            if (response instanceof MockHttpCaptureResponse) {
                final Optional<EMAConfigurationEntry> emaConfig = this.getEmaConfigByURL(currentSite, request.getRequestURI());
                final Optional<String> proxyUrlOpt = emaConfig.map(EMAConfigurationEntry::getUrlEndpoint);
                final String proxyUrl = getProxyURL(request).isPresent() ?
                        getProxyURL(request).get() : proxyUrlOpt.orElse("[ EMPTY ]");
                final MockHttpCaptureResponse mockResponse = (MockHttpCaptureResponse)response;
                final String postJson                      = new String(mockResponse.getBytes());
                final String authToken =
                        emaConfig.map(emaConfigurationEntry -> emaConfigurationEntry.getHeader(AUTHENTICATION_TOKEN_VAR)).orElse(null);
                final Map<String, String> params = ImmutableMap.of(PAGE_DATA_PARAM, postJson);
                
                Logger.info(this.getClass(), "Proxying Request --> " + proxyUrl);

                final ProxyResponse pResponse = proxy.sendPost(proxyUrl, params);
                final String authTokenFromEma = this.getHeaderValue(pResponse.getHeaders(), EMA_AUTH_HEADER);
                if (UtilMethods.isSet(authToken) && (UtilMethods.isNotSet(authTokenFromEma) || (UtilMethods.isSet(authToken) && !authToken.equals(authTokenFromEma)))) {
                    this.sendHttpResponse(this.generateErrorPage("Invalid Authentication Token",
                            proxyUrl, pResponse), postJson, response, true);
                } else if (pResponse.getResponseCode() != HttpStatus.SC_OK) {
                    this.sendHttpResponse(this.generateErrorPage("Unable to connect with the rendering engine",
                            proxyUrl, pResponse), postJson, response, true);
                } else {
                    this.sendHttpResponse(new String(pResponse.getResponse(), StandardCharsets.UTF_8), postJson, response, false);
                }
            }
        } catch (final Exception e) {
            final String siteInfo = (null != currentSite ? "'" + currentSite.getHostname() + "' [ " + currentSite.getIdentifier() + " ]" : "'unknown'");
            Logger.error(this.getClass(), String.format("Error processing EMA request for Site %s: %s",
                    siteInfo, e.getMessage()), e);
        }
        return true;
    }

    /**
     * Returns the URL to the EMA third-party server -- the Proxy URL -- that will handle the page editing requests for
     * the given Site. If required, such a Proxy URL can be overridden by adding it as a Request Parameter via the
     * {@link #PROXY_EDIT_MODE_URL_VAR} property.
     *
     * @param site The {@link Host} that will use the EMA Server.
     *
     * @return An Optional with the Proxy URL.
     */
    protected Optional<String> proxyUrl(final Host site, final HttpServletRequest request) {
        final Optional<String> overriddenProxyUrl = this.getProxyURL(request);
        if (overriddenProxyUrl.isPresent()) {
            return overriddenProxyUrl;
        }
        try {
            final Optional<EMAConfigurationEntry> emaConfig = this.getEmaConfigByURL(site, request.getRequestURI());
            return emaConfig.map(EMAConfigurationEntry::getUrlEndpoint);
        } catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when retrieving the Proxy URL for Site '%s': %s",
                    site, e.getMessage()));
            return Optional.empty();
        }
    }

    /**
     * Verifies if there's an active EMA configuration for the given Site, or the System Host.
     *
     * @param hostId The Site Identifier.
     *
     * @return If there's an EMA configuration for either the specified Site or the System Host, returns {@code true}.
     */
    private boolean existsConfiguration(final String hostId) {
        final List<String> hosts = Host.SYSTEM_HOST.equals(hostId) ? List.of(hostId) : Arrays.asList(Host.SYSTEM_HOST, hostId);
        return !APILocator.getAppsAPI().filterSitesForAppKey(EMA_APP_CONFIG_KEY,
                hosts, APILocator.systemUser()).isEmpty();
    }

    /**
     * Checks if the value of the EMA third-party server is being set via the HTTP Request. This will bypass the value
     * set via the EMA App.
     *
     * @param request The current instance of the {@link HttpServletRequest}.
     *
     * @return An Optional with the overridden Proxy URL, if present.
     */
    protected Optional<String> getProxyURL (final HttpServletRequest request) {
        Optional<String> proxyURL = Optional.empty();
        if (null != request.getParameter(PROXY_EDIT_MODE_URL_VAR)) {
            final String proxyURLParamValue = request.getParameter(PROXY_EDIT_MODE_URL_VAR);
            proxyURL = UtilMethods.isSet(proxyURLParamValue)?Optional.of(proxyURLParamValue):Optional.empty();
            if (null != request.getSession(false)) {
                if (UtilMethods.isSet(proxyURLParamValue)) {
                    // If the proxy is set, store it in the session
                    request.getSession(false).setAttribute(PROXY_EDIT_MODE_URL_VAR, proxyURLParamValue);
                } else {
                    // If it's set, but it's empty or null, just remove it
                    request.getSession(false).removeAttribute(PROXY_EDIT_MODE_URL_VAR);
                }
            }
        } else if (null != request.getSession(false) &&
                UtilMethods.isSet(request.getSession(false).getAttribute(PROXY_EDIT_MODE_URL_VAR))) {
            proxyURL = Optional.ofNullable(request.getSession(false).getAttribute(PROXY_EDIT_MODE_URL_VAR).toString());
        }
        return proxyURL;
    }

    /**
     * Returns the appropriate EMA configuration for a specific Site and page URL.
     *
     * @param site The {@link Host} object associated to the EMA configuration.
     * @param url  The incoming URL.
     *
     * @return An {@link Optional} object containing the value of the respective {@link EMAConfigurationEntry}.
     */
    protected Optional<EMAConfigurationEntry> getEmaConfigByURL(final Host site, final String url) {
        final Optional<EMAConfigStrategy> configStrategy = new EMAConfigStrategyResolver().get(site);
        final Optional<EMAConfigurations> emaConfig = configStrategy.flatMap(EMAConfigStrategy::resolveConfig);
        return emaConfig.isPresent() ? emaConfig.get().byUrl(url) : Optional.empty();
    }

    /**
     * Returns the value of a specific Header from the list of Headers.
     *
     * @param headers           The name-value array of {@link Header} objects that will be traversed.
     * @param emaAuthHeaderName The name of the Header that is being requested.
     *
     * @return The value of the Header, or {@code null} if it doesn't exist.
     */
    private String getHeaderValue(final Header[] headers, final String emaAuthHeaderName) {
        final Optional<Header> headerOpt =
                Arrays.stream(headers).filter(header -> emaAuthHeaderName.equals(header.getName())).findFirst();
        return headerOpt.map(NameValuePair::getValue).orElse(null);
    }

    /**
     * Generates an appropriate response to the client in the form of HTML code explaining why the EMA request failed.
     *
     * @param errorMsg  The human-readable error message.
     * @param proxyUrl  The URL that is being used to proxy the Edit Mode.
     * @param pResponse The object containing important information on the HTTP Headers present in the current response.
     *
     * @return The HTML code with the error message and related information.
     */
    protected String generateErrorPage(final String errorMsg, final String proxyUrl, final ProxyResponse pResponse) {
        final StringBuilder responseStringBuilder = new StringBuilder();
        responseStringBuilder.append("<html><body><h3>").append(errorMsg).append("</h3>")
                .append("<br><div style='display:inline-block;width:80px'>Trying:</div><b>").append(proxyUrl).append("</b>")
                .append("<br><div style='display:inline-block;width:80px'>Got:</div><b>").append(pResponse.getStatus()).append("</b>")
                .append("<hr>")
                .append("<h4>Headers</h4>")
                .append("<table border=1 style='min-width:500px'>");

        for (final Header header : pResponse.getHeaders()) {
            responseStringBuilder.append("<tr><td style='font-weight:bold;padding:5px;'><pre>")
                    .append(header.getName()).append("</pre></td><td><pre>").append(header.getValue()).append("</td></tr>");
        }
        responseStringBuilder.append("</table>")
                .append("<p>The Json Payload, POSTing as Content-Type:'application/x-www-form-urlencoded' with form " +
                                "param <b>dotPageData</b>, has been printed in the logs.</p>")
                .append("</body></html>");
        return responseStringBuilder.toString();
    }

    /**
     * Generates the HTTP Response with specific information that will be returned to the User.
     *
     * @param contents The HTML response.
     * @param postJson The JSON response from the EMA Server.
     * @param response The current {@link HttpServletResponse} object.
     * @param isError  If {@code true}, the JSON POST will be printed in the log.
     *
     * @throws IOException An error occurred when writing to the Response object.
     */
    protected void sendHttpResponse(final String contents, final String postJson, final HttpServletResponse response, final boolean isError) throws IOException {
        final JSONObject json = new JSONObject(postJson);
        if (isError) {
            Logger.error(this, "An error occurred with EMA: dotPageData = " + json);
        }
        json.getJSONObject("entity").getJSONObject("page").put("rendered", contents);
        json.getJSONObject("entity").getJSONObject("page").put("remoteRendered", true);
        response.setContentType(MediaType.APPLICATION_JSON);
        response.getWriter().write(json.toString());
    }

}
