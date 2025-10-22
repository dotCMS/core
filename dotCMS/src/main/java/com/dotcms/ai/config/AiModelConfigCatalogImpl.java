package com.dotcms.ai.config;


import com.dotcms.ai.config.parser.AiVendorCatalogData;
import com.dotcms.ai.config.parser.AiVendorNode;
import com.google.common.collect.ImmutableList;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// todo: see if want to extract an interface to have an easier vision of the gets

/**
 * This Class holds the AI configuration
 * @author jsanca
 */
public final class AiModelConfigCatalogImpl implements AiModelConfigCatalog {

    private final Map<String, AiVendorNode> vendors = new ConcurrentHashMap<>();

    private AiModelConfigCatalogImpl(final Map<String, AiVendorNode> initial) {

        this.vendors.putAll(initial);
    }

    public static AiModelConfigCatalogImpl from(final AiVendorCatalogData data) {

        return new AiModelConfigCatalogImpl(data.getVendors());
    }

    // -------------------- read API --------------------

    public AiModelConfig getChatConfig(final AiVendor  vendor) {

        return getChatConfig(vendor.getVendorName());
    }
    /**
     * Get the ModelChat Config for the default model fo rthe vendor.
     * @param vendor String
     * @return ModelConfig
     */
    @Override
    public AiModelConfig getChatConfig(final String vendor) {

        final String modelKey = resolveDefaultModelKey(vendor, true);
        return getChatConfig(vendor, modelKey);
    }

    /**
     * Get Chat Model Configuration based on vendor + model
     * @param vendor String
     * @param modelKey String
     * @return ModelConfig
     */
    @Override
    public AiModelConfig getChatConfig(final String vendor, final String modelKey) {

        final AiVendorNode node = getVendorOrThrow(vendor);
        final Map<String,String> modelProps = getModelProps(node.getChatModels(), modelKey);
        final Map<String,String> flat = flatten(vendor, modelProps, node.getVendorProps());
        return new AiModelConfig(vendor + ".chat." + modelKey, flat);
    }

    /**
     * Get the Embeddings Model Configuration based on vendor.
     * @param vendor String
     * @return ModelConfig
     */
    @Override
    public AiModelConfig getEmbeddingsConfig(final String vendor) {

        final String modelKey = resolveDefaultModelKey(vendor, false);
        return getEmbeddingsConfig(vendor, modelKey);
    }

    /**
     * Get Embeddings Model Config for a given vendor + modelKey
     * @param vendor String
     * @param modelKey String
     * @return ModelConfig
     */
    @Override
    public AiModelConfig getEmbeddingsConfig(final String vendor, final String modelKey) {

        final AiVendorNode node = getVendorOrThrow(vendor);
        final Map<String,String> modelProps = getModelProps(node.getEmbeddings(), modelKey);
        final Map<String,String> flat = flatten(vendor, modelProps, node.getVendorProps());
        return new AiModelConfig(vendor + ".embeddings." + modelKey, flat);
    }

    // Opcional: "openai.chat.gpt-4o-mini" / "openai.embeddings.text-embedding-3-small"
    @Override
    public AiModelConfig getByPath(final String path) {

        final String[] parts = path.split("\\.");
        if (parts.length != 3){
            throw new IllegalArgumentException("Path must be vendor.kind.modelKey");
        }

        final String vendor = parts[0], kind = parts[1], modelKey = parts[2];

        if ("chat".equals(kind)) {
            return getChatConfig(vendor, modelKey);
        }

        if ("embeddings".equals(kind)) {
            return getEmbeddingsConfig(vendor, modelKey);
        }

        throw new IllegalArgumentException("Unknown kind: " + kind);
    }

