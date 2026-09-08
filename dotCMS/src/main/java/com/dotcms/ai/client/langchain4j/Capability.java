package com.dotcms.ai.client.langchain4j;

/**
 * The AI capabilities a {@code providerConfig} section can configure independently: chat,
 * embeddings, and image generation. Not every {@link ModelProviderStrategy} supports every
 * capability — see {@link ModelProviderStrategy#supportedCapabilities()}.
 */
public enum Capability {
    CHAT,
    EMBEDDINGS,
    IMAGE
}
