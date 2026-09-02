package com.dotcms.ai;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.ai.app.ConfigService;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotcms.util.WireMockTestHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public interface AiTest {

    String API_URL = "http://localhost:%d/c";
    String API_IMAGE_URL = "http://localhost:%d/i";
    String API_EMBEDDINGS_URL = "http://localhost:%d/e";
    String API_KEY = "some-api-key-1a2bc3";
    String MODEL = "gpt-3.5-turbo-16k";
    String IMAGE_MODEL = "dall-e-3";
    String EMBEDDINGS_MODEL = "text-embedding-ada-002";
    String IMAGE_SIZE = "1024x1024";
    int PORT = 50505;

    static WireMockServer prepareWireMock() {
        final WireMockServer wireMockServer = WireMockTestHelper.wireMockServer(PORT);
        wireMockServer.start();

        return wireMockServer;
    }

    static void removeAiAppSecrets(final Host host) throws Exception {
        APILocator.getAppsAPI().deleteSecrets(AppKeys.APP_KEY, host, APILocator.systemUser());
    }

    static String providerConfigJson(final int port, final String chatModel) {
        final String endpoint = String.format("http://localhost:%d/", port);
        return String.format(
                "{" +
                "\"chat\":{\"provider\":\"openai\",\"apiKey\":\"%s\",\"model\":\"%s\",\"endpoint\":\"%s\",\"maxRetries\":0}," +
                "\"embeddings\":{\"provider\":\"openai\",\"apiKey\":\"%s\",\"model\":\"%s\",\"endpoint\":\"%s\",\"maxRetries\":0}," +
                "\"image\":{\"provider\":\"openai\",\"apiKey\":\"%s\",\"model\":\"%s\",\"endpoint\":\"%s\",\"maxRetries\":0}," +
                "\"settings\":{\"listenerIndexer\":{\"default\":\"blog\"}}" +
                "}",
                API_KEY, chatModel, endpoint,
                API_KEY, EMBEDDINGS_MODEL, endpoint,
                API_KEY, IMAGE_MODEL, endpoint);
    }

    static Map<String, Secret> aiAppSecretsWithProviderConfig(
            final Host host, final String providerConfigJson) throws Exception {
        final AppSecrets appSecrets = new AppSecrets.Builder()
                .withKey(AppKeys.APP_KEY)
                .withSecret(AppKeys.PROVIDER_CONFIG.key, providerConfigJson)
                .build();
        APILocator.getAppsAPI().saveSecrets(appSecrets, host, APILocator.systemUser());
        await().atMost(5, SECONDS).until(() -> ConfigService.INSTANCE.config(host).isEnabled());
        return appSecrets.getSecrets();
    }

}
