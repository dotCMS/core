package com.dotcms.contenttype.model.field;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.elasticsearch.common.Nullable;
import org.immutables.value.Value;

import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonTypeInfo(
	use = Id.CLASS,
	include = JsonTypeInfo.As.PROPERTY,
	property = "clazz"
)
@JsonSerialize(as = ImmutableFieldVariable.class)
@JsonDeserialize(as = ImmutableFieldVariable.class)
@Value.Immutable
public interface FieldVariable extends Serializable, IFieldVar {

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
		Preconditions.checkArgument(key()!=null,"FieldVariable.key cannot be null");
		Preconditions.checkArgument(value()!=null,"FieldVariable.val cannot be null");
	}	
}
