package com.dotcms.ai.config.parser;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.util.Config;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for the Model Config
 * @author jsanca
 */
public final class AiModelConfigParser {

    public static final String CHAT = "chat";
    public static final String EMBEDDINGS = "embeddings";
    public static final String DEFAULT_CHAT_MODEL = "defaultChatModel";
    public static final String DEFAULT_EMBEDDINGS_MODEL = "defaultEmbeddingsModel";
    private final ObjectMapper mapper = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

    @FunctionalInterface
    public interface ValueResolver {
        String resolve(String key);
    }

    public AiVendorCatalogData parse(final String json, final Map<String,String> context) {
        return parse(json, (key) -> {
            // default ValueResolver impl, based on dotCMS config and context (the context encapsulates the dotAI Secrets App
            return Config.getStringProperty(key, context.get(key));
        });
    }

    public AiVendorCatalogData parse(final String json, final ValueResolver resolver) {

        try {

            final Map<String, Object> raw = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            final Map<String, Object> root = interpolateDeep(raw, resolver);

            final Object configRoot = root.get("config");
            if (!(configRoot instanceof Map)) {
                throw new IllegalArgumentException("'config' object is required");
            }
            @SuppressWarnings("unchecked")
            final Map<String, Object> vendors = (Map<String, Object>) configRoot;

            final AiVendorCatalogData.Builder catalogBuilder = AiVendorCatalogData.builder();

            for (final Map.Entry<String, Object> vendorEntry : vendors.entrySet()) {

                final String vendorName = vendorEntry.getKey();
                @SuppressWarnings("unchecked")
                final Map<String, Object> vendorValueMap = (Map<String, Object>) vendorEntry.getValue();

                final Map<String,String> vendorProps = new LinkedHashMap<>();
                final Map<String, Map<String,String>> chatModels = new LinkedHashMap<>();
                final Map<String, Map<String,String>> embeddings = new LinkedHashMap<>();
                String defaultChat = null;
                String defaultEmbeddings = null;

                for (final Map.Entry<String,Object> vendorValueMapEntry : vendorValueMap.entrySet()) {

                    final String key = vendorValueMapEntry.getKey();
                    final Object value = vendorValueMapEntry.getValue();

                    /// todo refactor this
                    if (CHAT.equals(key) && value instanceof Map) {

                        @SuppressWarnings("unchecked")
                        Map<String,Object> cm = (Map<String,Object>) value;
                        for (Map.Entry<String,Object> me : cm.entrySet()) {
                            chatModels.put(me.getKey(), toFlatProps(me.getValue()));
                        }
                    } else if (EMBEDDINGS.equals(key) && value instanceof Map) {

                        @SuppressWarnings("unchecked")
                        Map<String,Object> em = (Map<String,Object>) value;
                        for (Map.Entry<String,Object> me : em.entrySet()) {
                            embeddings.put(me.getKey(), toFlatProps(me.getValue()));
                        }
                    } else if (DEFAULT_CHAT_MODEL.equals(key)) {

                        defaultChat = String.valueOf(value);
                    } else if (DEFAULT_EMBEDDINGS_MODEL.equals(key)) {

                        defaultEmbeddings = String.valueOf(value);
                    } else if (value != null && !(value instanceof Map) && !(value instanceof List)) {

                        vendorProps.put(key, String.valueOf(value));
                    }
                }

                // infer defaults if not provided
                if (defaultChat == null && !chatModels.isEmpty()) {
                    defaultChat = chatModels.keySet().iterator().next();
                }

                if (defaultEmbeddings == null && !embeddings.isEmpty()) {
                    defaultEmbeddings = embeddings.keySet().iterator().next();
                }

                final AiVendorNode vendorNode = AiVendorNode.builder()
                        .defaultChatModel(defaultChat)
                        .defaultEmbeddingsModel(defaultEmbeddings)
                        .build();

                // build again with props/maps (builder is simple; you can extend it)
                final AiVendorNode.Builder vendorNodeBuilder = AiVendorNode.builder();
                for (final Map.Entry<String,String> vendorPropEntry : vendorProps.entrySet()) {

                    vendorNodeBuilder.putVendorProp(vendorPropEntry.getKey(), vendorPropEntry.getValue());
                }

                for (final Map.Entry<String, Map<String,String>> chatModelEntry : chatModels.entrySet()) {

                    vendorNodeBuilder.putChatModel(chatModelEntry.getKey(), chatModelEntry.getValue());
                }

                for (final Map.Entry<String, Map<String,String>> embeddingsEntry : embeddings.entrySet()) {

                    vendorNodeBuilder.putEmbeddingsModel(embeddingsEntry.getKey(), embeddingsEntry.getValue());
                }

                if (defaultChat != null) {

                    vendorNodeBuilder.defaultChatModel(defaultChat);
                }

                if (defaultEmbeddings  != null) {

                    vendorNodeBuilder.defaultEmbeddingsModel(defaultEmbeddings);
                }

                catalogBuilder.putVendor(vendorName, vendorNodeBuilder.build());
            }

            return catalogBuilder.build();
        } catch (Exception e) {

            throw new IllegalArgumentException("Failed to parse model config JSON", e);
        }
    }

