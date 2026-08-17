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
        try {
            final String detail = switch (capability) {
                case CHAT -> testChat(config);
                case EMBEDDINGS -> testEmbeddings(config);
                case IMAGE -> testImage(config);
            };
            return new TestConnectionResult(true, detail);
        } catch (final Exception e) {
            Logger.warn(ProviderConnectionTester.class,
                    "dotAI provider connection test failed for provider="
                            + config.provider() + ", capability=" + capability + ": " + e.getMessage());
            return new TestConnectionResult(false, friendlyMessage(e));
        }
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

    private static String friendlyMessage(final Exception e) {
        final String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

}
