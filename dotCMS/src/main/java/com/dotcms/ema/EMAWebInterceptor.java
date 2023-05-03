package com.dotcms.ema;

import com.dotcms.ema.proxy.MockHttpCaptureResponse;
import com.dotcms.ema.proxy.ProxyResponse;
import com.dotcms.ema.proxy.ProxyTool;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.security.apps.AppSecrets;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import org.apache.http.Header;
import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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

    public  static final String      PROXY_EDIT_MODE_URL_VAR = "proxyEditModeURL";
    public  static final String      INCLUDE_RENDERED_VAR = "includeRendered";
    public static final String AUTHENTICATION_TOKEN_VAR = "authenticationToken";
    private static final String      API_CALL                = "/api/v1/page/render";
    public static final String EMA_APP_CONFIG_KEY = "dotema-config";
    private static final ProxyTool   proxy                   = new ProxyTool();

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

        final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

        if (!this.existsConfiguration(currentHost.getIdentifier())) {
            return Result.NEXT;
        }

        final Optional<String> proxyUrl = proxyUrl(currentHost, request);
        final PageMode mode             = PageMode.get(request);

        if (proxyUrl.isEmpty() || mode == PageMode.LIVE) {
            return Result.NEXT;
        }

        Logger.info(this.getClass(), "GOT AN EMA Call --> " + request.getRequestURI());
        request.setAttribute(EMA_REQUEST_ATTR, true);
        return new Result.Builder().wrap(new MockHttpCaptureResponse(response)).next().build();
    }


    @Override
    public boolean afterIntercept(final HttpServletRequest request, final HttpServletResponse response) {

        try {

            if (response instanceof MockHttpCaptureResponse) {

                final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
                final Optional<String> proxyUrl            = proxyUrl(currentHost, request);
                final MockHttpCaptureResponse mockResponse = (MockHttpCaptureResponse)response;
                final String postJson                      = new String(mockResponse.getBytes());
                final JSONObject json                      = new JSONObject(postJson);
                final Optional<String> authTokenOpt = getEmaAppParameter(currentHost, AUTHENTICATION_TOKEN_VAR);
                final Map<String, String> params           = ImmutableMap.of("dotPageData", postJson);
                
                Logger.info(this.getClass(), "Proxying Request --> " + proxyUrl.get());

                final StringBuilder responseStringBuilder = new StringBuilder();
                final ProxyResponse pResponse = proxy.sendPost(proxyUrl.get(), params);
                final String authTokenFromEma = this.getHeaderValue(pResponse.getHeaders(), EMA_AUTH_HEADER);
                if (authTokenOpt.isPresent() && (UtilMethods.isNotSet(authTokenFromEma) || !authTokenOpt.get().equals(authTokenFromEma))) {
                    responseStringBuilder.append(this.generateErrorPage("Invalid Authentication Token",
                            proxyUrl.get(), pResponse));
                } else if (pResponse.getResponseCode() == HttpStatus.SC_OK) {
                    responseStringBuilder.append(new String(pResponse.getResponse(), StandardCharsets.UTF_8.name()));
                } else {
                    responseStringBuilder.append(this.generateErrorPage("Unable to connect with the rendering engine"
                            , proxyUrl.get(), pResponse));
                }

                json.getJSONObject("entity").getJSONObject("page").put("rendered", responseStringBuilder.toString());
                json.getJSONObject("entity").getJSONObject("page").put("remoteRendered", true);
                response.setContentType("application/json");

                response.getWriter().write(json.toString());
            }
        } catch (final Exception e) {

            final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
            Logger.warnAndDebug(this.getClass(), "Error on site: " +
                (null != currentHost?currentHost.getIdentifier() +" " + currentHost.getHostname(): "unknown") + ", msg: " + e.getMessage(), e);
        }

        return true;
    }


    /**
     * Gets the proxyUrl for the given host
     * @param currentHost current Host to get the proxyUrl value
     * @return Optional String of the proxyUrl, if there is not found or an error is thrown returns an empty
     */
    protected Optional<String> proxyUrl(final Host currentHost, final HttpServletRequest request) {
        AppSecrets appSecrets = null;

        final Optional<String> overridedProxyUrl = this.getProxyURL(request);

        if (overridedProxyUrl.isPresent()) {

            return overridedProxyUrl;
        }

        try{
            appSecrets = APILocator.getAppsAPI().getSecrets(EMA_APP_CONFIG_KEY,
                    true, currentHost, APILocator.systemUser()).get();
            final Optional<String> proxyUrlOpt =  appSecrets.getSecrets().containsKey(PROXY_EDIT_MODE_URL_VAR)?
                    Optional.ofNullable(appSecrets.getSecrets().get(PROXY_EDIT_MODE_URL_VAR).getString()) : Optional.empty();

            if (proxyUrlOpt.isPresent()) {

                final String proxyUrlText = proxyUrlOpt.get().trim();
                if (StringUtils.isJson(proxyUrlText)) {

                    final String cacheKey          = "ema-rewrite-"+currentHost.getIdentifier();
                    RewritesBean rewriteBeans = (RewritesBean) CacheLocator.getSystemCache().get(cacheKey);
                    if (null == rewriteBeans) {
                        try {
                            rewriteBeans = DotObjectMapperProvider.getInstance().
                                    getDefaultObjectMapper().readValue(proxyUrlText, RewritesBean.class);
                        } catch (JsonProcessingException e) {
                            Logger.debug(this, "Wrong json: " + proxyUrlText + ", msg: " + e.getMessage(), e);
                            return Optional.empty();
                        }

                        CacheLocator.getSystemCache().put(cacheKey, rewriteBeans);
                    }

                    return getProxyUrlFrom(rewriteBeans, request);
                }
            }

            return proxyUrlOpt;
        } catch (final Exception e) {
            Logger.error(this, String.format("An error occurred when generating the Proxy URL for Site '%s': %s",
                    currentHost, e.getMessage()));
            return Optional.empty();
        } finally {
            if(UtilMethods.isSet(appSecrets)){
                appSecrets.destroy();
            }
        }
    }

    protected Optional<String> getProxyUrlFrom(final RewritesBean rewriteBeans, final HttpServletRequest request) {

        if (UtilMethods.isSet(rewriteBeans)) {

            final String requestURI = request.getRequestURI();
            for (final RewriteBean rewriteBean : rewriteBeans.getRewrites()) {

                if (isRequestURIMath (rewriteBean.getSource(), requestURI)) {

                    return Optional.of(rewriteBean.getDestination());
                }
            }
        }

        return Optional.empty();
    }

    protected boolean isRequestURIMath(final String source, final String requestURI) {

        String uftUri;

        try {

            uftUri = URLDecoder.decode(requestURI, "UTF-8");
        } catch (UnsupportedEncodingException e) {

            uftUri = requestURI;
        }

        return RegEX.containsCaseInsensitive(uftUri, source.trim());
    }

    /**
     * Check if a configuration exists for the given site
     * @param hostId host id to check
     * @return true if the configuration exists, false if does not
     */
    private boolean existsConfiguration(final String hostId) {
        final List<String> hosts = Host.SYSTEM_HOST.equals(hostId) ? List.of(hostId) : Arrays.asList(Host.SYSTEM_HOST, hostId);
        return !APILocator.getAppsAPI().filterSitesForAppKey(EMA_APP_CONFIG_KEY,
                hosts, APILocator.systemUser()).isEmpty();
    }

    protected Optional<String> getProxyURL (final HttpServletRequest request) {

        Optional<String> proxyURL = Optional.empty();
        if (null != request.getParameter(PROXY_EDIT_MODE_URL_VAR)) {

            final String proxyURLParamValue = request.getParameter(PROXY_EDIT_MODE_URL_VAR);
            proxyURL = UtilMethods.isSet(proxyURLParamValue)?Optional.ofNullable(proxyURLParamValue):Optional.empty();
            if (null != request.getSession(false)) {
                if (UtilMethods.isSet(proxyURLParamValue)) { // if the proxy is set, stores in the session
                    request.getSession(false).setAttribute(PROXY_EDIT_MODE_URL_VAR, proxyURLParamValue);
                } else { // if it is set but it is empty or null, remove it
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
     * Returns a given parameter from a specific Site.
     *
     * @param site     The dotCMS Site whose parameter will be retrieved.
     * @param paramKey The parameter being requested, using its key.
     *
     * @return An {@link Optional} object containing the value of the specified parameter, or an empty optional if it
     * doesn't exist.
     */
    protected Optional<String> getEmaAppParameter(final Host site, final String paramKey) {
        final AppSecrets appSecrets;
        try {
            appSecrets =
                    APILocator.getAppsAPI().getSecrets(EMA_APP_CONFIG_KEY, true, site, APILocator.systemUser()).get();
            return appSecrets.getSecrets().containsKey(paramKey) &&
                           UtilMethods.isSet(appSecrets.getSecrets().get(paramKey).getString())
                           ? Optional.of(appSecrets.getSecrets().get(paramKey).getString()) : Optional.empty();
        } catch (final DotDataException | DotSecurityException e) {
            Logger.error(this, String.format("An error occurred when accessing EMA parameters for site '%s': %s",
                    site, e.getMessage()), e);
        }
        return Optional.empty();
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
        if (headerOpt.isPresent()) {
            return headerOpt.get().getValue();
        }
        return null;
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

}
