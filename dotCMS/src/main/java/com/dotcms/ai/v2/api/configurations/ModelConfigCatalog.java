package com.dotcms.ai.v2.api.configurations;

import com.dotcms.ai.v2.api.provider.config.ModelConfig;

import java.util.*;

// todo: see if want to extract an interface to have an easier vision of the gets
public final class ModelConfigCatalog {

    public static final class Keys {
        public static final String VENDOR  = "vendor";
        public static final String MODEL   = "model";
        public static final String API_URL = "apiUrl";   // normalize  baseUrl/endpoint
        public static final String API_KEY = "key";      // normalize  apiKey
    }

    private Map<String, VendorNode> vendors;

    private ModelConfigCatalog(final Map<String, VendorNode> initial) {

        this.vendors = new LinkedHashMap<>(initial);
    }

    public static ModelConfigCatalog from(final VendorCatalogData data) {

        return new ModelConfigCatalog(data.getVendors());
    }

    // -------------------- read API --------------------

    public synchronized ModelConfig getChatConfig(final String vendor) {

        final String modelKey = resolveDefaultModelKey(vendor, true);
        return getChatConfig(vendor, modelKey);
    }

    public synchronized ModelConfig getChatConfig(final String vendor, final String modelKey) {

        final VendorNode node = getVendorOrThrow(vendor);
        final Map<String,String> modelProps = getModelProps(node.getChatModels(), modelKey);
        final Map<String,String> flat = flatten(vendor, modelProps, node.getVendorProps());
        return new ModelConfig(vendor + ".chat." + modelKey, flat);
    }

    public synchronized ModelConfig getEmbeddingsConfig(final String vendor) {

        final String modelKey = resolveDefaultModelKey(vendor, false);
        return getEmbeddingsConfig(vendor, modelKey);
    }

    public synchronized ModelConfig getEmbeddingsConfig(final String vendor, final String modelKey) {

        final VendorNode node = getVendorOrThrow(vendor);
        final Map<String,String> modelProps = getModelProps(node.getEmbeddings(), modelKey);
        final Map<String,String> flat = flatten(vendor, modelProps, node.getVendorProps());
        return new ModelConfig(vendor + ".embeddings." + modelKey, flat);
    }

    // Opcional: "openai.chat.gpt-4o-mini" / "openai.embeddings.text-embedding-3-small"
    public synchronized ModelConfig getByPath(final String path) {

        final String[] parts = path.split("\\.");
        if (parts.length != 3){
            throw new IllegalArgumentException("Path must be vendor.kind.modelKey");
        }

        String vendor = parts[0], kind = parts[1], modelKey = parts[2];

        if ("chat".equals(kind)) {
            return getChatConfig(vendor, modelKey);
        }

        if ("embeddings".equals(kind)) {
            return getEmbeddingsConfig(vendor, modelKey);
        }

        throw new IllegalArgumentException("Unknown kind: " + kind);
    }

    // -------------------- Mutation API (OSGi) --------------------

    public synchronized void addOrUpdateVendorProps(final String vendor,
                                                    final Map<String,String> vendorProps) {

        final VendorNode old = vendors.get(vendor);
        final VendorNode.Builder builder = VendorNode.builder();
        final Map<String,String> base = (old != null) ? old.getVendorProps() : Collections.emptyMap();
        base.forEach(builder::putVendorProp);
        vendorProps.forEach(builder::putVendorProp);

        if (old != null) {

            if (old.getDefaultChatModel() != null) {
                builder.defaultChatModel(old.getDefaultChatModel());
            }

            if (old.getDefaultEmbeddingsModel() != null) {
                builder.defaultEmbeddingsModel(old.getDefaultEmbeddingsModel());
            }

            old.getChatModels().forEach(builder::putChatModel);
            old.getEmbeddings().forEach(builder::putEmbeddingsModel);
        }

        vendors.put(vendor, builder.build());
    }

    public synchronized void addOrUpdateChatModel(final String vendor,
                                                  final String modelKey,
                                                  final Map<String,String> modelProps,
                                                  final boolean setAsDefault) {

        final VendorNode old = getOrCreateVendor(vendor);
        final VendorNode.Builder builder = rebuild(old);
        builder.putChatModel(modelKey, modelProps);
        if (setAsDefault) {
            builder.defaultChatModel(modelKey);
        }
        vendors.put(vendor, builder.build());
    }

    public synchronized void addOrUpdateEmbeddingsModel(final String vendor,
                                                        final String modelKey,
                                                        final Map<String,String> modelProps,
                                                        final boolean setAsDefault) {

        final VendorNode old = getOrCreateVendor(vendor);
        final VendorNode.Builder builder = rebuild(old);
        builder.putEmbeddingsModel(modelKey, modelProps);
        if (setAsDefault) {
            builder.defaultEmbeddingsModel(modelKey);
        }
        vendors.put(vendor, builder.build());
    }

    public synchronized void removeChatModel(final String vendor, final String modelKey) {

        final VendorNode old = getVendorOrThrow(vendor);
        if (!old.getChatModels().containsKey(modelKey)) {
            return;
        }
        final VendorNode.Builder builder = rebuild(old);
        // reconstruir sin ese model
        final Map<String, Map<String,String>> keep = new LinkedHashMap<>(old.getChatModels());
        keep.remove(modelKey);
        keep.forEach(builder::putChatModel);
        // reset default si hacía falta
        if (modelKey.equals(old.getDefaultChatModel())) {
            String newDefault = keep.isEmpty() ? null : keep.keySet().iterator().next();
            if (newDefault != null) {
                builder.defaultChatModel(newDefault);
            }
        } else if (old.getDefaultChatModel() != null) {
            builder.defaultChatModel(old.getDefaultChatModel());
        }
        old.getEmbeddings().forEach(builder::putEmbeddingsModel);
        old.getVendorProps().forEach(builder::putVendorProp);
        vendors.put(vendor, builder.build());
    }

