package com.dotcms.content.model.type.radio;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;

/**
 * Float Radio Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = FloatRadioFieldType.class)
@JsonTypeName(value = AbstractFloatRadioFieldType.TYPENAME)
public interface AbstractFloatRadioFieldType extends FieldValue<Float> {

    String TYPENAME = "Float-Radio";

    /**
     *
     * @return
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    abstract class Builder implements FieldValueBuilder {}
}
