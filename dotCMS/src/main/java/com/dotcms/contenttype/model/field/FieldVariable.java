package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import org.apache.commons.lang.time.DateUtils;
import org.elasticsearch.common.Nullable;
import org.immutables.value.Value;

@JsonTypeInfo(
	use = Id.CLASS,
	include = JsonTypeInfo.As.PROPERTY,
	property = "clazz"
)
@JsonSerialize(as = ImmutableFieldVariable.class)
@JsonDeserialize(as = ImmutableFieldVariable.class)
@Value.Immutable
public interface FieldVariable extends Serializable, IFieldVar {

    //Key name of the variable used to define a custom mapping for a field in ES
    String ES_CUSTOM_MAPPING_KEY = "esCustomMapping";

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

	@Value.Default
	default Date modDate() {
		return DateUtils.round(new Date(), Calendar.SECOND);
	}


	@Value.Check
	default void check() {
		//Preconditions.checkArgument(StringUtils.isNotEmpty(key()), "FieldVariable.key cannot be empty");
		//Preconditions.checkArgument(StringUtils.isNotEmpty(value()), "FieldVariable.val cannot be empty");
	}	
}
