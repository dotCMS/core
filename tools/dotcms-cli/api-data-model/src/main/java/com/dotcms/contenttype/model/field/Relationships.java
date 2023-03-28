package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableRelationships.class)
@JsonDeserialize(as = ImmutableRelationships.class)
public interface Relationships {

      @Nullable
      Boolean isParentField();
      @Nullable
      String velocityVar();
      @Nullable
      Long cardinality();

}