    // ----- helpers (interpolation + flatten) -----

    private static Map<String,String> toFlatProps(final Object node) {

        final Map<String,String> out = new LinkedHashMap<>();
        if (!(node instanceof Map)) {
            return out;
        }

        @SuppressWarnings("unchecked")
        final Map<String,Object> nodeMap = (Map<String,Object>) node;
        for (final Map.Entry<String,Object> nodeMapEntry : nodeMap.entrySet()) {

            final Object value = nodeMapEntry.getValue();
            if (value == null || value instanceof Map || value instanceof List) {
                continue;
            }

            out.put(nodeMapEntry.getKey(), String.valueOf(value));
        }

        return out;
    }

    private static final Pattern VAR = Pattern.compile("\\$\\{([^}]+)}");

    @SuppressWarnings("unchecked")
    private static Map<String, Object> interpolateDeep(final Map<String, Object> node,
                                                       final ValueResolver resolver) {

        final Map<String, Object> out = new LinkedHashMap<>();

        for (final Map.Entry<String, Object> nodeEntry : node.entrySet()) {

            out.put(nodeEntry.getKey(), interpolateValue(nodeEntry.getValue(), resolver));
        }

        return out;
    }

    @SuppressWarnings("unchecked")
    private static Object interpolateValue(final Object value,
                                           final ValueResolver resolver) {
        if (value == null) {

            return null;
        }

        if (value instanceof String) {

            // todo: check if this is already implemented in dotCMS
            return interpolateString((String) value, resolver);
        }
        if (value instanceof Map) {

            final Map<String,Object> resultMap = new LinkedHashMap<>();
            for (Map.Entry<String,Object> valueEntry : ((Map<String,Object>) value).entrySet()) {

                resultMap.put(valueEntry.getKey(), interpolateValue(valueEntry.getValue(), resolver));
            }
            return resultMap;
        }

        if (value instanceof List) {
            List<Object> resultList = new ArrayList<>();
            for (Object o : (List<?>) value) {

                resultList.add(interpolateValue(o, resolver));
            }
            return resultList;
        }

        return value;
    }

    private static String interpolateString(final String string, final ValueResolver resolver) {

        final Matcher matcher = VAR.matcher(string);
        final StringBuffer sb = new StringBuffer();
        while (matcher.find()) {

            final String key = matcher.group(1);
            String repl = resolver != null ? resolver.resolve(key) : null;

            if (repl == null) {
                repl = matcher.group(0);
            } else {
                repl = repl.replace("\\", "\\\\").replace("$", "\\$");
            }

            matcher.appendReplacement(sb, repl);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
