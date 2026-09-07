package com.dotcms.ai.client.langchain4j;

import com.dotmarketing.util.Logger;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;

import java.util.List;

/**
 * Verifies that a {@link ProviderConfig} actually works by building the LangChain4J model for the
 * requested {@link Capability} and issuing one minimal, real request against the provider.
 *
 * <p>Building the model already validates required fields (see {@link ModelProviderStrategy}),
 * so a missing {@code apiKey}/{@code model} fails fast with a clear message before any network
 * call is made. Anything past that — bad credentials, unreachable endpoint, unknown model — only
 * surfaces once the provider actually answers, hence the real call.
 */
public final class ProviderConnectionTester {

    private static final String TEST_PROMPT = "Reply with just the word: OK";
    private static final String TEST_EMBEDDING_INPUT = "dotCMS connection test";
    private static final String TEST_IMAGE_PROMPT = "a single red pixel on a white background";

    /**
     * Provider SDK exceptions (OpenAI, Bedrock, Google, etc.) often carry the full raw HTTP
     * response body in {@link Exception#getMessage()} — sometimes several KB of JSON. Capping the
     * length keeps the UI toast readable; the untruncated message is still recorded via
     * {@link Logger#warn} in {@link #test} for anyone debugging the actual failure.
     */
    private static final int MAX_MESSAGE_LENGTH = 200;
    private static final String TRUNCATION_SUFFIX = "…";

    /**
     * Upper bound applied to the test call when the posted config doesn't set {@code timeout}.
     * Every provider strategy honors {@code timeout} once set (see each strategy's {@code build*}
     * methods) except Vertex AI, which ignores it outright regardless of source — so this default
     * only ever narrows an otherwise-unbounded provider-SDK default, never overrides an explicit
     * value the caller supplied. Without this, an unreachable or slow {@code endpoint} could hold
     * the request thread open indefinitely, since the SDKs' own defaults vary by provider and
     * aren't all finite.
     */
    private static final int DEFAULT_TEST_TIMEOUT_SECONDS = 10;

    /**
     * Same purpose as {@link #DEFAULT_TEST_TIMEOUT_SECONDS}, but for {@link Capability#IMAGE}:
     * real image generation routinely takes well past 10s, so the chat/embeddings default would
     * fail a perfectly healthy provider before it ever finishes rendering.
     */
    private static final int DEFAULT_IMAGE_TEST_TIMEOUT_SECONDS = 60;

    private ProviderConnectionTester() {
    }

    /**
     * Tests the given provider configuration for the given capability.
     *
     * @param capability which capability section to test (chat, embeddings, image)
     * @param config     the provider configuration to test — same shape as one {@code providerConfig} section
     * @return a result carrying whether the call succeeded and a human-readable message
     */
    public static TestConnectionResult test(final Capability capability, final ProviderConfig config) {
        final ProviderConfig effectiveConfig = withDefaultTimeoutIfUnset(config, capability);
        try {
            final String detail = switch (capability) {
                case CHAT -> testChat(effectiveConfig);
                case EMBEDDINGS -> testEmbeddings(effectiveConfig);
                case IMAGE -> testImage(effectiveConfig);
            };
            return new TestConnectionResult(true, detail);
        } catch (final Exception e) {
            Logger.warn(ProviderConnectionTester.class,
                    "dotAI provider connection test failed for provider="
                            + config.provider() + ", capability=" + capability + ": " + e.getMessage());
            return new TestConnectionResult(false, friendlyMessage(e));
        }
    }

    /**
     * Returns {@code config} unchanged when it already sets a {@code timeout}, otherwise a copy
     * with a default applied — {@link #DEFAULT_IMAGE_TEST_TIMEOUT_SECONDS} for
     * {@link Capability#IMAGE}, {@link #DEFAULT_TEST_TIMEOUT_SECONDS} for every other capability
     * — scoped to this connection-test path only, so normal save/use of the configuration is
     * unaffected.
     */
    static ProviderConfig withDefaultTimeoutIfUnset(final ProviderConfig config, final Capability capability) {
        if (config.timeout() != null) {
            return config;
        }

        final int defaultSeconds = capability == Capability.IMAGE
                ? DEFAULT_IMAGE_TEST_TIMEOUT_SECONDS
                : DEFAULT_TEST_TIMEOUT_SECONDS;
        return ImmutableProviderConfig.copyOf(config).withTimeout(defaultSeconds);
    }

    private static String testChat(final ProviderConfig config) {
        final ChatModel model = LangChain4jModelFactory.buildChatModel(config);
        model.chat(ChatRequest.builder()
                .messages(List.of(UserMessage.from(TEST_PROMPT)))
                .build());
        return "Connection successful.";
    }

    private static String testEmbeddings(final ProviderConfig config) {
        final EmbeddingModel model = LangChain4jModelFactory.buildEmbeddingModel(config);
        model.embed(TextSegment.from(TEST_EMBEDDING_INPUT)).content();
        return "Connection successful.";
    }

    private static String testImage(final ProviderConfig config) {
        final ImageModel model = LangChain4jModelFactory.buildImageModel(config);
        model.generate(TEST_IMAGE_PROMPT).content();
        return "Connection successful. A test image was generated.";
    }

    /**
     * Reduces a provider exception to something short enough to show in a UI toast: collapses
     * whitespace/newlines into single spaces and caps the length, appending
     * {@value #TRUNCATION_SUFFIX} when text was cut. Falls back to the exception's simple class
     * name when there's no message at all. Provider-agnostic on purpose — it doesn't parse any
     * SDK's specific error shape, so it needs no per-provider maintenance.
     */
    static String friendlyMessage(final Exception e) {
        final String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }

        final String collapsed = message.trim().replaceAll("\\s+", " ");
        return collapsed.length() > MAX_MESSAGE_LENGTH
                ? collapsed.substring(0, MAX_MESSAGE_LENGTH).stripTrailing() + TRUNCATION_SUFFIX
                : collapsed;
    }

}
