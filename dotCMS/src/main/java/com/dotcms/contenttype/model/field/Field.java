package com.dotcms.contenttype.model.field;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.contenttype.model.component.FieldFormRenderer;
import com.dotcms.contenttype.model.component.FieldValueRenderer;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.elasticsearch.common.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Derived;


@JsonTypeInfo(
	use = Id.CLASS,
	include = JsonTypeInfo.As.PROPERTY,
	property = "clazz"
)
@JsonSubTypes({
	@Type(value = BinaryField.class),
	@Type(value = CategoryField.class),
	@Type(value = CheckboxField.class),
	@Type(value = ConstantField.class),
	@Type(value = CustomField.class),
	@Type(value = DateField.class),
	@Type(value = DateTimeField.class),
	@Type(value = EmptyField.class),
	@Type(value = FileField.class),
	@Type(value = HiddenField.class),
	@Type(value = HostFolderField.class),
	@Type(value = ImageField.class),
	@Type(value = KeyValueField.class),
	@Type(value = LineDividerField.class),
	@Type(value = MultiSelectField.class),
	@Type(value = PermissionTabField.class),
	@Type(value = RadioField.class),
    @Type(value = RelationshipField.class),
	@Type(value = RelationshipsTabField.class),
	@Type(value = SelectField.class),
	@Type(value = TabDividerField.class),
	@Type(value = TagField.class),
	@Type(value = TextAreaField.class),
	@Type(value = TextField.class),
	@Type(value = TimeField.class),
	@Type(value = WysiwygField.class),
    @Type(value = RowField.class),
    @Type(value = ColumnField.class),
})
public abstract class Field implements FieldIf, Serializable {

  public final static int SORT_ORDER_DEFAULT_VALUE = -1;

  @Value.Check
  public void check() {
	Preconditions.checkArgument(StringUtils.isNotEmpty(name()), "Name cannot be empty for " + this.getClass());

  }


  private static final long serialVersionUID = 5640078738113157867L;
  final static Date legacyFieldDate = new Date(1470845479000L); // 08/10/2016 @ 4:11pm (UTC)

  @Value.Default
  public boolean searchable() {
    return false;
  }

  @Value.Default
  public boolean unique() {
    return false;
  }

  @Value.Default
  public boolean indexed() {
    return false;
  }

  @Value.Default
  public boolean listed() {
    return false;
  }

  @Value.Default
  public boolean readOnly() {
    return false;
  }

  @Nullable
  public abstract String owner();

  @Nullable
  public abstract String id();


  @Value.Lazy
  public String inode() {
    return id();
  }

  @Value.Default
  public Date modDate() {
    return DateUtils.round(new Date(), Calendar.SECOND);
  }


  public abstract String name();

  @JsonIgnore
  @Derived
  public String typeName() {
    return LegacyFieldTypes.getImplClass(this.getClass().getCanonicalName()).getCanonicalName();
  }

  @JsonIgnore
  @Derived
  public Class<Field> type() {
    return LegacyFieldTypes.getImplClass(this.getClass().getCanonicalName());
  }

  @Nullable
  public abstract String relationType();

  @Value.Default
  public boolean required() {
    return false;
  }

  @Nullable
  public abstract String variable();

  @Value.Default
  public int sortOrder() {
    return SORT_ORDER_DEFAULT_VALUE;
  }

  @Value.Lazy
  public List<SelectableValue> selectableValues() {
    return ImmutableList.of();
  };


  @Nullable
  public abstract String values();

  @Nullable
  public abstract String regexCheck();

  @Nullable
  public abstract String hint();

  @Nullable
  public abstract String defaultValue();


  @Value.Default
  public boolean fixed() {
    return false;
  }

  public boolean legacyField() {
    return false;
  }

  @CloseDBIfOpened
  @JsonIgnore
  @Value.Lazy
  public List<FieldVariable> fieldVariables() {
    if (innerFieldVariables == null) {
      try {
        innerFieldVariables = FactoryLocator.getFieldFactory().loadVariables(this);
      } catch (DotDataException e) {
        throw new DotStateException("unable to load field variables:" + e.getMessage(), e);
      }
    }

    return innerFieldVariables;

  }

  @JsonIgnore
  @Value.Lazy
  public Map<String, FieldVariable> fieldVariablesMap() {
    Map<String, FieldVariable> fmap = new HashMap<>();
    for (FieldVariable fv : this.fieldVariables()) {
      fmap.put(fv.id(), fv);
      fmap.put(fv.key(), fv);
    }
    return ImmutableMap.copyOf(fmap);
  }

  private List<FieldVariable> innerFieldVariables = null;

  public void constructFieldVariables(List<FieldVariable> fieldVariables) {

    innerFieldVariables = fieldVariables;
  }

  @JsonIgnore
  public abstract List<DataTypes> acceptedDataTypes();

  public abstract DataTypes dataType();

  @Nullable
  public abstract String contentTypeId();
  
  @Nullable
  @Value.Auxiliary 
  public abstract String dbColumn();

  @Value.Default
  public Date iDate() {
    return DateUtils.round(new Date(), Calendar.SECOND);

  }



  @Value.Lazy
  public FieldFormRenderer formRenderer() {
    return new FieldFormRenderer() {};
  }

  @Value.Lazy
  public FieldValueRenderer valueRenderer() {
    return new FieldValueRenderer() {};
  }


  @Value.Lazy
  public FieldValueRenderer listRenderer() {
    return new FieldValueRenderer() {};
  }

  /**
   * Field' properties to be set by the UI
   * @return
   */
  public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
    return Collections.emptyList();
  }

  /**
   * Key for the Field' help text
   * @return
   */
  @JsonIgnore
  public String getContentTypeFieldHelpTextKey(){
    String legacyName = LegacyFieldTypes.getLegacyName(this.getClass());
    return "field.type.help." + legacyName;
  }

  /**
   * Key for the Field'label
   * @return
   */
  @JsonIgnore
  public String getContentTypeFieldLabelKey(){
    String legacyName = LegacyFieldTypes.getLegacyName(this.getClass());
    return legacyName.substring(0, 1).toUpperCase() + legacyName.substring(1);
  }
}
