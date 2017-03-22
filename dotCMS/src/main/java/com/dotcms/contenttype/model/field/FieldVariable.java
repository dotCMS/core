package com.dotcms.contenttype.model.field;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.immutables.value.Value;

import com.dotcms.repackage.org.apache.commons.lang.time.DateUtils;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = ImmutableFieldVariable.class)
@JsonDeserialize(as = ImmutableFieldVariable.class)
@Value.Immutable
public interface FieldVariable extends Serializable, IFieldVar {
    public static final String NOT_PERSISTED="NOT_PERSISTED";
    @Value.Default
    default String id(){
        return NOT_PERSISTED;
    }


  @Value.Default
  default String fieldId() {
    return NOT_PERSISTED;
  }

	
	@Value.Default
	default String name(){
	    return NOT_PERSISTED;
	}

	abstract String key();

	abstract String value();

    @Value.Default
    default String userId(){
        return NOT_PERSISTED;
    }
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
