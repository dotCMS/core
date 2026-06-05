package com.dotcms.ai.client.langchain4j;

import dev.langchain4j.model.bedrock.BedrockCohereEmbeddingModel;
import dev.langchain4j.model.bedrock.BedrockTitanEmbeddingModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link BedrockModelProviderStrategy}.
 *
 * <p>The strategy is package-private, so these tests live in the same package and instantiate it
 * directly. Constructing a Bedrock model (chat, streaming, or embedding) does not call AWS: the
 * underlying {@code BedrockRuntimeClient}/{@code BedrockRuntimeAsyncClient} resolve credentials and
 * endpoints lazily on first invocation, so a config with static dummy keys (or none at all) builds
 * cleanly in a CI-like environment without network access or real credentials.
 */
public class BedrockModelProviderStrategyTest {

    private static final String MODEL_TYPE = "chat";

    private final BedrockModelProviderStrategy strategy = new BedrockModelProviderStrategy();

    // ── providerName ─────────────────────────────────────────────────────────

    /**
     * Given the Bedrock strategy,
     * When providerName is called,
     * Then it returns "bedrock".
     */
    @Test
    public void test_providerName_returnsBedrock() {
        assertTrue("bedrock".equals(strategy.providerName()));
    }

    // ── buildChatModel ─────────────────────────────────────────────────────────

    /**
     * Given a full Bedrock config with static keys,
     * When buildChatModel is called,
     * Then a ChatModel is returned.
     */
    @Test
    public void test_buildChatModel_fullStaticKeyConfig_returnsModel() {
        final ChatModel model = strategy.buildChatModel(bedrockConfig("openai.gpt-oss-120b-1:0"), MODEL_TYPE);
        assertNotNull(model);
    }

