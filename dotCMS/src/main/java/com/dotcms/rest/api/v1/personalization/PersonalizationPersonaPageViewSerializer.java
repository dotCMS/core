package com.dotcms.rest.api.v1.personalization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class PersonalizationPersonaPageViewSerializer extends JsonSerializer<PersonalizationPersonaPageView>  {
    @Override
    public void serialize(final PersonalizationPersonaPageView personaPageView,
                          final JsonGenerator jsonGenerator,
                          final SerializerProvider serializers) throws IOException {

        jsonGenerator.writeRawValue(new ObjectMapper().writer().withDefaultPrettyPrinter()
                .writeValueAsString(personaPageView.getPersona()));
    }
}
