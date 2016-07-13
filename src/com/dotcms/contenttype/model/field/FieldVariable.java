package com.dotcms.contenttype.model.field;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.immutables.value.Value;

import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;
import com.google.common.base.Preconditions;

@Value.Immutable
public interface FieldVariable extends Serializable {

	abstract String id();

	abstract String fieldId();

	abstract String name();

	abstract String key();

	abstract String value();

	abstract String userId();

	@Value.Default
	default Date modDate() {
		return DateUtils.round(new Date(), Calendar.SECOND);

	}
	
	
	@Value.Check
	default void check() {
		Preconditions.checkArgument(fieldId()==null,"FieldVariable.fieldId cannot be null");
		Preconditions.checkArgument(key()==null,"FieldVariable.key cannot be null");
	}
	
}
