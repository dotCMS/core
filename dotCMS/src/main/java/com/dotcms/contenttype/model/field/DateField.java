package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.contenttype.util.FieldUtil;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Collection;
import java.util.List;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableDateField.class)
@JsonDeserialize(as = ImmutableDateField.class)
@Value.Immutable
public abstract class DateField extends Field {

  private static final long serialVersionUID = 1L;

  @Override
  public Class type() {
    return DateField.class;
  }

  @Value.Default
  @Override
  public DataTypes dataType() {
    return DataTypes.DATE;
  };

  @JsonIgnore
  @Value.Derived
  @Override
  public List<DataTypes> acceptedDataTypes() {
    return ImmutableList.of(DataTypes.DATE);
  }

  public abstract static class Builder implements FieldBuilder {}

  @Value.Check
  public void check() {

    Preconditions.checkArgument(
        new FieldUtil().validDate(defaultValue()),
        this.getClass().getSimpleName() + " invalid default Value:" + defaultValue());
  }

  @JsonIgnore
  public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties() {
    return list(
        ContentTypeFieldProperties.NAME,
        ContentTypeFieldProperties.REQUIRED,
        ContentTypeFieldProperties.HINT,
        ContentTypeFieldProperties.DEFAULT_VALUE,
        ContentTypeFieldProperties.SEARCHABLE,
        ContentTypeFieldProperties.INDEXED,
        ContentTypeFieldProperties.LISTED);
  }
}
