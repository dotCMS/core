package com.dotcms.visitor.domain;

import java.io.IOException;

import com.dotmarketing.portlets.personas.model.Persona;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.ImmutableMap;

public class VisitorSerializer extends JsonSerializer<Visitor> {

  @Override
  public void serialize(Visitor visitor, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    final ImmutableMap.Builder<Object, Object> visitorBuilder = ImmutableMap.builder();

    if (visitor.getPersona() != null) {
      Persona persona = visitor.getPersona();
      visitorBuilder.put("persona", ImmutableMap.of(
          "keyTag", persona.getKeyTag(), 
          "identifier",persona.getIdentifier(),
          "tags",persona.getTags(),
          "title",persona.getTitle()
          )
        );
    }
    
    visitorBuilder.put("tags", visitor.getAccruedTags());
    if (visitor.getDevice() != null) {
      visitorBuilder.put("device", visitor.getDevice());
    }
    visitorBuilder.put("isNew", visitor.isNewVisitor());
    if (visitor.getUserAgent() != null) {
      visitorBuilder.put("userAgent", visitor.getUserAgent());
    }
    if (visitor.getReferrer() != null) {
      visitorBuilder.put("referer", visitor.getReferrer());
    }
    if (visitor.getDmid() != null) {
      visitorBuilder.put("dmid", visitor.getDmid());
    }
    visitorBuilder.put("personas", visitor.getWeightedPersonas());
    gen.writeObject(visitorBuilder.build());
  }

}