    public synchronized void removeEmbeddingsModel(final String vendor, final String modelKey) {

        final VendorNode old = getVendorOrThrow(vendor);
        if (!old.getEmbeddings().containsKey(modelKey)) {
            return;
        }

        final VendorNode.Builder builder = rebuild(old);
        final Map<String, Map<String,String>> keep = new LinkedHashMap<>(old.getEmbeddings());
        keep.remove(modelKey);
        keep.forEach(builder::putEmbeddingsModel);
        if (modelKey.equals(old.getDefaultEmbeddingsModel())) {
            String newDefault = keep.isEmpty() ? null : keep.keySet().iterator().next();
            if (newDefault != null) {
                builder.defaultEmbeddingsModel(newDefault);
            }
        } else if (old.getDefaultEmbeddingsModel() != null) {
            builder.defaultEmbeddingsModel(old.getDefaultEmbeddingsModel());
        }
        old.getChatModels().forEach(builder::putChatModel);
        old.getVendorProps().forEach(builder::putVendorProp);
        vendors.put(vendor, builder.build());
    }

    public synchronized void setDefaultChatModel(final String vendor, final String modelKey) {

        final VendorNode old = getVendorOrThrow(vendor);
        if (!old.getChatModels().containsKey(modelKey)) {
            throw new NoSuchElementException("Chat model not found: " + vendor + "/" + modelKey);
        }

        final VendorNode.Builder builder = rebuild(old).defaultChatModel(modelKey);
        vendors.put(vendor, builder.build());
    }

    public synchronized void setDefaultEmbeddingsModel(final String vendor, final String modelKey) {

        final VendorNode old = getVendorOrThrow(vendor);
        if (!old.getEmbeddings().containsKey(modelKey)) {

            throw new NoSuchElementException("Embeddings model not found: " + vendor + "/" + modelKey);
        }

        final VendorNode.Builder builder = rebuild(old).defaultEmbeddingsModel(modelKey);
        vendors.put(vendor, builder.build());
    }

    public synchronized void removeVendor(final String vendor) {
        vendors.remove(vendor);
    }

    // -------------------- internos --------------------

    private VendorNode getVendorOrThrow(final String vendor) {
        final VendorNode node = vendors.get(vendor);
        if (node == null) {
            throw new NoSuchElementException("Vendor not found: " + vendor);
        }
        return node;
    }

    private VendorNode getOrCreateVendor(final String vendor) {

        final VendorNode vendorNode = vendors.get(vendor);
        if (vendorNode != null) {
            return vendorNode;
        }

        final VendorNode empty = VendorNode.builder().build();
        vendors.put(vendor, empty);
        return empty;
    }

    private static VendorNode.Builder rebuild(final VendorNode src) {

        VendorNode.Builder builder = VendorNode.builder();
        src.getVendorProps().forEach(builder::putVendorProp);
        src.getChatModels().forEach(builder::putChatModel);
        src.getEmbeddings().forEach(builder::putEmbeddingsModel);

        if (src.getDefaultChatModel() != null) {
            builder.defaultChatModel(src.getDefaultChatModel());
        }
        if (src.getDefaultEmbeddingsModel() != null) {
            builder.defaultEmbeddingsModel(src.getDefaultEmbeddingsModel());
        }
        return builder;
    }

    private String resolveDefaultModelKey(final String vendor, final boolean chat) {

        final VendorNode node = getVendorOrThrow(vendor);
        final String explicit = chat ? node.getDefaultChatModel() : node.getDefaultEmbeddingsModel();
        if (explicit != null && !explicit.isEmpty()) {
            return explicit;
        }
        final Map<String, Map<String,String>> bucket = chat ? node.getChatModels() : node.getEmbeddings();
        if (bucket.isEmpty()) {
            throw new NoSuchElementException("No models found for vendor=" + vendor + " kind=" + (chat?"chat":"embeddings"));
        }
        return bucket.keySet().iterator().next();
    }

    private static Map<String,String> getModelProps(final Map<String, Map<String,String>> bucket, final String modelKey) {

        final Map<String,String> props = bucket.get(modelKey);
        if (props == null) {
            throw new NoSuchElementException("Model not found: " + modelKey);
        }
        return props;
    }

    private static Map<String, String> flatten(final String vendor, final Map<String,String> modelProps,
                                               final Map<String,String> vendorProps) {

        final Map<String,String> out = new LinkedHashMap<>();
        // vendor first
        out.putAll(vendorProps);
        // model overrides
        out.putAll(modelProps);

        // normalize keys → key/apiUrl
        if (out.containsKey("apiKey")) {
            out.put(Keys.API_KEY, out.get("apiKey"));
        }
        if (out.containsKey("baseUrl")) {
            out.put(Keys.API_URL, out.get("baseUrl"));
        }
        if (out.containsKey("endpoint")) {
            out.put(Keys.API_URL, out.get("endpoint"));
        }

        // cleanup
        out.remove("chat");
        out.remove("embeddings");
        out.remove("defaultChatModel");
        out.remove("defaultEmbeddingsModel");

        out.put(Keys.VENDOR, vendor);
        if (!out.containsKey("model") && modelProps.containsKey("model")) {
            out.put("model", modelProps.get("model"));
        }
        return Collections.unmodifiableMap(out);
    }
}
