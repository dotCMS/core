package com.dotcms.ai.config;

import java.util.List;

/**
 * Model Config Catalog
 * Returns the ModelConfig for Chat and Embeddings based on vendor and model
 * @author jsanca
 */
public interface AiModelConfigCatalog {

    /**
     * Gets the default chat config based on the vendor
     * @param vendor
     * @return
     */
    AiModelConfig getChatConfig(AiVendor  vendor);

    /**
     * Get the default chat config based on the vendor
     * @param vendor
     * @return
     */
    AiModelConfig getChatConfig(String vendor);

    /**
     * Gets the chat config model for vendor and modelKey
     * @param vendor
     * @param modelKey
     * @return
     */
    AiModelConfig getChatConfig(String vendor, String modelKey);

    /**
     * Get the default embeddings model for this vendor
     * @param vendor
     * @return
     */
    AiModelConfig getEmbeddingsConfig(String vendor);

    /**
     * Gets an embedding model config based on vendor and model key
     * @param vendor
     * @param modelKey
     * @return
     */
    AiModelConfig getEmbeddingsConfig(String vendor, String modelKey);

    /**
     * Return a model config based on a path such as
     * "openai.chat.gpt-4o-mini" / "openai.embeddings.text-embedding-3-small"
     * @param path
     * @return
     */
    AiModelConfig getByPath(String path);

    /**
     * Return all the chat models
     * @param vendorName
     * @return
     */
    List<String> getChatModelNames(String vendorName);

    /**
     * Return all the vendors name
     * @return
     */
    List<String> getVendorNames();

    AiModelConfig getDefaultChatModel();

}
