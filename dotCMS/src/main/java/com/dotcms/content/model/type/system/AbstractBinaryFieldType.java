package com.dotcms.content.model.type.system;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.annotation.HydrateWith;
import com.dotcms.content.model.annotation.Hydration;
import com.dotcms.content.model.annotation.ValueType;
import com.dotcms.content.model.hydration.MetadataDelegate;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * Binary-Field json representation
 */
@ValueType
@Immutable
@JsonDeserialize(as = BinaryFieldType.class)
@JsonTypeName(value = AbstractBinaryFieldType.TYPENAME)
public interface AbstractBinaryFieldType extends FieldValue<String> {

    String TYPENAME = "Binary";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    };

    //Additional calculated attributes must be marked as nullable since the calculation might fail
    @Nullable
    @JsonProperty("metadata")
    @Parameter
    Map<String, Serializable> metadata();

    @Hydration(properties = {
        @HydrateWith(delegate = MetadataDelegate.class, propertyName = "metadata")
    })
    abstract class Builder implements FieldValueBuilder {

    }

}
