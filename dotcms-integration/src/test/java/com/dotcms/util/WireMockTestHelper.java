package com.dotcms.util;

import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Helper class for WireMock tests.
 *
 * @author vico
 */
public class WireMockTestHelper {

    private WireMockTestHelper() {}

    public static WireMockServer wireMockServer(final Integer port) {
        return new WireMockServer(
                Optional.ofNullable(port)
                        .map(p -> options().port(p))
                        .orElse(options().dynamicPort())
                        .jettyAcceptors(6)
                        .asynchronousResponseEnabled(true));
    }

}
