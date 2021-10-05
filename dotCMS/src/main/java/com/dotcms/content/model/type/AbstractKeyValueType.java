package com.dotcms.content.model.type;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@ValueTypeStyle
@Immutable
@JsonDeserialize(as = KeyValueType.class)
@JsonTypeName(value = AbstractKeyValueType.TYPENAME)
public interface AbstractKeyValueType extends FieldValue<Map<String,?>> {

    String TYPENAME = "KeyValue";

    @Override
    default String type() {
        return TYPENAME;
    };

    @JsonProperty("value")
    @Parameter
    Map<String,?> value();

}
