package com.dotcms.security;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.repackage.org.apache.axiom.om.util.Base64;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import org.apache.commons.lang.RandomStringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

/**
 * Provide util method to set the Content-security-Policy header. It has methos to calculate a Nonce value,
 * set the <code>Content-Security-Policy</code> header and to set a nonce value to each script or
 * style block ino a html code.
 * You need set the <code>ContentSecurityPolicy.header</code> config property to use the
 * <code>Content-Security-Policy</code> header features, the value the in the config properties is going
 * to be the same set to the header, but it has two special values:
 * - {script-src nonce}: You can use it like <code>script-src {script-src nonce}</code>, and a random
 * nonce is going to be calculated and set to each script block in <code>htmlCode</code>
 *
 * - {style-src nonce}: You can use it like <code>style-src {style-src nonce}</code>, and a random
 * nonce is going to be calculated and set to each style block in <code>htmlCode</code>
 *
 * When any of the special values to the <code>ContentSecurityPolicy.header</code> config property
 * is used then a new nonce value have to be calculated.
 *
 * To use this class you have to call the {@link this#init(HttpServletRequest)} method first,
 * if a nonce value need to be calculated then it is calculated and a request's attribute is set to
 * save the value for the request.
 *
 * To set the <code>Content-Security-Policy</code> header in a {@link HttpServletResponse} you have to use the
 * {@link this#addHeader(HttpServletResponse)} method after called the {@link this#init(HttpServletRequest)} method.
 *
 * Use the {@link this#apply(String)} to apply the nonce calculate in the current request in a
 * html code.
 *
 * Example:
 * <pre>
 *     HttpServletRequest request = ...;
 *     HttpServletResponse = ...;
 *     ContentSecurityPolicyUtil.init(request);
 *     ContentSecurityPolicyUtil.addHeader(response);
 *
 *     String html = "<script>console.log('This is a test');</script><style>h1{color:red;}</style>";
 *     ContentSecurityPolicyUtil.apply(html);
 * </pre>
 *
 * The value to the html variable and the Content-Security-Policy header will depend on
 * the <code>ContentSecurityPolicy.header</code> config property:
 *
 * - ContentSecurityPolicy.header equals to <code>default-src 'self'</code>: nonce value not need to be calculated
 * Content-Security-Policy header equals to <code>default-src 'self'</code>
 * html equals to "<script>console.log('This is a test');</script><style>h1{color:red;}</style>";
 *
 * - ContentSecurityPolicy.header equals to <code>script-src {script-src nonce}</code>: nonce value not need to be calculated
 * Content-Security-Policy header equals to <code>script-src [nonce random]</code>
 * html equals to "<script nonce='[random nonce value]'>console.log('This is a test');</script><style>h1{color:red;}</style>";
 *
 * - ContentSecurityPolicy.header equals to <code>style-src {style-src nonce}</code>: nonce value need to be calculated
 * Content-Security-Policy header equals to <code>style-src [nonce random]</code>
 * html equals to "<script>console.log('This is a test');</script><style nonce='[random nonce value]'>h1{color:red;}</style>";
 *
 * - ContentSecurityPolicy.header equals to <code>script-src {script-src nonce} style-src {style-src nonce}</code>: nonce value not need to be calculated
 * Content-Security-Policy header equals to <code>script-src [nonce random] style-src [nonce random]</code>
 * html equals to "<script nonce='[random nonce value]'>console.log('This is a test');</script><style nonce='[random nonce value]'>h1{color:red;}</style>";
 *
 * Where <code>random nonce value</code> is a random string encode with Base 64 algorithm.
 *
 * <p>Separately from site pages, {@link #addBackendHeader(HttpServletRequest, HttpServletResponse)}
 * writes a fixed policy on the back-end paths ({@code /dotAdmin}, {@code /api} and {@code /html}),
 * and {@link #addBackendHeader(HttpServletResponse)} writes it on the portal servlet's responses under
 * {@code /c}. It is configured by {@value #BACKEND_ENABLED}, {@value #BACKEND_POLICY} and
 * {@value #BACKEND_REPORT_ONLY}, and does not use the nonce placeholders.
 */
