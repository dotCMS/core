package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableFieldLayoutColumn.class)
@JsonDeserialize(as = ImmutableFieldLayoutColumn.class)
public abstract class FieldLayoutColumn {

    public abstract ColumnField columnDivider();

    public abstract List<Field> fields();

}
