package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.repackage.com.fasterxml.jackson.core.JsonGenerator;
import com.dotcms.repackage.com.fasterxml.jackson.core.JsonProcessingException;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonSerializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.SerializerProvider;
import com.dotmarketing.portlets.personas.model.Persona;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;

/**
 * Json serializer {@link ViewAsPageStatus}
 */
public class ViewAsPageStatusSerializer extends JsonSerializer<ViewAsPageStatus> {
    @Override
    public void serialize(ViewAsPageStatus viewAsPageStatus, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

        final ImmutableMap.Builder<Object, Object> viewAsMapBuilder = ImmutableMap.builder();

        if (viewAsPageStatus.getPersona() != null) {
            viewAsMapBuilder.put("persona", ((Persona) viewAsPageStatus.getPersona()).getMap() );
        }

        viewAsMapBuilder.put("language", viewAsPageStatus.getLanguage());

        if (viewAsPageStatus.getDevice() != null) {
            viewAsMapBuilder.put("device", viewAsPageStatus.getDevice().getMap());
        }

        viewAsMapBuilder.put("mode", viewAsPageStatus.getPageMode().toString());

        jsonGenerator.writeObject(viewAsMapBuilder.build());
    }
}
