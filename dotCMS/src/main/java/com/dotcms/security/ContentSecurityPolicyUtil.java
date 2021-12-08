package com.dotcms.security;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.repackage.org.apache.axiom.om.util.Base64;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.RandomStringUtils;

public class ContentSecurityPolicyUtil {

    final static Map<String, ContentSecurityPolicyCalculator> contentSecurityPolicyCalculators;
    public static final int RANDOM_STRING_LENGTH = 20;

    static {
        contentSecurityPolicyCalculators = map(
                "{script-src nonce}", (htmlCode, headerConfig) -> calculateNonceToScript(headerConfig, htmlCode),
                "{style-src nonce}", (htmlCode, headerConfig) -> calculateNonceToStyle(headerConfig, htmlCode)
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
    public static String calculateContentSecurityPolicy(final String htmlCode,
            final HttpServletResponse response) {
        String htmlCodeResult = htmlCode;
        String contentSecurityPolicyHeader = Config.getStringProperty(
                "ContentSecurityPolicy.header", null);

        if (UtilMethods.isSet(contentSecurityPolicyHeader)) {

            for (final Entry<String, ContentSecurityPolicyCalculator> entry : contentSecurityPolicyCalculators.entrySet()) {
                if (contentSecurityPolicyHeader.contains(entry.getKey())) {
                    final ContentSecurityPolicyData calculate = entry.getValue()
                            .calculate(htmlCodeResult, contentSecurityPolicyHeader);

                    htmlCodeResult = calculate.htmlCode;
                   contentSecurityPolicyHeader = calculate.headerValue;
                }
            }

            response.addHeader("Content-Security-Policy", contentSecurityPolicyHeader);
        }
        return htmlCodeResult;
    }

    private static String calculateNonce() {
        final String randomAlphanumeric = RandomStringUtils.randomAlphanumeric(RANDOM_STRING_LENGTH);
        return Base64.encode(randomAlphanumeric.getBytes());
    }

    private static class ContentSecurityPolicyData {

        String htmlCode;
        String headerValue;
    }

    private static ContentSecurityPolicyData calculateNonceToScript(final String contentSecurityPolicyConfig,
            final String htmlCode) {

        final ContentSecurityPolicyData contentSecurityPolicyData = new ContentSecurityPolicyData();
        final String nonce = calculateNonce();
        contentSecurityPolicyData.htmlCode = htmlCode.replaceAll("<script",
                String.format("<script nonce='%s'",nonce));

        contentSecurityPolicyData.headerValue = contentSecurityPolicyConfig.replace("{script-src nonce}",
                String.format("'nonce-%s'",nonce));

        return contentSecurityPolicyData;
    }

    private static ContentSecurityPolicyData calculateNonceToStyle(final String contentSecurityPolicyConfig,
            final String htmlCode) {

        final ContentSecurityPolicyData contentSecurityPolicyData = new ContentSecurityPolicyData();
        final String nonce = calculateNonce();
        contentSecurityPolicyData.htmlCode = htmlCode.replaceAll("<style",
                String.format("<style nonce='%s'",nonce));

        contentSecurityPolicyData.headerValue = contentSecurityPolicyConfig.replace("{style-src nonce}",
                String.format("'nonce-%s'",nonce));

        return contentSecurityPolicyData;
    }


    @FunctionalInterface
    interface ContentSecurityPolicyCalculator {
        ContentSecurityPolicyData calculate(final String contentSecurityPolicyConfig,
                final String htmlCode);
    }
}
