package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.dotmarketing.portlets.personas.model.Persona;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.HashMap;
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

            final Map personaMap = new HashMap(Persona.class.cast(viewAsPageStatus.getPersona()).getMap());
            personaMap.put("personalized", viewAsPageStatus.isPersonalized());
            viewAsMapBuilder.put("persona", personaMap);
        }

        viewAsMapBuilder.put("language", viewAsPageStatus.getLanguage());

        if (viewAsPageStatus.getDevice() != null) {
            viewAsMapBuilder.put("device", viewAsPageStatus.getDevice().getMap());
        }

        viewAsMapBuilder.put("mode", viewAsPageStatus.getPageMode().toString());

        jsonGenerator.writeObject(viewAsMapBuilder.build());
    }
}
