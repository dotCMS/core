package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * This JSON Serializer is used to transform an {@link EmptyPageView} object into a JSON string. It
 * uses default or empty data structures that simulate an empty HTML Page, but includes other
 * objects, such as the associated Vanity URL.
 *
 * @author Jose Castro
 * @since May 8th, 2024
 */
public class EmptyPageViewSerializer extends JsonSerializer<EmptyPageView> {

    @Override
    public void serialize(final EmptyPageView emptyPageView, final JsonGenerator jsonGenerator,
                          final SerializerProvider serializerProvider) throws IOException {
        final ObjectWriter objectWriter = JsonMapper.mapper.writer().withDefaultPrettyPrinter();
        final ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
        builder.putAll(getObjectMap(emptyPageView));
        final String json = objectWriter.writeValueAsString(builder.build());
        jsonGenerator.writeRawValue(json);
    }

    /**
     * Generates a Map with empty properties for the empty HTML Page.
     *
     * @param emptyPageView The {@link EmptyPageView} object to serialize.
     *
     * @return A Map with empty properties for the empty HTML Page.
     */
    protected Map<String, Object> getObjectMap(final EmptyPageView emptyPageView) {
        final Map<String, Object> pageViewMap = new TreeMap<>();
        pageViewMap.put("vanityUrl", emptyPageView.getCachedVanityUrl());
        pageViewMap.put("page", new HashMap<>());
        pageViewMap.put("containers", new HashMap<>());
        pageViewMap.put("template", new HashMap<>());
        
        pageViewMap.put("site", new HashMap<>());
        pageViewMap.put("viewAs", new HashMap<>());
        pageViewMap.put("canCreateTemplate", emptyPageView.canCreateTemplate());
        pageViewMap.put("numberContents", emptyPageView.getNumberContents());

        if (emptyPageView.getLayout() != null) {
            pageViewMap.put("layout", emptyPageView.getLayout());
        }
        return pageViewMap;
    }

}
