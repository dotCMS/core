package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.repackage.com.fasterxml.jackson.core.JsonGenerator;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonProcessingException;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonSerializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.SerializerProvider;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.templates.model.Template;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableMap;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Json serializer of {@link HTMLPageAssetRendered}
 */
public class HTMLPageAssetRenderedSerializer extends JsonSerializer<HTMLPageAssetRendered> {
    @Override
    public void serialize(HTMLPageAssetRendered htmlPageAssetRendered, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

        final Template template = htmlPageAssetRendered.getTemplate();
        final ImmutableMap.Builder<String, Object> responseMapBuilder = ImmutableMap.<String, Object> builder()
                .put("html", htmlPageAssetRendered.getHtml())
                .put("page", htmlPageAssetRendered.getPageInfo())
                .put("containers", htmlPageAssetRendered.getContainers()
                        .stream()
                        .collect(Collectors.toMap(
                                containerRendered -> containerRendered.getContainer().getIdentifier() +"_" + containerRendered.getUuid(),
                                containerRendered -> containerRendered
                        ))
                )
                .put("viewAs", htmlPageAssetRendered.getViewAs())
                .put("canCreateTemplate", htmlPageAssetRendered.isCanCreateTemplate())
                .put("template", ImmutableMap.builder()
                        .put("canEdit", htmlPageAssetRendered.isCanEditTemplate())
                        .putAll(this.asMap(template))
                        .build()
                );


        if (htmlPageAssetRendered.getLayout() != null) {
            responseMapBuilder.put("layout", htmlPageAssetRendered.getLayout());
        }

        jsonGenerator.writeObject(responseMapBuilder.build());
    }

    private Map<Object, Object> asMap(final Object object)  {
        final ObjectWriter objectWriter = JsonMapper.mapper.writer().withDefaultPrettyPrinter();

        try {
            String json = objectWriter.writeValueAsString(object);
            Map map = JsonMapper.mapper.readValue(new CharArrayReader(json.toCharArray()), Map.class);
            map.values().removeIf(Objects::isNull);
            return map;
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }
    }
}
