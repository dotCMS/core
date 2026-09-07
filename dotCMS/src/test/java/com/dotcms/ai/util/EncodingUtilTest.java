package com.dotcms.ai.util;

import com.dotcms.ai.app.AIModelType;
import com.dotcms.ai.app.AppConfig;
import com.dotcms.ai.app.AppKeys;
import com.dotcms.security.apps.Secret;
import com.knuddels.jtokkit.api.Encoding;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EncodingUtilTest {

    @Test
    public void test_getEncoding_knownModel_returnsEncoding() {
        final AppConfig config = buildAppConfig("gpt-4o-mini");

        final Optional<Encoding> encoding = EncodingUtil.get().getEncoding(config, AIModelType.TEXT);

        assertTrue("Known model gpt-4o-mini should resolve to a valid encoding", encoding.isPresent());
    }

    @Test
    public void test_getEncoding_unknownModel_returnsEmpty() {
        final AppConfig config = buildAppConfig("gpt-future-unknown-model");

        final Optional<Encoding> encoding = EncodingUtil.get().getEncoding(config, AIModelType.TEXT);

        assertFalse("Unknown model should return empty Optional, not throw", encoding.isPresent());
    }

    @Test
    public void test_getEncoding_unknownModelWithKnownFallback_returnsFallbackEncoding() {
        final AppConfig config = buildAppConfig("gpt-future-unknown-model,gpt-4o-mini");

        final Optional<Encoding> encoding = EncodingUtil.get().getEncoding(config, AIModelType.TEXT);

        assertTrue("Should fall back to gpt-4o-mini encoding when primary model is unknown", encoding.isPresent());
    }

    private static AppConfig buildAppConfig(final String model) {
        final String providerConfig =
                "{\"chat\":{\"provider\":\"openai\",\"apiKey\":\"sk-test\",\"model\":\"" + model + "\"}}";
        final Map<String, Secret> secrets = new HashMap<>();
        final Secret providerConfigSecret = mock(Secret.class);
        when(providerConfigSecret.getString()).thenReturn(providerConfig);
        secrets.put(AppKeys.PROVIDER_CONFIG.key, providerConfigSecret);
        return new AppConfig("localhost", secrets);
    }

}
