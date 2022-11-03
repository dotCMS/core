package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.value.Value;


@Value.Immutable
@JsonSerialize(as = ImmutableFieldLayoutRow.class)
@JsonDeserialize(as = ImmutableFieldLayoutRow.class)
public abstract class FieldLayoutRow {

    public abstract Field divider();

    public abstract List<FieldLayoutColumn> columns();


}
