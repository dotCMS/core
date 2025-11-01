package com.dotcms.ai.api.provider;

/**
 * Just groups model providers
 * @author jsanca
 */
public interface VendorModelProvider extends ChatModelProvider, EmbeddingModelProvider {

    String getVendorName();
}
