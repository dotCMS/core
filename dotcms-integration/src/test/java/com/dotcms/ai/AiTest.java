package com.dotcms.ai;

import com.dotcms.ai.config.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotcms.util.WireMockTestHelper;
import com.dotmarketing.beans.Host;
import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.HashMap;
import java.util.Map;

public interface AiTest {

    String API_URL = "http://localhost:%d/c";
    String API_IMAGE_URL = "http://localhost:%d/i";
    String API_KEY = "some-api-key-1a2bc3";
    String MODEL = "gpt-3.5-turbo-16k";
    String IMAGE_MODEL = "dall-e-3";
    String IMAGE_SIZE = "1024x1024";
    int PORT = 50505;

    static AppConfig prepareConfig(final Host host, final WireMockServer wireMockServer) {
        return new AppConfig(host.getHostname(), appConfigMap(wireMockServer));
    }

    static AppConfig prepareCompletionConfig(final Host host, final WireMockServer wireMockServer) {
        return new AppConfig(host.getHostname(), completionAppConfigMap(appConfigMap(wireMockServer)));
    }

    static WireMockServer prepareWireMock() {
        final WireMockServer wireMockServer = WireMockTestHelper.wireMockServer(PORT);
        wireMockServer.start();

        return wireMockServer;
    }

    private static Map<String, Secret> completionAppConfigMap(final Map<String, Secret> configMap) {
        final Map<String, Secret> completionValues = Map.of(
                AppKeys.COMPLETION_ROLE_PROMPT.key,
                Secret.builder()
                        .withType(Type.STRING)
                        .withValue(AppKeys.COMPLETION_ROLE_PROMPT.defaultValue.toCharArray())
                        .build(),

                AppKeys.COMPLETION_TEXT_PROMPT.key,
                Secret.builder()
                        .withType(Type.STRING)
                        .withValue(AppKeys.COMPLETION_TEXT_PROMPT.defaultValue.toCharArray())
                        .build(),

                AppKeys.TEXT_MODEL_NAMES.key,
                Secret.builder()
                        .withType(Type.STRING)
                        .withValue(AppKeys.TEXT_MODEL_NAMES.defaultValue.toCharArray())
                        .build()
        );
        final Map<String, Secret> all = new HashMap<>(configMap);
        all.putAll(completionValues);
        return Map.copyOf(all);
    }

    static Map<String, Secret> appConfigMap(final WireMockServer wireMockServer) {
        return Map.of(
                AppKeys.API_URL.key,
                Secret.builder()
                        .withType(Type.STRING)
                        .withValue(String.format(API_URL, wireMockServer.port()).toCharArray())
                        .build(),

                AppKeys.API_IMAGE_URL.key,
                Secret.builder()
                        .withType(Type.STRING)
                        .withValue(String.format(API_IMAGE_URL, wireMockServer.port()).toCharArray())
                        .build(),

                AppKeys.API_KEY.key,
                Secret.builder().withType(Type.STRING).withValue(API_KEY.toCharArray()).build(),

                AppKeys.TEXT_MODEL_NAMES.key,
                Secret.builder().withType(Type.STRING).withValue(MODEL.toCharArray()).build(),

                AppKeys.IMAGE_MODEL_NAMES.key,
                Secret.builder().withType(Type.STRING).withValue(IMAGE_MODEL.toCharArray()).build(),

                AppKeys.IMAGE_SIZE.key,
                Secret.builder().withType(Type.SELECT).withValue(IMAGE_SIZE.toCharArray()).build(),

                AppKeys.LISTENER_INDEXER.key,
                Secret.builder()
                        .withType(Type.STRING)
                        .withValue("{\"default\":\"blog\"}".toCharArray())
                        .build());
    }
}
