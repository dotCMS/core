package com.dotcms.ai.config;

/**
 * Encapsulates
 * @author jsanca
 */
public enum AiVendor {

    OPEN_AI("openai"),
    ANTHROPIC("anthropic"),
    AZURE_OPEN_AI("azureopenai");

    private final String vendorName;

    AiVendor(
           final String vendorName) {

        this.vendorName = vendorName;
    }

    public String getVendorName() {
        return vendorName;
    }
}
