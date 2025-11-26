package com.dotcms.contenttype.model.field;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.contenttype.model.component.FieldFormRenderer;
import com.dotcms.contenttype.model.component.FieldValueRenderer;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
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

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Derived;


@JsonTypeInfo(
	use = Id.CLASS,
	include = JsonTypeInfo.As.PROPERTY,
	property = "clazz"
)
@JsonTypeIdResolver(value = Field.ClassNameAliasResolver.class)
@JsonSubTypes({
	@Type(value = StoryBlockField.class),
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

  /**
   * Determines whether the field must be returned by the API (for instance, the GraphQL API) or not, even if the field
   * is removable.
   * @deprecated Since 24.07, for removal in a future version.
   * @return If the field must be returned by the API, set it to {@code true}.
   */
  @Deprecated(since = "24.07", forRemoval = true)
  @Value.Default
  public boolean forceIncludeInApi() {
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
      } catch (final DotDataException e) {
        final String errorMsg = String.format("Unable to load field variables for field '%s' [%s]: %s", this.name(),
                this.id(), e.getMessage());
        Logger.error(this, errorMsg);
        throw new DotStateException(errorMsg, e);
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

  /**
   * Returns a collection of variable keys that the Field should respect
   * @return List of String (property names)
   */
  @JsonIgnore
  @Value.Default
  public List<String> fieldVariableKeys() {
    return Collections.emptyList();
  }

  /**
   * This method and any specific descendant implementation should examine the type value of the
   * passed on the param and decide what Builder must be used to create a FieldValue to represent
   * the Field as json properly
   * @return
   */
  public Optional<FieldValueBuilder> fieldValue(final Object value) {
    Logger.debug(Field.class, () -> String
            .format(" No field Specific Impl found for field with name `%s` and variable `%s`. ",
                    this.name(), variable()));
    return Optional.empty();
  }

  public Optional<String> fieldVariableValue(final String fieldVariableName) {
    return this.fieldVariables().stream()
            .filter(fieldVariable -> fieldVariable.key().equals(fieldVariableName))
            .map(fieldVariable -> fieldVariable.value())
            .findFirst();
  }

  static class ClassNameAliasResolver extends ClassNameIdResolver {
    static TypeFactory typeFactory = TypeFactory.defaultInstance();
    public ClassNameAliasResolver() {
      super(typeFactory.constructType(new TypeReference<Field>() {
              }), typeFactory,
              BasicPolymorphicTypeValidator.builder().allowIfSubType(Field.class).build()
      );
    }

    @Override
    public JavaType typeFromId(final DatabindContext context, final String id) throws IOException {
      final String packageName = Field.class.getPackageName();
      if( !id.contains(".") && !id.startsWith(packageName)){
        final String className = String.format("%s.Immutable%s",packageName,id);
        return super.typeFromId(context, className);
      }
      return super.typeFromId(context, id);
    }
  }

}
