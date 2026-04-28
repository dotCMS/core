package com.dotcms.ai;

import com.dotcms.ai.app.AppKeys;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotcms.util.WireMockTestHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.github.tomakehurst.wiremock.WireMockServer;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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

    static Map<String, Secret> aiAppSecrets(final Host host,
                                            final String apiKey,
                                            final String textModels,
                                            final String imageModels,
                                            final String embeddingsModel) throws Exception {
        final AppSecrets.Builder builder = new AppSecrets.Builder()
                .withKey(AppKeys.APP_KEY)
                .withSecret(AppKeys.API_URL.key, String.format(API_URL, PORT))
                .withSecret(AppKeys.API_IMAGE_URL.key, String.format(API_IMAGE_URL, PORT))
                .withSecret(AppKeys.API_EMBEDDINGS_URL.key, String.format(API_EMBEDDINGS_URL, PORT))
                .withHiddenSecret(AppKeys.API_KEY.key, apiKey)
                .withSecret(AppKeys.IMAGE_SIZE.key, IMAGE_SIZE)
                .withSecret(AppKeys.LISTENER_INDEXER.key, "{\"default\":\"blog\"}")
                .withSecret(AppKeys.COMPLETION_ROLE_PROMPT.key, AppKeys.COMPLETION_ROLE_PROMPT.defaultValue)
                .withSecret(AppKeys.COMPLETION_TEXT_PROMPT.key, AppKeys.COMPLETION_TEXT_PROMPT.defaultValue);

        if (Objects.nonNull(textModels)) {
            builder.withSecret(AppKeys.TEXT_MODEL_NAMES.key, textModels);
        }
        if (Objects.nonNull(imageModels)) {
            builder.withSecret(AppKeys.IMAGE_MODEL_NAMES.key, imageModels);
        }
        if (Objects.nonNull(embeddingsModel)) {
            builder.withSecret(AppKeys.EMBEDDINGS_MODEL_NAMES.key, embeddingsModel);
        }

        final AppSecrets appSecrets = builder.build();
        APILocator.getAppsAPI().saveSecrets(appSecrets, host, APILocator.systemUser());
        TimeUnit.SECONDS.sleep(1);
        return appSecrets.getSecrets();
    }

    static Map<String, Secret> aiAppSecrets(final Host host, final String apiKey) throws Exception {
        return aiAppSecrets(host, apiKey, MODEL, IMAGE_MODEL, EMBEDDINGS_MODEL);
    }

    static Map<String, Secret> aiAppSecrets(final Host host,
                                            final String textModels,
                                            final String imageModels,
                                            final String embeddingsModel) throws Exception {
        return aiAppSecrets(host, API_KEY, textModels, imageModels, embeddingsModel);
    }

    static Map<String, Secret> aiAppSecrets(final Host host) throws Exception {

        return aiAppSecrets(host, MODEL, IMAGE_MODEL, EMBEDDINGS_MODEL);
    }

    static void removeAiAppSecrets(final Host host) throws Exception {
        APILocator.getAppsAPI().deleteSecrets(AppKeys.APP_KEY, host, APILocator.systemUser());
    }

}
