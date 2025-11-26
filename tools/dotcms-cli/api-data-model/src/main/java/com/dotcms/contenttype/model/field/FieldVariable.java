package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Date;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;

@JsonTypeInfo(
	use = Id.CLASS,
		property = "clazz"
)
@JsonSerialize(as = ImmutableFieldVariable.class)
@JsonDeserialize(as = ImmutableFieldVariable.class)
@Value.Immutable
public interface FieldVariable {

	@Nullable
	abstract String id();

	@Nullable
	abstract String fieldId();

	@Nullable
	abstract String name();

	abstract String key();

	abstract String value();

	@Nullable
    abstract String userId();

	@Nullable
	abstract Date modDate();

}
