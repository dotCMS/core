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

            response.addHeader("Content-Security-Policy",
                    String.format("script-src 'nonce-%s'", contentSecurityPolicyHeader));
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
                String.format("nonce-%s",nonce));

        return contentSecurityPolicyData;
    }

    private static ContentSecurityPolicyData calculateNonceToStyle(final String contentSecurityPolicyConfig,
            final String htmlCode) {

        final ContentSecurityPolicyData contentSecurityPolicyData = new ContentSecurityPolicyData();
        final String nonce = calculateNonce();
        contentSecurityPolicyData.htmlCode = htmlCode.replaceAll("<style",
                String.format("<style nonce='%s'",nonce));

        contentSecurityPolicyData.headerValue = contentSecurityPolicyConfig.replace("{style-src nonce}",
                String.format("nonce-%s",nonce));

        return contentSecurityPolicyData;
    }


    @FunctionalInterface
    interface ContentSecurityPolicyCalculator {
        ContentSecurityPolicyData calculate(final String contentSecurityPolicyConfig,
                final String htmlCode);
    }
}