public class ContentSecurityPolicyUtil {

    final static Map<String, ContentSecurityPolicyResolver> contentSecurityPolicyResolvers;
    public static final int RANDOM_STRING_LENGTH = 20;

    private static final String NONCE_REQUEST_ATTRIBUTE = "NONCE_REQUEST_ATTRIBUTE";

    private static final Lazy<String> CONTENT_SECURITY_POLICY_CONFIG = Lazy.of(()->Config
            .getStringProperty("ContentSecurityPolicy.header", null));

    private static String overwriteContentSecurityPolicyConfig;

    /**
     * Turns the back-end policy on or off. On by default: responses on the back-end paths carry
     * the policy set in {@value #BACKEND_POLICY}, or {@link #DEFAULT_BACKEND_POLICY} when it is
     * not set. Off: they carry no policy header. Site pages are not affected either way.
     */
    public static final String BACKEND_ENABLED = "CONTENT_SECURITY_POLICY_BACKEND_ENABLED";

    /**
     * The policy sent on the back-end paths, replacing {@link #DEFAULT_BACKEND_POLICY}. A blank
     * value sends no policy.
     */
    public static final String BACKEND_POLICY = "CONTENT_SECURITY_POLICY_BACKEND_HEADER";

    /**
     * When true, the back-end policy is sent as {@code Content-Security-Policy-Report-Only}, so
     * browsers report what it would block without blocking it. Off by default. A report-only
     * trial mainly previews {@code object-src} and any fetch directives an override adds.
     * Browsers differ on whether they report {@code frame-ancestors} in report-only mode, and the
     * default policy has no {@code report-to}, so a quiet console does not show that a framing
     * change is safe.
     */
    public static final String BACKEND_REPORT_ONLY = "CONTENT_SECURITY_POLICY_BACKEND_REPORT_ONLY";

    /**
     * The back-end policy sent when {@value #BACKEND_POLICY} is not set: only dotCMS itself may
     * frame the back end, and plugin content ({@code object}, {@code embed}) does not load. Script,
     * style, image and connection sources are left open, because the legacy portlets rely on
     * inline scripts and the page editor shows site pages, with their own resources, inside the
     * back-end document.
     */
    public static final String DEFAULT_BACKEND_POLICY = "frame-ancestors 'self'; object-src 'none'";

    private static final List<String> BACKEND_PATH_PREFIXES = List.of("/dotAdmin", "/api", "/html");

    static {
        contentSecurityPolicyResolvers = Map.of(
                "{script-src nonce}", new ContentSecurityPolicyResolver(
                        (header) -> calculateNonceHeaderToScript(header),
                        (htmlCode) -> calculateNonceToScript(htmlCode)
                ),
                "{style-src nonce}", new ContentSecurityPolicyResolver(
                        (header) -> calculateNonceHeaderToStyle(header),
                        (htmlCode) -> calculateNonceToStyle(htmlCode)
                )
        );
    }

    private ContentSecurityPolicyUtil() {}

    @VisibleForTesting
    public static void overwriteConfigValue(final String value){
        overwriteContentSecurityPolicyConfig = value;
    }

    private static String calculateNonce() {
        final String randomAlphanumeric = RandomStringUtils.randomAlphanumeric(RANDOM_STRING_LENGTH);
        return Base64.encode(randomAlphanumeric.getBytes());
    }

    public static boolean isConfig() {
        final String contentSecurityPolicyHeader = getContentSecurityPolicyHeader();

        return UtilMethods.isSet(contentSecurityPolicyHeader);
    }

    private static String getContentSecurityPolicyHeader() {
        return UtilMethods.isSet(overwriteContentSecurityPolicyConfig) ? overwriteContentSecurityPolicyConfig :
                CONTENT_SECURITY_POLICY_CONFIG.get();
    }

