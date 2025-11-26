package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonInclude(Include.USE_DEFAULTS)
@Value.Style(passAnnotations = {JsonInclude.class}, jakarta = true)
@JsonSerialize(as = ImmutableRelationships.class)
@JsonDeserialize(as = ImmutableRelationships.class)
public interface Relationships {

      //This field isn't getting returned by the ContentTypeResource
      //Apparently some additional logic needs to happen to get this field populated
      //Unfortunately, relationships are usually built using fields API in a separate call
      //This might be problematic in the future but for now it escapes the scope of the CLI
      @Nullable
      Boolean isParentField();

      String velocityVar();

      @JsonSerialize(using = RelationshipCardinalityViewBasedSerializer.class)
      @JsonDeserialize(using = RelationshipCardinalityDeserializer.class)
      RelationshipCardinality cardinality();

}