    /**
     * Given a Bedrock config with no credentials (default credential chain path),
     * When buildChatModel is called,
     * Then a ChatModel is returned.
     */
    @Test
    public void test_buildChatModel_noKeys_defaultChain_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("us.deepseek.r1-v1:0")
                .region("us-east-1")
                .build();
        assertNotNull(strategy.buildChatModel(config, MODEL_TYPE));
    }

    /**
     * Given a Bedrock config with only an accessKeyId (secretAccessKey absent),
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown with the both-or-neither message.
     */
    @Test
    public void test_buildChatModel_onlyKeyId_throwsBothOrNeither() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("openai.gpt-oss-120b-1:0")
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .build();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> strategy.buildChatModel(config, MODEL_TYPE));
        assertTrue(ex.getMessage().contains("both be set or both be absent"));
    }

    /**
     * Given a Bedrock config with only a secretAccessKey (accessKeyId absent),
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown with the both-or-neither message.
     */
    @Test
    public void test_buildChatModel_onlySecret_throwsBothOrNeither() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("openai.gpt-oss-120b-1:0")
                .region("us-east-1")
                .secretAccessKey("test-secret-key")
                .build();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> strategy.buildChatModel(config, MODEL_TYPE));
        assertTrue(ex.getMessage().contains("both be set or both be absent"));
    }

    /**
     * Given a Bedrock config without a region,
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown naming the region field and modelType.
     */
    @Test
    public void test_buildChatModel_missingRegion_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("openai.gpt-oss-120b-1:0")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .build();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> strategy.buildChatModel(config, MODEL_TYPE));
        assertTrue(ex.getMessage().contains("region"));
        assertTrue(ex.getMessage().contains(MODEL_TYPE));
    }

    /**
     * Given a Bedrock config without a model,
     * When buildChatModel is called,
     * Then an IllegalArgumentException is thrown naming the model field and modelType.
     */
    @Test
    public void test_buildChatModel_missingModel_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .build();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> strategy.buildChatModel(config, MODEL_TYPE));
        assertTrue(ex.getMessage().contains("model"));
        assertTrue(ex.getMessage().contains(MODEL_TYPE));
    }

    /**
     * Given a Bedrock config with temperature and maxTokens set,
     * When buildChatModel is called,
     * Then a ChatModel is returned (the defaultRequestParameters branch is exercised).
     */
    @Test
    public void test_buildChatModel_withTemperatureAndMaxTokens_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("us.deepseek.r1-v1:0")
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .temperature(0.7)
                .maxTokens(512)
                .build();
        assertNotNull(strategy.buildChatModel(config, MODEL_TYPE));
    }

    /**
     * Given a Bedrock config with no temperature or maxTokens,
     * When buildChatModel is called,
     * Then a ChatModel is returned (the defaultRequestParameters branch is skipped).
     */
    @Test
    public void test_buildChatModel_withoutTemperatureOrMaxTokens_returnsModel() {
        assertNotNull(strategy.buildChatModel(bedrockConfig("us.deepseek.r1-v1:0"), MODEL_TYPE));
    }

    /**
     * Given a Bedrock config with timeout and maxRetries set,
     * When buildChatModel is called,
     * Then a ChatModel is returned (the client override-configuration branch is exercised).
     */
    @Test
    public void test_buildChatModel_withTimeoutAndMaxRetries_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("openai.gpt-oss-120b-1:0")
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .timeout(30)
                .maxRetries(2)
                .build();
        assertNotNull(strategy.buildChatModel(config, MODEL_TYPE));
    }

    /**
     * Given a Bedrock config with only timeout set,
     * When buildChatModel is called,
     * Then a ChatModel is returned.
     */
    @Test
    public void test_buildChatModel_withTimeoutOnly_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("us.deepseek.r1-v1:0")
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .timeout(15)
                .build();
        assertNotNull(strategy.buildChatModel(config, MODEL_TYPE));
    }

    /**
     * Given a Bedrock config with only maxRetries set,
     * When buildChatModel is called,
     * Then a ChatModel is returned.
     */
    @Test
    public void test_buildChatModel_withMaxRetriesOnly_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("us.deepseek.r1-v1:0")
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .maxRetries(4)
                .build();
        assertNotNull(strategy.buildChatModel(config, MODEL_TYPE));
    }

    /**
     * Given a Bedrock config with only an accessKeyId (secretAccessKey absent),
     * When buildStreamingChatModel is called,
     * Then an IllegalArgumentException is thrown with the both-or-neither message.
     */
    @Test
    public void test_buildStreamingChatModel_onlyKeyId_throwsBothOrNeither() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("us.deepseek.r1-v1:0")
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .build();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> strategy.buildStreamingChatModel(config, MODEL_TYPE));
        assertTrue(ex.getMessage().contains("both be set or both be absent"));
    }

    /**
     * Given a Bedrock config with only a secretAccessKey (accessKeyId absent),
     * When buildStreamingChatModel is called,
     * Then an IllegalArgumentException is thrown with the both-or-neither message.
     */
    @Test
    public void test_buildStreamingChatModel_onlySecret_throwsBothOrNeither() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("us.deepseek.r1-v1:0")
                .region("us-east-1")
                .secretAccessKey("test-secret-key")
                .build();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> strategy.buildStreamingChatModel(config, MODEL_TYPE));
        assertTrue(ex.getMessage().contains("both be set or both be absent"));
    }

    // ── buildStreamingChatModel ──────────────────────────────────────────────

    /**
     * Given a full Bedrock config,
     * When buildStreamingChatModel is called,
     * Then a StreamingChatModel is returned.
     */
    @Test
    public void test_buildStreamingChatModel_fullConfig_returnsModel() {
        final StreamingChatModel model =
                strategy.buildStreamingChatModel(bedrockConfig("us.deepseek.r1-v1:0"), MODEL_TYPE);
        assertNotNull(model);
    }

    /**
     * Given a Bedrock config with temperature and maxTokens set,
     * When buildStreamingChatModel is called,
     * Then a StreamingChatModel is returned.
     */
    @Test
    public void test_buildStreamingChatModel_withParams_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("us.deepseek.r1-v1:0")
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .temperature(0.5)
                .maxTokens(256)
                .build();
        assertNotNull(strategy.buildStreamingChatModel(config, MODEL_TYPE));
    }

    /**
     * Given a Bedrock config with timeout and maxRetries set,
     * When buildStreamingChatModel is called,
     * Then a StreamingChatModel is returned (the async client override-configuration branch is exercised).
     */
    @Test
    public void test_buildStreamingChatModel_withTimeoutAndMaxRetries_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("us.deepseek.r1-v1:0")
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .timeout(20)
                .maxRetries(3)
                .build();
        assertNotNull(strategy.buildStreamingChatModel(config, MODEL_TYPE));
    }

    // ── buildEmbeddingModel ──────────────────────────────────────────────────

    /**
     * Given a Bedrock config with a cohere.* model,
     * When buildEmbeddingModel is called,
     * Then a BedrockCohereEmbeddingModel is returned.
     */
    @Test
    public void test_buildEmbeddingModel_cohere_returnsCohereModel() {
        final EmbeddingModel model = strategy.buildEmbeddingModel(bedrockConfig("cohere.embed-english-v3"), "embeddings");
        assertNotNull(model);
        assertTrue(model instanceof BedrockCohereEmbeddingModel);
    }

    /**
     * Given a Bedrock config with an amazon.titan-* model,
     * When buildEmbeddingModel is called,
     * Then a BedrockTitanEmbeddingModel is returned.
     */
    @Test
    public void test_buildEmbeddingModel_titan_returnsTitanModel() {
        final EmbeddingModel model =
                strategy.buildEmbeddingModel(bedrockConfig("amazon.titan-embed-text-v2:0"), "embeddings");
        assertNotNull(model);
        assertTrue(model instanceof BedrockTitanEmbeddingModel);
    }

    /**
     * Given a Bedrock config with an upper/mixed-case amazon.titan model id,
     * When buildEmbeddingModel is called,
     * Then routing is case-insensitive and a BedrockTitanEmbeddingModel is returned.
     */
    @Test
    public void test_buildEmbeddingModel_titanMixedCase_routesToTitan() {
        final EmbeddingModel model =
                strategy.buildEmbeddingModel(bedrockConfig("AMAZON.TITAN-embed-text-v2:0"), "embeddings");
        assertNotNull(model);
        assertTrue(model instanceof BedrockTitanEmbeddingModel);
    }

    /**
     * Given a Bedrock config with an upper-case cohere model id,
     * When buildEmbeddingModel is called,
     * Then routing is case-insensitive and a BedrockCohereEmbeddingModel is returned.
     */
    @Test
    public void test_buildEmbeddingModel_cohereUpperCase_routesToCohere() {
        final EmbeddingModel model =
                strategy.buildEmbeddingModel(bedrockConfig("COHERE.embed-english-v3"), "embeddings");
        assertNotNull(model);
        assertTrue(model instanceof BedrockCohereEmbeddingModel);
    }

    /**
     * Given a Bedrock config with a cohere model and an explicit embeddingInputType,
     * When buildEmbeddingModel is called,
     * Then a cohere embedding model is constructed without error.
     */
    @Test
    public void test_buildEmbeddingModel_cohereWithInputType_returnsModel() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("cohere.embed-english-v3")
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .embeddingInputType("search_query")
                .build();
        final EmbeddingModel model = strategy.buildEmbeddingModel(config, "embeddings");
        assertNotNull(model);
        assertTrue(model instanceof BedrockCohereEmbeddingModel);
    }

    /**
     * Given a Bedrock config without a region,
     * When buildEmbeddingModel is called,
     * Then an IllegalArgumentException is thrown naming the region field and modelType.
     */
    @Test
    public void test_buildEmbeddingModel_missingRegion_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model("cohere.embed-english-v3")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .build();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> strategy.buildEmbeddingModel(config, "embeddings"));
        assertTrue(ex.getMessage().contains("region"));
        assertTrue(ex.getMessage().contains("embeddings"));
    }

    /**
     * Given a Bedrock config without a model,
     * When buildEmbeddingModel is called,
     * Then an IllegalArgumentException is thrown naming the model field and modelType.
     */
    @Test
    public void test_buildEmbeddingModel_missingModel_throws() {
        final ProviderConfig config = ImmutableProviderConfig.builder()
                .provider("bedrock")
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .build();
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> strategy.buildEmbeddingModel(config, "embeddings"));
        assertTrue(ex.getMessage().contains("model"));
        assertTrue(ex.getMessage().contains("embeddings"));
    }

    /**
     * Given a Bedrock config with an unsupported embedding family,
     * When buildEmbeddingModel is called,
     * Then an IllegalArgumentException is thrown listing the supported families.
     */
    @Test
    public void test_buildEmbeddingModel_unsupportedFamily_throws() {
        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> strategy.buildEmbeddingModel(bedrockConfig("openai.gpt-oss-120b-1:0"), "embeddings"));
        assertTrue(ex.getMessage().contains("cohere."));
        assertTrue(ex.getMessage().contains("amazon.titan-"));
    }

    // ── buildImageModel ──────────────────────────────────────────────────────

    /**
     * Given any Bedrock config,
     * When buildImageModel is called,
     * Then an UnsupportedOperationException is thrown.
     */
    @Test
    public void test_buildImageModel_throwsUnsupported() {
        assertThrows(UnsupportedOperationException.class,
                () -> strategy.buildImageModel(bedrockConfig("stability.stable-diffusion-xl-v1"), "image"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ProviderConfig bedrockConfig(final String model) {
        return ImmutableProviderConfig.builder()
                .provider("bedrock")
                .model(model)
                .region("us-east-1")
                .accessKeyId("test-access-key")
                .secretAccessKey("test-secret-key")
                .build();
    }

}
