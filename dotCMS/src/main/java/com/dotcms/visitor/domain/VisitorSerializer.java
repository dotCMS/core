package com.dotcms.visitor.domain;

import java.io.IOException;

import com.dotmarketing.portlets.personas.model.Persona;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.ImmutableMap;
import io.vavr.control.Try;

public class VisitorSerializer extends JsonSerializer<Visitor> {

  @Override
  public void serialize(Visitor visitor, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    final ImmutableMap.Builder<Object, Object> visitorBuilder = ImmutableMap.builder();

    if (visitor.getPersona() != null) {
      Persona persona = visitor.getPersona();
      ImmutableMap.Builder<String,Object> builder =ImmutableMap.builder();
      if(persona.getKeyTag() !=null) {
          builder.put("keyTag", persona.getKeyTag());
      }
      if(persona.getIdentifier() !=null) {
          builder.put("identifier", persona.getIdentifier());
      }
      if(persona.getTitle() !=null) {
          builder.put("title", persona.getTitle());
      }
      if(persona.getTitleImage().isPresent()) {
          builder.put("titleImage", persona.getTitleImage().get());
      }
      if(persona.getTags()!=null) {
          builder.put("tags", persona.getTags());
      }
      visitorBuilder.put("persona", builder.build());
    }
    if (visitor.getAccruedTags() != null) {
        visitorBuilder.put("tags", visitor.getAccruedTags());
    }
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
    Geolocation geo = Try.of(()->visitor.getGeo()).getOrNull();
    if(geo!=null) {
        visitorBuilder.put("geo", geo);
    }
    visitorBuilder.put("personas", visitor.getWeightedPersonas());
    gen.writeObject(visitorBuilder.build());
  }

}