    /**
     * Returns the list of all chat model names available for a vendor.
     *
     * @param vendor the vendor name (e.g. "openai", "anthropic")
     * @return a sorted list of chat model keys, never null
     */
    @Override
    public List<String> getChatModelNames(final String vendor) {
        final AiVendorNode node = getVendorOrThrow(vendor);
        if (node.getChatModels() == null || node.getChatModels().isEmpty()) {
            return Collections.emptyList();
        }
        return node.getChatModels().keySet().stream()
                .sorted()
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<String> getVendorNames() {

        final ImmutableList.Builder<String> vendorNamesBuilder = ImmutableList.builder();

        for (final String vendorName : this.vendors.keySet()) {
            vendorNamesBuilder.add(vendorName);
        }

        return vendorNamesBuilder.build();
    }

    /**
     * Returns the list of all embeddings model names available for a vendor.
     *
     * @param vendor the vendor name (e.g. "openai", "azure")
     * @return a sorted list of embeddings model keys, never null
     */
    public List<String> getEmbeddingsModelNames(final String vendor) {
        final AiVendorNode node = getVendorOrThrow(vendor);
        if (node.getEmbeddings() == null || node.getEmbeddings().isEmpty()) {
            return Collections.emptyList();
        }
        return node.getEmbeddings().keySet().stream()
                .sorted()
                .collect(Collectors.toUnmodifiableList());
    }


    // -------------------- Mutation API (OSGi) --------------------

    public void addOrUpdateVendorProps(final String vendor,
                                                    final Map<String,String> vendorProps) {

        final AiVendorNode old = vendors.get(vendor);
        final AiVendorNode.Builder builder = AiVendorNode.builder();
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

    public void addOrUpdateChatModel(final String vendor,
                                                  final String modelKey,
                                                  final Map<String,String> modelProps,
                                                  final boolean setAsDefault) {

        final AiVendorNode old = getOrCreateVendor(vendor);
        final AiVendorNode.Builder builder = rebuild(old);
        builder.putChatModel(modelKey, modelProps);
        if (setAsDefault) {
            builder.defaultChatModel(modelKey);
        }
        vendors.put(vendor, builder.build());
    }

    public void addOrUpdateEmbeddingsModel(final String vendor,
                                                        final String modelKey,
                                                        final Map<String,String> modelProps,
                                                        final boolean setAsDefault) {

        final AiVendorNode old = getOrCreateVendor(vendor);
        final AiVendorNode.Builder builder = rebuild(old);
        builder.putEmbeddingsModel(modelKey, modelProps);
        if (setAsDefault) {
            builder.defaultEmbeddingsModel(modelKey);
        }
        vendors.put(vendor, builder.build());
    }

    public void removeChatModel(final String vendor, final String modelKey) {

        final AiVendorNode old = getVendorOrThrow(vendor);
        if (!old.getChatModels().containsKey(modelKey)) {
            return;
        }
        final AiVendorNode.Builder builder = rebuild(old);
        // rebuilt without this model
        final Map<String, Map<String,String>> keep = new LinkedHashMap<>(old.getChatModels());
        keep.remove(modelKey);
        keep.forEach(builder::putChatModel);
        // reset default if needed
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

    public void removeEmbeddingsModel(final String vendor, final String modelKey) {

        final AiVendorNode old = getVendorOrThrow(vendor);
        if (!old.getEmbeddings().containsKey(modelKey)) {
            return;
        }

        final AiVendorNode.Builder builder = rebuild(old);
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

    public void setDefaultChatModel(final String vendor, final String modelKey) {

        final AiVendorNode old = getVendorOrThrow(vendor);
        if (!old.getChatModels().containsKey(modelKey)) {
            throw new NoSuchElementException("Chat model not found: " + vendor + "/" + modelKey);
        }

        final AiVendorNode.Builder builder = rebuild(old).defaultChatModel(modelKey);
        vendors.put(vendor, builder.build());
    }

    public void setDefaultEmbeddingsModel(final String vendor, final String modelKey) {

        final AiVendorNode old = getVendorOrThrow(vendor);
        if (!old.getEmbeddings().containsKey(modelKey)) {

            throw new NoSuchElementException("Embeddings model not found: " + vendor + "/" + modelKey);
        }

        final AiVendorNode.Builder builder = rebuild(old).defaultEmbeddingsModel(modelKey);
        vendors.put(vendor, builder.build());
    }

    public void removeVendor(final String vendor) {
        vendors.remove(vendor);
    }

    // -------------------- intern --------------------

    private AiVendorNode getVendorOrThrow(final String vendor) {
        final AiVendorNode node = vendors.get(vendor);
        if (node == null) {
            throw new NoSuchElementException("Vendor not found: " + vendor);
        }
        return node;
    }

    private AiVendorNode getOrCreateVendor(final String vendor) {

        final AiVendorNode vendorNode = vendors.get(vendor);
        if (vendorNode != null) {
            return vendorNode;
        }

        final AiVendorNode empty = AiVendorNode.builder().build();
        vendors.put(vendor, empty);
        return empty;
    }

    private static AiVendorNode.Builder rebuild(final AiVendorNode src) {

        AiVendorNode.Builder builder = AiVendorNode.builder();
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

        final AiVendorNode node = getVendorOrThrow(vendor);
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
            out.put(AiModelConfig.API_KEY, out.get("apiKey"));
        }
        if (out.containsKey("baseUrl")) {
            out.put(AiModelConfig.API_URL, out.get("baseUrl"));
        }
        if (out.containsKey("endpoint")) {
            out.put(AiModelConfig.API_URL, out.get("endpoint"));
        }

        // cleanup
        out.remove("chat");
        out.remove("embeddings");
        out.remove("defaultChatModel");
        out.remove("defaultEmbeddingsModel");

        out.put(AiModelConfig.VENDOR, vendor);
        if (!out.containsKey("model") && modelProps.containsKey("model")) {
            out.put("model", modelProps.get("model"));
        }
        return Collections.unmodifiableMap(out);
    }
}