    /**
     * Prepare the {@link HttpServletRequest} for the Content Security Policy features.
     * If a nonce value need to be calculated then it is calculated and save as a request's attribute.
     *
     * @see ContentSecurityPolicyUtil
     * @param request current request
     * @return
     */
    public static void init(final HttpServletRequest request) {
        if (shouldCalculateNonce()) {
            final String nonce = calculateNonce();
            request.setAttribute(NONCE_REQUEST_ATTRIBUTE, nonce);
        }
    }

    private static boolean shouldCalculateNonce() {
        final String contentSecurityPolicyHeader = getContentSecurityPolicyHeader();
        return shouldSetContentPolicyHeader() && contentSecurityPolicyHeader.contains("nonce");
    }

    private static boolean shouldSetContentPolicyHeader() {
        return PageMode.get() == PageMode.LIVE;
    }

    /**
     * Add the <code>Content-Security-Policy</code> header to the {@link HttpServletResponse}.
     * @see ContentSecurityPolicyUtil
     * @param response
     */
    public static void addHeader(final HttpServletResponse response) {
        if (isConfig() && shouldSetContentPolicyHeader()) {
            String contentSecurityPolicyHeader = getContentSecurityPolicyHeader();

            for (final Entry<String, ContentSecurityPolicyResolver> entry : contentSecurityPolicyResolvers.entrySet()) {
                if (contentSecurityPolicyHeader.contains(entry.getKey())) {
                    contentSecurityPolicyHeader = entry.getValue().headerResolver.apply(contentSecurityPolicyHeader);
                }
            }

            response.addHeader("Content-Security-Policy", contentSecurityPolicyHeader);
        }
    }

    /**
     * Return a html code using the nonce value calculated for the current request to the <code>htmlCodeParam</code>.
     * If the <code>ContentSecurityPolicy.header</code> config property not have any special value and
     * a nonce value not need to be calculated then the value returned is equals to <code>htmlCodeParam</code>.
     *
     * @see ContentSecurityPolicyUtil
     * @param htmlCodeParam
     * @return
     */
    public static String apply(final String htmlCodeParam) {
        String htmlCodeResult = htmlCodeParam;

        if (isConfig() && shouldSetContentPolicyHeader()) {
            String contentSecurityPolicyHeader = getContentSecurityPolicyHeader();

            for (final Entry<String, ContentSecurityPolicyResolver> entry : contentSecurityPolicyResolvers.entrySet()) {
                if (contentSecurityPolicyHeader.contains(entry.getKey())) {
                    htmlCodeResult = entry.getValue().htmlCodeResolver.apply(htmlCodeResult);
                }
            }
        }

        return htmlCodeResult;
    }

    /**
     * Writes the back-end policy to the response when the request is for a back-end path
     * ({@code /dotAdmin}, {@code /api} or {@code /html}, or anything below them) and the policy is
     * enabled. The path is read from the servlet path and path info, which the
     * container has already decoded and normalized, so an encoded spelling of a back-end path is
     * matched too. Any other request is left untouched, so site pages keep the policy configured
     * by {@code ContentSecurityPolicy.header}.
     *
     * <p>{@code /c} is not matched here: a site may have a root folder named {@code c}, and
     * {@code CMSFilter} serves its pages before the portal servlet mapped to {@code /c/*} is
     * reached. The portal servlet calls {@link #addBackendHeader(HttpServletResponse)} instead.
     *
     * @param request  the current request
     * @param response the response to write the header to
     */
    public static void addBackendHeader(final HttpServletRequest request,
            final HttpServletResponse response) {
        if (isBackendPath(request)) {
            addBackendHeader(response);
        }
    }

