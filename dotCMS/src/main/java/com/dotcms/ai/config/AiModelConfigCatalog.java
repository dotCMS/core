package com.dotcms.ai.config;

import java.util.List;

/**
 * Model Config Catalog
 * Returns the ModelConfig for Chat and Embeddings based on vendor and model
 * @author jsanca
 */
public interface AiModelConfigCatalog {

    AiModelConfig getChatConfig(AiVendor  vendor);

    AiModelConfig getChatConfig(String vendor);

    AiModelConfig getChatConfig(String vendor, String modelKey);

    AiModelConfig getEmbeddingsConfig(String vendor);

    AiModelConfig getEmbeddingsConfig(String vendor, String modelKey);

    // Opcional: "openai.chat.gpt-4o-mini" / "openai.embeddings.text-embedding-3-small"
    AiModelConfig getByPath(String path);

    List<String> getChatModelNames(String vendorName);
}
