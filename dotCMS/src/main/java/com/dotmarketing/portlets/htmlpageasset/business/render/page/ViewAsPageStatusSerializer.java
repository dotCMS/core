package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Map;

/**
 * Json serializer {@link ViewAsPageStatus}
 */
public class ViewAsPageStatusSerializer extends JsonSerializer<ViewAsPageStatus> {
    @Override
    public void serialize(ViewAsPageStatus viewAsPageStatus, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

        final ImmutableMap.Builder<Object, Object> viewAsMapBuilder = ImmutableMap.builder();

        if (viewAsPageStatus.getPersona() != null) {

            try {

                final Map personaMap = ContentletUtil.getContentPrintableMap(APILocator.systemUser(),
                        (Contentlet)viewAsPageStatus.getPersona());
                personaMap.put("personalized", viewAsPageStatus.isPersonalized());
                viewAsMapBuilder.put("persona", personaMap);
                
            } catch (DotDataException e) {

                throw new IOException(e);
            }
        }
        if (viewAsPageStatus.getVisitor() != null) {
          viewAsMapBuilder.put("visitor", viewAsPageStatus.getVisitor());
        }
        viewAsMapBuilder.put("language", viewAsPageStatus.getLanguage());

        if (viewAsPageStatus.getDevice() != null) {
            viewAsMapBuilder.put("device", viewAsPageStatus.getDevice().getMap());
        }

        viewAsMapBuilder.put("mode", viewAsPageStatus.getPageMode().toString());
        viewAsMapBuilder.put("variantId", viewAsPageStatus.getVariantId());

        jsonGenerator.writeObject(viewAsMapBuilder.build());
    }
}
