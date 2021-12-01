package com.dotcms.security;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.repackage.org.apache.axiom.om.util.Base64;
import com.dotcms.util.RandomString;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletResponse;

public class ContentSecurityPolicyUtil {

    final static Map<String, ContentSecurityPolicyCalculator> contentSecurityPolicyCalculators;
    public static final int RANDOM_STRING_LENGTH = 20;

    static {
        contentSecurityPolicyCalculators = map(
                "{script-src nonce}", (htmlCode, headerConfig) -> calculateNonceToScript(headerConfig, htmlCode),
                "{style-src nonce}", (htmlCode, headerConfig) -> calculateNonceToStyle(headerConfig, htmlCode),
                "{script-src hash}", (htmlCode, headerConfig) -> calculateHashToScript(headerConfig, htmlCode)

        );
    }

    private ContentSecurityPolicyUtil() {
    }

    public static String calculateContentSecurityPolicy(final String htmlCode,
            final HttpServletResponse response) {
        String htmlCodeResult = htmlCode;
        //To use the Content Security Policy set the properties ContentSecurityPolicy.header in the config file
        //like default-src 'self'
        // To use nonce set like script-src {script-src nonce} or style-src {style-src nonce}
        // To use hash set like script-src {script-src hash}
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

            response.addHeader("Content-Security-Policy",
                    String.format("script-src 'nonce-%s'", contentSecurityPolicyHeader));
        }
        return htmlCodeResult;
    }

    private static String calculateNonce() {
        return Base64.encode(
                RandomString.getAlphaNumericString(RANDOM_STRING_LENGTH).getBytes());
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

        contentSecurityPolicyData.headerValue = contentSecurityPolicyConfig.replace("{nonce}",
                String.format("nonce-%s",nonce));

        return contentSecurityPolicyData;
    }

    private static ContentSecurityPolicyData calculateNonceToStyle(final String contentSecurityPolicyConfig,
            final String htmlCode) {

        final ContentSecurityPolicyData contentSecurityPolicyData = new ContentSecurityPolicyData();
        final String nonce = calculateNonce();
        contentSecurityPolicyData.htmlCode = htmlCode.replaceAll("<style",
                String.format("<style nonce='%s'",nonce));

        contentSecurityPolicyData.headerValue = contentSecurityPolicyConfig.replace("{nonce}",
                String.format("nonce-%s",nonce));

        return contentSecurityPolicyData;
    }

    private static ContentSecurityPolicyData calculateHashToScript(final String contentSecurityPolicyConfig,
            final String htmlCode) {

        final ContentSecurityPolicyData contentSecurityPolicyData = new ContentSecurityPolicyData();
        contentSecurityPolicyData.htmlCode = htmlCode;

        final String scriptHash = ""; //calculateHash(htmlCode);
        contentSecurityPolicyData.headerValue = contentSecurityPolicyConfig.replace("{hash}",
                String.format("sha256-%s", scriptHash));

        return contentSecurityPolicyData;
    }

    @FunctionalInterface
    interface ContentSecurityPolicyCalculator {
        ContentSecurityPolicyData calculate(final String contentSecurityPolicyConfig,
                final String htmlCode);
    }
}
