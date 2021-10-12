package com.dotcms.content.model.type.radio;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * Float Radio Field json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = FloatRadioFieldType.class)
@JsonTypeName(value = AbstractFloatRadioFieldType.TYPENAME)
public interface AbstractFloatRadioFieldType extends FieldValue<Float> {

    String TYPENAME = "FloatRadio";

    /**
     *
     * @return
     */
    @Override
    default String type() {
        return TYPENAME;
    };

    /**
     *
     * @return
     */
    @JsonProperty("value")
    @Parameter
    Float value();

}
