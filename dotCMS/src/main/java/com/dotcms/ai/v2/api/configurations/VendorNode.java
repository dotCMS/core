package com.dotcms.ai.v2.api.configurations;

import java.util.*;

public class VendorNode {

    private final Map<String, String> vendorProps;                 // level vendor (apiKey, baseUrl/endpoint, etc.)
    private final Map<String, Map<String, String>> chatModels;     // key -> props
    private final Map<String, Map<String, String>> embeddings;     // key -> props
    private final String defaultChatModel;
    private final String defaultEmbeddingsModel;

    private VendorNode(final Builder builder) {

        this.vendorProps = Collections.unmodifiableMap(new LinkedHashMap<>(builder.vendorProps));
        this.chatModels  = deepRO(builder.chatModels);
        this.embeddings  = deepRO(builder.embeddings);
        this.defaultChatModel = builder.defaultChatModel;
        this.defaultEmbeddingsModel = builder.defaultEmbeddingsModel;
    }

    public Map<String, String> getVendorProps() { return vendorProps; }
    public Map<String, Map<String, String>> getChatModels() { return chatModels; }
    public Map<String, Map<String, String>> getEmbeddings() { return embeddings; }
    public String getDefaultChatModel() { return defaultChatModel; }
    public String getDefaultEmbeddingsModel() { return defaultEmbeddingsModel; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final Map<String, String> vendorProps = new LinkedHashMap<>();
        private final Map<String, Map<String, String>> chatModels = new LinkedHashMap<>();
        private final Map<String, Map<String, String>> embeddings = new LinkedHashMap<>();
        private String defaultChatModel;
        private String defaultEmbeddingsModel;

        public Builder putVendorProp(String k, String v) { if (v != null) vendorProps.put(k, v); return this; }
        public Builder putChatModel(String key, Map<String,String> props) { chatModels.put(key, new LinkedHashMap<>(props)); return this; }
        public Builder putEmbeddingsModel(String key, Map<String,String> props) { embeddings.put(key, new LinkedHashMap<>(props)); return this; }
        public Builder defaultChatModel(String key) { this.defaultChatModel = key; return this; }
        public Builder defaultEmbeddingsModel(String key) { this.defaultEmbeddingsModel = key; return this; }
        public VendorNode build() { return new VendorNode(this); }
    }

    private static Map<String, Map<String,String>> deepRO(Map<String, Map<String,String>> src) {
        Map<String, Map<String,String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String,String>> e : src.entrySet()) {
            out.put(e.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(e.getValue())));
        }
        return Collections.unmodifiableMap(out);
    }
}
