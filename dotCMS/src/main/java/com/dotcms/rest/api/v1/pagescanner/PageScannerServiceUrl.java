package com.dotcms.rest.api.v1.pagescanner;

import com.dotcms.util.network.IPUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Checks the Page Scanner service URL read from the {@code dotPageScanner-config} App before
 * anything is sent to it. The Page Scanner and A11y Agent resources both call it before they
 * create the short-lived token that travels with their requests to that service.
 */
public final class PageScannerServiceUrl {

    private static final String ALLOW_ACCESS_TO_PRIVATE_SUBNETS = "ALLOW_ACCESS_TO_PRIVATE_SUBNETS";

    private PageScannerServiceUrl() {
    }

    /**
     * Returns the service base URL when it may be used, or empty when it may not.
     *
     * <p>The URL must be an absolute {@code https} URL with a host and no user info, query or
     * fragment, so that a path appended to it stays on the same host. The host must not be, or
     * resolve to, a loopback, link-local, private or otherwise internal address unless
     * {@code ALLOW_ACCESS_TO_PRIVATE_SUBNETS} is {@code true}; a host that does not resolve is
     * refused. A refused URL is logged without its value.</p>
     *
     * @param apiUrl the configured base URL of the service
     * @return the base URL without a trailing slash, or empty when it is refused
     */
    public static Optional<String> validate(final String apiUrl) {
        if (!UtilMethods.isSet(apiUrl)) {
            return Optional.empty();
        }
        final URI uri;
        try {
            uri = new URI(apiUrl);
        } catch (final URISyntaxException e) {
            return refuse("it is not a valid URL");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !UtilMethods.isSet(uri.getHost())
                || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            return refuse("it must be an https URL with a host and no user info, query or fragment");
        }
        if (!Config.getBooleanProperty(ALLOW_ACCESS_TO_PRIVATE_SUBNETS, false)
                && IPUtils.isIpPrivateSubnet(uri.getHost())) {
            return refuse("its host is, or resolves to, a private or internal address, which "
                    + ALLOW_ACCESS_TO_PRIVATE_SUBNETS + " would allow");
        }
        return Optional.of(apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl);
    }

    /**
     * Logs why the configured URL is not used and returns the empty result.
     *
     * @param reason what the URL lacks, without the URL itself
     * @return an empty {@link Optional}
     */
    private static Optional<String> refuse(final String reason) {
        Logger.warn(PageScannerServiceUrl.class,
                "The Page Scanner App apiUrl is not used: " + reason + ".");
        return Optional.empty();
    }
}
