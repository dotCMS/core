package com.dotcms.security;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.repackage.org.apache.axiom.om.util.Base64;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.RandomStringUtils;

public class ContentSecurityPolicyUtil {

    final static Map<String, ContentSecurityPolicyResolver> contentSecurityPolicyResolvers;
    public static final int RANDOM_STRING_LENGTH = 20;

    private static String NONCE_REQUEST_ATTRIBUTE = "NONCE_REQUEST_ATTRIBUTE";

    static {
        contentSecurityPolicyResolvers = map(
                "{script-src nonce}", new ContentSecurityPolicyResolver(
                        (header) -> calculateNonceHeaderToScript(header),
                        (headerConfig, htmlCode) -> calculateNonceToScript(headerConfig, htmlCode)
                ),
                "{style-src nonce}", new ContentSecurityPolicyResolver(
                        (header) -> calculateNonceHeaderToStyle(header),
                        (headerConfig, htmlCode) -> calculateNonceToStyle(headerConfig, htmlCode)
                )
        );
    }

    private ContentSecurityPolicyUtil() {
    }

    /**
     *
     * Set the <code>Content-Security-Policy</code> header according to the
     * <code>ContentSecurityPolicy.header</code> config property, for example if the config property
     * has the follow value <code>default-src 'self'</code>, then the header is going to be set with the value
     *
     * The <code>ContentSecurityPolicy.header</code> config property has two special values:
     * - {script-src nonce}: You can use it like <code>script-src {script-src nonce}</code>, and a random
     * nonce is going to be calculated and set to each script block in <code>htmlCode</code>
     *
     * - {style-src nonce}: You can use it like <code>style-src {style-src nonce}</code>, and a random
     * nonce is going to be calculated and set to each style block in <code>htmlCode</code>
     *
     * When any of the special values to the <code>ContentSecurityPolicy.header</code> config property
     * is used then a new nonce value is calculated and set it to any script block or style block
     * in <code>htmlCode</code>, so if you have the follow html code:
     *
     * <pre>
     *     <script>
     *         console.log('This is a test');
     *     </script>
     *
     *     <h1>THIS IS A TEST</h1>
     * </pre>
     *
     * This method is going to return:
     *
     * <pre>
     *     <script nonce='[random nonce value]'">
     *         console.log('This is a test');
     *     </script>
     *
     *     <h1>THIS IS A TEST</h1>
     * </pre>
     *
     * Where <code>random nonce value</code> is a random string encode with Base 64 algorithm.
     *
     * @param htmlCode
     * @param response
     * @return
     */

    private static String calculateNonce() {
        final String randomAlphanumeric = RandomStringUtils.randomAlphanumeric(RANDOM_STRING_LENGTH);
        return Base64.encode(randomAlphanumeric.getBytes());
    }

    public static boolean isConfig() {
        final String contentSecurityPolicyHeader = getContentSecurityPolicyHeader();

        return UtilMethods.isSet(contentSecurityPolicyHeader);
    }

    private static String getContentSecurityPolicyHeader() {
        return Config.getStringProperty(
                "ContentSecurityPolicy.header", null);
    }

    public static void init(final HttpServletRequest request) {
        if (shouldCalculateNonce()) {
            final String nonce = calculateNonce();
            request.setAttribute(NONCE_REQUEST_ATTRIBUTE, nonce);
        }
    }

    private static boolean shouldCalculateNonce() {
        final String contentSecurityPolicyHeader = getContentSecurityPolicyHeader();
        final PageMode pageMode = PageMode.get();
        return pageMode == PageMode.LIVE && contentSecurityPolicyHeader.contains("nonce");
    }

    public static void addHeader(final HttpServletResponse response) {
        if (isConfig()) {
            String contentSecurityPolicyHeader = getContentSecurityPolicyHeader();

            for (final Entry<String, ContentSecurityPolicyResolver> entry : contentSecurityPolicyResolvers.entrySet()) {
                if (contentSecurityPolicyHeader.contains(entry.getKey())) {
                    contentSecurityPolicyHeader = entry.getValue().headerResolver.resolve(contentSecurityPolicyHeader);
                }
            }

            response.addHeader("Content-Security-Policy", contentSecurityPolicyHeader);
        }
    }

    public static String apply(final String htmlCodeParam) {
        String htmlCodeResult = htmlCodeParam;

        if (isConfig()) {
            String contentSecurityPolicyHeader = getContentSecurityPolicyHeader();

            for (final Entry<String, ContentSecurityPolicyResolver> entry : contentSecurityPolicyResolvers.entrySet()) {
                if (contentSecurityPolicyHeader.contains(entry.getKey())) {
                    htmlCodeResult = entry.getValue().htmlCodeResolver
                            .resolve(contentSecurityPolicyHeader, htmlCodeResult);
                }
            }
        }

        return htmlCodeResult;
    }

    private static String calculateNonceToScript(final String contentSecurityPolicyConfig,
            final String htmlCode) {

        final String nonce = getNonceFromCurrentRequest();
        return htmlCode.replaceAll("<script", String.format("<script nonce='%s'",nonce));
    }

    private static String calculateNonceHeaderToScript(final String header) {
        final String nonce = getNonceFromCurrentRequest();
        return header.replace("{script-src nonce}", String.format("'nonce-%s'",nonce));
    }

    private static String calculateNonceToStyle(final String contentSecurityPolicyConfig,
            final String htmlCode) {

        final String nonce = getNonceFromCurrentRequest();
        return htmlCode.replaceAll("<style", String.format("<style nonce='%s'",nonce));
    }

    private static String calculateNonceHeaderToStyle(final String header) {
        final String nonce = getNonceFromCurrentRequest();
        return header.replace("{style-src nonce}", String.format("'nonce-%s'",nonce));
    }

    private static String getNonceFromCurrentRequest() {
        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        final String nonce = (String) request.getAttribute(NONCE_REQUEST_ATTRIBUTE);
        return nonce;
    }

    @FunctionalInterface
    interface HeaderResolver {
        String resolve(final String header);
    }

    @FunctionalInterface
    interface HtmlCodeResolver {
        String resolve(final String contentSecurityPolicyConfig, final String htmlCode);
    }

    private static class ContentSecurityPolicyResolver {
        HeaderResolver headerResolver;
        HtmlCodeResolver htmlCodeResolver;

        public ContentSecurityPolicyResolver(
                HeaderResolver headerResolver,
                HtmlCodeResolver htmlCodeResolver) {
            this.headerResolver = headerResolver;
            this.htmlCodeResolver = htmlCodeResolver;
        }
    }
}