    /**
     * Writes the back-end policy to the response when it is enabled, without looking at the
     * path. For code that only ever produces back-end responses, such as the portal servlet.
     *
     * @param response the response to write the header to
     */
    public static void addBackendHeader(final HttpServletResponse response) {
        if (!readBackendFlag(BACKEND_ENABLED, true)) {
            return;
        }
        final String policy = Config.getStringProperty(BACKEND_POLICY, DEFAULT_BACKEND_POLICY);
        if (!UtilMethods.isSet(policy)) {
            return;
        }
        final String header = readBackendFlag(BACKEND_REPORT_ONLY, false)
                ? "Content-Security-Policy-Report-Only" : "Content-Security-Policy";
        response.setHeader(header, policy.trim());
    }

    /**
     * Reads one of the back-end policy switches. These are read on every back-end request, and
     * the raw string is checked here rather than through
     * {@link Config#getBooleanProperty(String, boolean)}, because that method throws for a bad
     * value from a properties file or environment variable but silently reads a bad value from
     * the system table (a typo such as {@code flase}) as false. Reading the string gives all
     * three sources the same rule: a missing or blank value is the default, {@code true} or
     * {@code false} (any case, surrounding spaces ignored) is used, and anything else uses the
     * default, which keeps the policy enforced, and logs a warning at most once a minute per
     * property. The request never fails.
     *
     * @param name     the property name
     * @param fallback the value to use when the property is missing or cannot be read
     * @return the configured value, or {@code fallback} when it is missing or cannot be read
     */
    private static boolean readBackendFlag(final String name, final boolean fallback) {
        final String value = Config.getStringProperty(name, null);
        if (!UtilMethods.isSet(value)) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(value.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(value.trim())) {
            return false;
        }
        Logger.warnEvery(ContentSecurityPolicyUtil.class, name,
                name + " is not true or false; using " + fallback, 60_000);
        return fallback;
    }

    /**
     * Tells whether the request's path is one of the back-end prefixes or lies below one.
     *
     * @param request the current request
     * @return true for {@code /api} and {@code /api/v1/sites}, false for {@code /application}
     */
    private static boolean isBackendPath(final HttpServletRequest request) {
        final String path = (request.getServletPath() == null ? "" : request.getServletPath())
                + (request.getPathInfo() == null ? "" : request.getPathInfo());
        return BACKEND_PATH_PREFIXES.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    private static String calculateNonceToScript(final String htmlCode) {

        final Optional<String> optionalNonce = getNonceFromCurrentRequest();
        return optionalNonce.isPresent() ?
                htmlCode.replaceAll(
                "<script", String.format("<script nonce='%s'", optionalNonce.get()))
                : htmlCode;
    }

    private static String calculateNonceHeaderToScript(final String header) {
        final Optional<String> optionalNonce = getNonceFromCurrentRequest();
        return optionalNonce.isPresent() ?
                header.replace("{script-src nonce}", String.format("'nonce-%s'", optionalNonce.get()))
                : header;
    }

    private static String calculateNonceToStyle(final String htmlCode) {

        final Optional<String> optionalNonce = getNonceFromCurrentRequest();
        return optionalNonce.isPresent() ?
                htmlCode.replace("<style", String.format("<style nonce='%s'", optionalNonce.get()))
                : htmlCode;
    }

    private static String calculateNonceHeaderToStyle(final String header) {
        final Optional<String> optionalNonce = getNonceFromCurrentRequest();
        return optionalNonce.isPresent() ?
                header.replace("{style-src nonce}", String.format("'nonce-%s'", optionalNonce.get()))
                : header;
    }

    private static Optional<String> getNonceFromCurrentRequest() {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        if (!UtilMethods.isSet(request)) {
            return Optional.empty();
        }

        final String nonce = (String) request.getAttribute(NONCE_REQUEST_ATTRIBUTE);
        return Optional.of(nonce);
    }

    private static class ContentSecurityPolicyResolver {
        final Function<String, String> headerResolver;
        final Function<String, String> htmlCodeResolver;

        public ContentSecurityPolicyResolver(
                final Function<String, String> headerResolver,
                final Function<String, String> htmlCodeResolver) {
            this.headerResolver = headerResolver;
            this.htmlCodeResolver = htmlCodeResolver;
        }
    }
}
