package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Collection;
import java.util.List;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableHostFolderField.class)
@JsonDeserialize(as = ImmutableHostFolderField.class)
@Value.Immutable
public abstract class HostFolderField extends Field implements OnePerContentType {

  @Value.Default
  @Override
  public boolean indexed() {
    return true;
  };

  private static final long serialVersionUID = 1L;

  @Override
  public Class type() {
    return HostFolderField.class;
  }

  @Value.Default
  @Override
  public DataTypes dataType() {
    return DataTypes.SYSTEM;
  };

  @Override
  public final List<DataTypes> acceptedDataTypes() {
    return ImmutableList.of(DataTypes.SYSTEM, DataTypes.TEXT);
  }

  public abstract static class Builder implements FieldBuilder {}

  @JsonIgnore
  public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties() {
    return list(
        ContentTypeFieldProperties.NAME,
        ContentTypeFieldProperties.HINT,
        ContentTypeFieldProperties.REQUIRED,
        ContentTypeFieldProperties.SEARCHABLE);
  }

  @JsonIgnore
  public String getContentTypeFieldLabelKey() {
    return "Host-Folder";
  }
}
