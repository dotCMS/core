package com.dotcms.security;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.repackage.org.apache.axiom.om.util.Base64;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Lazy;
import org.apache.commons.lang.RandomStringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
 */
public class ContentSecurityPolicyUtil {

    final static Map<String, ContentSecurityPolicyResolver> contentSecurityPolicyResolvers;
    public static final int RANDOM_STRING_LENGTH = 20;

    private static final String NONCE_REQUEST_ATTRIBUTE = "NONCE_REQUEST_ATTRIBUTE";

    private static final Lazy<String> CONTENT_SECURITY_POLICY_CONFIG = Lazy.of(()->Config
            .getStringProperty("ContentSecurityPolicy.header", null));

    private static String overwriteContentSecurityPolicyConfig;

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
