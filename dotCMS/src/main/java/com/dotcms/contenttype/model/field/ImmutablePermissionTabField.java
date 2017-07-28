package com.dotcms.contenttype.model.field;

import com.dotcms.contenttype.model.component.FieldFormRenderer;
import com.dotcms.contenttype.model.component.FieldValueRenderer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.Booleans;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import org.elasticsearch.common.Nullable;

/**
 * Immutable implementation of {@link PermissionTabField}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutablePermissionTabField.builder()}.
 */
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@Generated({"Immutables.generator", "PermissionTabField"})
@Immutable
public final class ImmutablePermissionTabField extends PermissionTabField {
  private final List<DataTypes> acceptedDataTypes;
  private final DataTypes dataType;
  private final boolean searchable;
  private final boolean unique;
  private final boolean indexed;
  private final boolean listed;
  private final boolean readOnly;
  private final @Nullable String owner;
  private final @Nullable String id;
  private final Date modDate;
  private final String name;
  private final String typeName;
  private final @Nullable String relationType;
  private final boolean required;
  private final @Nullable String variable;
  private final int sortOrder;
  private final @Nullable String values;
  private final @Nullable String regexCheck;
  private final @Nullable String hint;
  private final @Nullable String defaultValue;
  private final boolean fixed;
  private final @Nullable String contentTypeId;
  private final @Nullable String dbColumn;
  private final Date iDate;

  private ImmutablePermissionTabField(ImmutablePermissionTabField.Builder builder) {
    this.owner = builder.owner;
    this.id = builder.id;
    this.name = builder.name;
    this.relationType = builder.relationType;
    this.variable = builder.variable;
    this.values = builder.values;
    this.regexCheck = builder.regexCheck;
    this.hint = builder.hint;
    this.defaultValue = builder.defaultValue;
    this.contentTypeId = builder.contentTypeId;
    this.dbColumn = builder.dbColumn;
    if (builder.dataType != null) {
      initShim.dataType(builder.dataType);
    }
    if (builder.searchableIsSet()) {
      initShim.searchable(builder.searchable);
    }
    if (builder.uniqueIsSet()) {
      initShim.unique(builder.unique);
    }
    if (builder.indexedIsSet()) {
      initShim.indexed(builder.indexed);
    }
    if (builder.listedIsSet()) {
      initShim.listed(builder.listed);
    }
    if (builder.readOnlyIsSet()) {
      initShim.readOnly(builder.readOnly);
    }
    if (builder.modDate != null) {
      initShim.modDate(builder.modDate);
    }
    if (builder.requiredIsSet()) {
      initShim.required(builder.required);
    }
    if (builder.sortOrderIsSet()) {
      initShim.sortOrder(builder.sortOrder);
    }
    if (builder.fixedIsSet()) {
      initShim.fixed(builder.fixed);
    }
    if (builder.iDate != null) {
      initShim.iDate(builder.iDate);
    }
    this.dataType = initShim.dataType();
    this.searchable = initShim.searchable();
    this.unique = initShim.unique();
    this.indexed = initShim.indexed();
    this.listed = initShim.listed();
    this.readOnly = initShim.readOnly();
    this.modDate = initShim.modDate();
    this.required = initShim.required();
    this.sortOrder = initShim.sortOrder();
    this.fixed = initShim.fixed();
    this.iDate = initShim.iDate();
    this.acceptedDataTypes = initShim.acceptedDataTypes();
    this.typeName = initShim.typeName();
    this.initShim = null;
  }

  private ImmutablePermissionTabField(
      DataTypes dataType,
      boolean searchable,
      boolean unique,
      boolean indexed,
      boolean listed,
      boolean readOnly,
      @Nullable String owner,
      @Nullable String id,
      Date modDate,
      String name,
      @Nullable String relationType,
      boolean required,
      @Nullable String variable,
      int sortOrder,
      @Nullable String values,
      @Nullable String regexCheck,
      @Nullable String hint,
      @Nullable String defaultValue,
      boolean fixed,
      @Nullable String contentTypeId,
      @Nullable String dbColumn,
      Date iDate) {
    this.dataType = dataType;
    this.searchable = searchable;
    this.unique = unique;
    this.indexed = indexed;
    this.listed = listed;
    this.readOnly = readOnly;
    this.owner = owner;
    this.id = id;
    this.modDate = modDate;
    this.name = name;
    this.relationType = relationType;
    this.required = required;
    this.variable = variable;
    this.sortOrder = sortOrder;
    this.values = values;
    this.regexCheck = regexCheck;
    this.hint = hint;
    this.defaultValue = defaultValue;
    this.fixed = fixed;
    this.contentTypeId = contentTypeId;
    this.dbColumn = dbColumn;
    this.iDate = iDate;
    initShim.dataType(this.dataType);
    initShim.searchable(this.searchable);
    initShim.unique(this.unique);
    initShim.indexed(this.indexed);
    initShim.listed(this.listed);
    initShim.readOnly(this.readOnly);
    initShim.modDate(this.modDate);
    initShim.required(this.required);
    initShim.sortOrder(this.sortOrder);
    initShim.fixed(this.fixed);
    initShim.iDate(this.iDate);
    this.acceptedDataTypes = initShim.acceptedDataTypes();
    this.typeName = initShim.typeName();
    this.initShim = null;
  }

  private static final int STAGE_INITIALIZING = -1;
  private static final int STAGE_UNINITIALIZED = 0;
  private static final int STAGE_INITIALIZED = 1;
  private transient volatile InitShim initShim = new InitShim();

  private final class InitShim {
    private List<DataTypes> acceptedDataTypes;
    private int acceptedDataTypesBuildStage;

    List<DataTypes> acceptedDataTypes() {
      if (acceptedDataTypesBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (acceptedDataTypesBuildStage == STAGE_UNINITIALIZED) {
        acceptedDataTypesBuildStage = STAGE_INITIALIZING;
        this.acceptedDataTypes = Preconditions.checkNotNull(ImmutablePermissionTabField.super.acceptedDataTypes(), "acceptedDataTypes");
        acceptedDataTypesBuildStage = STAGE_INITIALIZED;
      }
      return this.acceptedDataTypes;
    }
    private DataTypes dataType;
    private int dataTypeBuildStage;

    DataTypes dataType() {
      if (dataTypeBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (dataTypeBuildStage == STAGE_UNINITIALIZED) {
        dataTypeBuildStage = STAGE_INITIALIZING;
        this.dataType = Preconditions.checkNotNull(ImmutablePermissionTabField.super.dataType(), "dataType");
        dataTypeBuildStage = STAGE_INITIALIZED;
      }
      return this.dataType;
    }

    void dataType(DataTypes dataType) {
      this.dataType = dataType;
      dataTypeBuildStage = STAGE_INITIALIZED;
    }
    private boolean searchable;
    private int searchableBuildStage;

    boolean searchable() {
      if (searchableBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (searchableBuildStage == STAGE_UNINITIALIZED) {
        searchableBuildStage = STAGE_INITIALIZING;
        this.searchable = ImmutablePermissionTabField.super.searchable();
        searchableBuildStage = STAGE_INITIALIZED;
      }
      return this.searchable;
    }

    void searchable(boolean searchable) {
      this.searchable = searchable;
      searchableBuildStage = STAGE_INITIALIZED;
    }
    private boolean unique;
    private int uniqueBuildStage;

    boolean unique() {
      if (uniqueBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (uniqueBuildStage == STAGE_UNINITIALIZED) {
        uniqueBuildStage = STAGE_INITIALIZING;
        this.unique = ImmutablePermissionTabField.super.unique();
        uniqueBuildStage = STAGE_INITIALIZED;
      }
      return this.unique;
    }

    void unique(boolean unique) {
      this.unique = unique;
      uniqueBuildStage = STAGE_INITIALIZED;
    }
    private boolean indexed;
    private int indexedBuildStage;

    boolean indexed() {
      if (indexedBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (indexedBuildStage == STAGE_UNINITIALIZED) {
        indexedBuildStage = STAGE_INITIALIZING;
        this.indexed = ImmutablePermissionTabField.super.indexed();
        indexedBuildStage = STAGE_INITIALIZED;
      }
      return this.indexed;
    }

    void indexed(boolean indexed) {
      this.indexed = indexed;
      indexedBuildStage = STAGE_INITIALIZED;
    }
    private boolean listed;
    private int listedBuildStage;

    boolean listed() {
      if (listedBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (listedBuildStage == STAGE_UNINITIALIZED) {
        listedBuildStage = STAGE_INITIALIZING;
        this.listed = ImmutablePermissionTabField.super.listed();
        listedBuildStage = STAGE_INITIALIZED;
      }
      return this.listed;
    }

    void listed(boolean listed) {
      this.listed = listed;
      listedBuildStage = STAGE_INITIALIZED;
    }
    private boolean readOnly;
    private int readOnlyBuildStage;

    boolean readOnly() {
      if (readOnlyBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (readOnlyBuildStage == STAGE_UNINITIALIZED) {
        readOnlyBuildStage = STAGE_INITIALIZING;
        this.readOnly = ImmutablePermissionTabField.super.readOnly();
        readOnlyBuildStage = STAGE_INITIALIZED;
      }
      return this.readOnly;
    }

    void readOnly(boolean readOnly) {
      this.readOnly = readOnly;
      readOnlyBuildStage = STAGE_INITIALIZED;
    }
    private Date modDate;
    private int modDateBuildStage;

    Date modDate() {
      if (modDateBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (modDateBuildStage == STAGE_UNINITIALIZED) {
        modDateBuildStage = STAGE_INITIALIZING;
        this.modDate = Preconditions.checkNotNull(ImmutablePermissionTabField.super.modDate(), "modDate");
        modDateBuildStage = STAGE_INITIALIZED;
      }
      return this.modDate;
    }

    void modDate(Date modDate) {
      this.modDate = modDate;
      modDateBuildStage = STAGE_INITIALIZED;
    }
    private String typeName;
    private int typeNameBuildStage;

    String typeName() {
      if (typeNameBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (typeNameBuildStage == STAGE_UNINITIALIZED) {
        typeNameBuildStage = STAGE_INITIALIZING;
        this.typeName = Preconditions.checkNotNull(ImmutablePermissionTabField.super.typeName(), "typeName");
        typeNameBuildStage = STAGE_INITIALIZED;
      }
      return this.typeName;
    }
    private boolean required;
    private int requiredBuildStage;

    boolean required() {
      if (requiredBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (requiredBuildStage == STAGE_UNINITIALIZED) {
        requiredBuildStage = STAGE_INITIALIZING;
        this.required = ImmutablePermissionTabField.super.required();
        requiredBuildStage = STAGE_INITIALIZED;
      }
      return this.required;
    }

    void required(boolean required) {
      this.required = required;
      requiredBuildStage = STAGE_INITIALIZED;
    }
    private int sortOrder;
    private int sortOrderBuildStage;

    int sortOrder() {
      if (sortOrderBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (sortOrderBuildStage == STAGE_UNINITIALIZED) {
        sortOrderBuildStage = STAGE_INITIALIZING;
        this.sortOrder = ImmutablePermissionTabField.super.sortOrder();
        sortOrderBuildStage = STAGE_INITIALIZED;
      }
      return this.sortOrder;
    }

    void sortOrder(int sortOrder) {
      this.sortOrder = sortOrder;
      sortOrderBuildStage = STAGE_INITIALIZED;
    }
    private boolean fixed;
    private int fixedBuildStage;

    boolean fixed() {
      if (fixedBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (fixedBuildStage == STAGE_UNINITIALIZED) {
        fixedBuildStage = STAGE_INITIALIZING;
        this.fixed = ImmutablePermissionTabField.super.fixed();
        fixedBuildStage = STAGE_INITIALIZED;
      }
      return this.fixed;
    }

    void fixed(boolean fixed) {
      this.fixed = fixed;
      fixedBuildStage = STAGE_INITIALIZED;
    }
    private Date iDate;
    private int iDateBuildStage;

    Date iDate() {
      if (iDateBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (iDateBuildStage == STAGE_UNINITIALIZED) {
        iDateBuildStage = STAGE_INITIALIZING;
        this.iDate = Preconditions.checkNotNull(ImmutablePermissionTabField.super.iDate(), "iDate");
        iDateBuildStage = STAGE_INITIALIZED;
      }
      return this.iDate;
    }

    void iDate(Date iDate) {
      this.iDate = iDate;
      iDateBuildStage = STAGE_INITIALIZED;
    }

    private String formatInitCycleMessage() {
      ArrayList<String> attributes = Lists.newArrayList();
      if (acceptedDataTypesBuildStage == STAGE_INITIALIZING) attributes.add("acceptedDataTypes");
      if (dataTypeBuildStage == STAGE_INITIALIZING) attributes.add("dataType");
      if (searchableBuildStage == STAGE_INITIALIZING) attributes.add("searchable");
      if (uniqueBuildStage == STAGE_INITIALIZING) attributes.add("unique");
      if (indexedBuildStage == STAGE_INITIALIZING) attributes.add("indexed");
      if (listedBuildStage == STAGE_INITIALIZING) attributes.add("listed");
      if (readOnlyBuildStage == STAGE_INITIALIZING) attributes.add("readOnly");
      if (modDateBuildStage == STAGE_INITIALIZING) attributes.add("modDate");
      if (typeNameBuildStage == STAGE_INITIALIZING) attributes.add("typeName");
      if (requiredBuildStage == STAGE_INITIALIZING) attributes.add("required");
      if (sortOrderBuildStage == STAGE_INITIALIZING) attributes.add("sortOrder");
      if (fixedBuildStage == STAGE_INITIALIZING) attributes.add("fixed");
      if (iDateBuildStage == STAGE_INITIALIZING) attributes.add("iDate");
      return "Cannot build PermissionTabField, attribute initializers form cycle" + attributes;
    }
  }

  /**
   * @return The computed-at-construction value of the {@code acceptedDataTypes} attribute
   */
  @JsonProperty("acceptedDataTypes")
  @JsonIgnore
  @Override
  public List<DataTypes> acceptedDataTypes() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.acceptedDataTypes()
        : this.acceptedDataTypes;
  }

  /**
   * @return The value of the {@code dataType} attribute
   */
  @JsonProperty("dataType")
  @Override
  public DataTypes dataType() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.dataType()
        : this.dataType;
  }

  /**
   * @return The value of the {@code searchable} attribute
   */
  @JsonProperty("searchable")
  @Override
  public boolean searchable() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.searchable()
        : this.searchable;
  }

  /**
   * @return The value of the {@code unique} attribute
   */
  @JsonProperty("unique")
  @Override
  public boolean unique() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.unique()
        : this.unique;
  }

  /**
   * @return The value of the {@code indexed} attribute
   */
  @JsonProperty("indexed")
  @Override
  public boolean indexed() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.indexed()
        : this.indexed;
  }

  /**
   * @return The value of the {@code listed} attribute
   */
  @JsonProperty("listed")
  @Override
  public boolean listed() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.listed()
        : this.listed;
  }

  /**
   * @return The value of the {@code readOnly} attribute
   */
  @JsonProperty("readOnly")
  @Override
  public boolean readOnly() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.readOnly()
        : this.readOnly;
  }

  /**
   * @return The value of the {@code owner} attribute
   */
  @JsonProperty("owner")
  @Override
  public @Nullable String owner() {
    return owner;
  }

  /**
   * @return The value of the {@code id} attribute
   */
  @JsonProperty("id")
  @Override
  public @Nullable String id() {
    return id;
  }

  /**
   * @return The value of the {@code modDate} attribute
   */
  @JsonProperty("modDate")
  @Override
  public Date modDate() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.modDate()
        : this.modDate;
  }

  /**
   * @return The value of the {@code name} attribute
   */
  @JsonProperty("name")
  @Override
  public String name() {
    return name;
  }

  /**
   * @return The computed-at-construction value of the {@code typeName} attribute
   */
  @JsonProperty("typeName")
  @JsonIgnore
  @Override
  public String typeName() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.typeName()
        : this.typeName;
  }

  /**
   * @return The value of the {@code relationType} attribute
   */
  @JsonProperty("relationType")
  @Override
  public @Nullable String relationType() {
    return relationType;
  }

  /**
   * @return The value of the {@code required} attribute
   */
  @JsonProperty("required")
  @Override
  public boolean required() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.required()
        : this.required;
  }

  /**
   * @return The value of the {@code variable} attribute
   */
  @JsonProperty("variable")
  @Override
  public @Nullable String variable() {
    return variable;
  }

  /**
   * @return The value of the {@code sortOrder} attribute
   */
  @JsonProperty("sortOrder")
  @Override
  public int sortOrder() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.sortOrder()
        : this.sortOrder;
  }

  /**
   * @return The value of the {@code values} attribute
   */
  @JsonProperty("values")
  @Override
  public @Nullable String values() {
    return values;
  }

  /**
   * @return The value of the {@code regexCheck} attribute
   */
  @JsonProperty("regexCheck")
  @Override
  public @Nullable String regexCheck() {
    return regexCheck;
  }

  /**
   * @return The value of the {@code hint} attribute
   */
  @JsonProperty("hint")
  @Override
  public @Nullable String hint() {
    return hint;
  }

  /**
   * @return The value of the {@code defaultValue} attribute
   */
  @JsonProperty("defaultValue")
  @Override
  public @Nullable String defaultValue() {
    return defaultValue;
  }

  /**
   * @return The value of the {@code fixed} attribute
   */
  @JsonProperty("fixed")
  @Override
  public boolean fixed() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.fixed()
        : this.fixed;
  }

  /**
   * @return The value of the {@code contentTypeId} attribute
   */
  @JsonProperty("contentTypeId")
  @Override
  public @Nullable String contentTypeId() {
    return contentTypeId;
  }

  /**
   * @return The value of the {@code dbColumn} attribute
   */
  @JsonProperty("dbColumn")
  @Override
  public @Nullable String dbColumn() {
    return dbColumn;
  }

  /**
   * @return The value of the {@code iDate} attribute
   */
  @JsonProperty("iDate")
  @Override
  public Date iDate() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.iDate()
        : this.iDate;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#dataType() dataType} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for dataType
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withDataType(DataTypes value) {
    if (this.dataType == value) return this;
    DataTypes newValue = Preconditions.checkNotNull(value, "dataType");
    return validate(new ImmutablePermissionTabField(
        newValue,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#searchable() searchable} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for searchable
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withSearchable(boolean value) {
    if (this.searchable == value) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        value,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#unique() unique} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for unique
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withUnique(boolean value) {
    if (this.unique == value) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        value,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#indexed() indexed} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for indexed
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withIndexed(boolean value) {
    if (this.indexed == value) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        value,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#listed() listed} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for listed
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withListed(boolean value) {
    if (this.listed == value) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        value,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#readOnly() readOnly} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for readOnly
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withReadOnly(boolean value) {
    if (this.readOnly == value) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        value,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#owner() owner} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for owner (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withOwner(@Nullable String value) {
    if (Objects.equal(this.owner, value)) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        value,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#id() id} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for id (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withId(@Nullable String value) {
    if (Objects.equal(this.id, value)) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        value,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#modDate() modDate} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for modDate
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withModDate(Date value) {
    if (this.modDate == value) return this;
    Date newValue = Preconditions.checkNotNull(value, "modDate");
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        newValue,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#name() name} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for name
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withName(String value) {
    if (this.name.equals(value)) return this;
    String newValue = Preconditions.checkNotNull(value, "name");
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        newValue,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#relationType() relationType} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for relationType (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withRelationType(@Nullable String value) {
    if (Objects.equal(this.relationType, value)) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        value,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#required() required} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for required
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withRequired(boolean value) {
    if (this.required == value) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        value,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#variable() variable} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for variable (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withVariable(@Nullable String value) {
    if (Objects.equal(this.variable, value)) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        value,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#sortOrder() sortOrder} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for sortOrder
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withSortOrder(int value) {
    if (this.sortOrder == value) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        value,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#values() values} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for values (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withValues(@Nullable String value) {
    if (Objects.equal(this.values, value)) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        value,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#regexCheck() regexCheck} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for regexCheck (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withRegexCheck(@Nullable String value) {
    if (Objects.equal(this.regexCheck, value)) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        value,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#hint() hint} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for hint (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withHint(@Nullable String value) {
    if (Objects.equal(this.hint, value)) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        value,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#defaultValue() defaultValue} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for defaultValue (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withDefaultValue(@Nullable String value) {
    if (Objects.equal(this.defaultValue, value)) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        value,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#fixed() fixed} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for fixed
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withFixed(boolean value) {
    if (this.fixed == value) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        value,
        this.contentTypeId,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#contentTypeId() contentTypeId} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for contentTypeId (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withContentTypeId(@Nullable String value) {
    if (Objects.equal(this.contentTypeId, value)) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        value,
        this.dbColumn,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#dbColumn() dbColumn} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for dbColumn (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withDbColumn(@Nullable String value) {
    if (Objects.equal(this.dbColumn, value)) return this;
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        value,
        this.iDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link PermissionTabField#iDate() iDate} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for iDate
   * @return A modified copy of the {@code this} object
   */
  public final ImmutablePermissionTabField withIDate(Date value) {
    if (this.iDate == value) return this;
    Date newValue = Preconditions.checkNotNull(value, "iDate");
    return validate(new ImmutablePermissionTabField(
        this.dataType,
        this.searchable,
        this.unique,
        this.indexed,
        this.listed,
        this.readOnly,
        this.owner,
        this.id,
        this.modDate,
        this.name,
        this.relationType,
        this.required,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.fixed,
        this.contentTypeId,
        this.dbColumn,
        newValue));
  }

  /**
   * This instance is equal to all instances of {@code ImmutablePermissionTabField} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@javax.annotation.Nullable Object another) {
    if (this == another) return true;
    return another instanceof ImmutablePermissionTabField
        && equalTo((ImmutablePermissionTabField) another);
  }

  private boolean equalTo(ImmutablePermissionTabField another) {
    return acceptedDataTypes.equals(another.acceptedDataTypes)
        && dataType.equals(another.dataType)
        && searchable == another.searchable
        && unique == another.unique
        && indexed == another.indexed
        && listed == another.listed
        && readOnly == another.readOnly
        && Objects.equal(owner, another.owner)
        && Objects.equal(id, another.id)
        && modDate.equals(another.modDate)
        && name.equals(another.name)
        && typeName.equals(another.typeName)
        && Objects.equal(relationType, another.relationType)
        && required == another.required
        && Objects.equal(variable, another.variable)
        && sortOrder == another.sortOrder
        && Objects.equal(values, another.values)
        && Objects.equal(regexCheck, another.regexCheck)
        && Objects.equal(hint, another.hint)
        && Objects.equal(defaultValue, another.defaultValue)
        && fixed == another.fixed
        && Objects.equal(contentTypeId, another.contentTypeId)
        && iDate.equals(another.iDate);
  }

  /**
   * Computes a hash code from attributes: {@code acceptedDataTypes}, {@code dataType}, {@code searchable}, {@code unique}, {@code indexed}, {@code listed}, {@code readOnly}, {@code owner}, {@code id}, {@code modDate}, {@code name}, {@code typeName}, {@code relationType}, {@code required}, {@code variable}, {@code sortOrder}, {@code values}, {@code regexCheck}, {@code hint}, {@code defaultValue}, {@code fixed}, {@code contentTypeId}, {@code iDate}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 31;
    h = h * 17 + acceptedDataTypes.hashCode();
    h = h * 17 + dataType.hashCode();
    h = h * 17 + Booleans.hashCode(searchable);
    h = h * 17 + Booleans.hashCode(unique);
    h = h * 17 + Booleans.hashCode(indexed);
    h = h * 17 + Booleans.hashCode(listed);
    h = h * 17 + Booleans.hashCode(readOnly);
    h = h * 17 + Objects.hashCode(owner);
    h = h * 17 + Objects.hashCode(id);
    h = h * 17 + modDate.hashCode();
    h = h * 17 + name.hashCode();
    h = h * 17 + typeName.hashCode();
    h = h * 17 + Objects.hashCode(relationType);
    h = h * 17 + Booleans.hashCode(required);
    h = h * 17 + Objects.hashCode(variable);
    h = h * 17 + sortOrder;
    h = h * 17 + Objects.hashCode(values);
    h = h * 17 + Objects.hashCode(regexCheck);
    h = h * 17 + Objects.hashCode(hint);
    h = h * 17 + Objects.hashCode(defaultValue);
    h = h * 17 + Booleans.hashCode(fixed);
    h = h * 17 + Objects.hashCode(contentTypeId);
    h = h * 17 + iDate.hashCode();
    return h;
  }

  /**
   * Prints the immutable value {@code PermissionTabField} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper("PermissionTabField")
        .omitNullValues()
        .add("acceptedDataTypes", acceptedDataTypes)
        .add("dataType", dataType)
        .add("searchable", searchable)
        .add("unique", unique)
        .add("indexed", indexed)
        .add("listed", listed)
        .add("readOnly", readOnly)
        .add("owner", owner)
        .add("id", id)
        .add("modDate", modDate)
        .add("name", name)
        .add("typeName", typeName)
        .add("relationType", relationType)
        .add("required", required)
        .add("variable", variable)
        .add("sortOrder", sortOrder)
        .add("values", values)
        .add("regexCheck", regexCheck)
        .add("hint", hint)
        .add("defaultValue", defaultValue)
        .add("fixed", fixed)
        .add("contentTypeId", contentTypeId)
        .add("iDate", iDate)
        .toString();
  }

  /**
   * Utility type used to correctly read immutable object from JSON representation.
   * @deprecated Do not use this type directly, it exists only for the <em>Jackson</em>-binding infrastructure
   */
  @Deprecated
  @JsonDeserialize
  @JsonTypeInfo(use=JsonTypeInfo.Id.NONE)
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
  static final class Json extends PermissionTabField {
    @javax.annotation.Nullable DataTypes dataType;
    boolean searchable;
    boolean searchableIsSet;
    boolean unique;
    boolean uniqueIsSet;
    boolean indexed;
    boolean indexedIsSet;
    boolean listed;
    boolean listedIsSet;
    boolean readOnly;
    boolean readOnlyIsSet;
    @javax.annotation.Nullable String owner;
    @javax.annotation.Nullable String id;
    @javax.annotation.Nullable Date modDate;
    @javax.annotation.Nullable String name;
    @javax.annotation.Nullable String relationType;
    boolean required;
    boolean requiredIsSet;
    @javax.annotation.Nullable String variable;
    int sortOrder;
    boolean sortOrderIsSet;
    @javax.annotation.Nullable String values;
    @javax.annotation.Nullable String regexCheck;
    @javax.annotation.Nullable String hint;
    @javax.annotation.Nullable String defaultValue;
    boolean fixed;
    boolean fixedIsSet;
    @javax.annotation.Nullable String contentTypeId;
    @javax.annotation.Nullable String dbColumn;
    @javax.annotation.Nullable Date iDate;
    @JsonProperty("dataType")
    public void setDataType(DataTypes dataType) {
      this.dataType = dataType;
    }
    @JsonProperty("searchable")
    public void setSearchable(boolean searchable) {
      this.searchable = searchable;
      this.searchableIsSet = true;
    }
    @JsonProperty("unique")
    public void setUnique(boolean unique) {
      this.unique = unique;
      this.uniqueIsSet = true;
    }
    @JsonProperty("indexed")
    public void setIndexed(boolean indexed) {
      this.indexed = indexed;
      this.indexedIsSet = true;
    }
    @JsonProperty("listed")
    public void setListed(boolean listed) {
      this.listed = listed;
      this.listedIsSet = true;
    }
    @JsonProperty("readOnly")
    public void setReadOnly(boolean readOnly) {
      this.readOnly = readOnly;
      this.readOnlyIsSet = true;
    }
    @JsonProperty("owner")
    public void setOwner(@Nullable String owner) {
      this.owner = owner;
    }
    @JsonProperty("id")
    public void setId(@Nullable String id) {
      this.id = id;
    }
    @JsonProperty("modDate")
    public void setModDate(Date modDate) {
      this.modDate = modDate;
    }
    @JsonProperty("name")
    public void setName(String name) {
      this.name = name;
    }
    @JsonProperty("relationType")
    public void setRelationType(@Nullable String relationType) {
      this.relationType = relationType;
    }
    @JsonProperty("required")
    public void setRequired(boolean required) {
      this.required = required;
      this.requiredIsSet = true;
    }
    @JsonProperty("variable")
    public void setVariable(@Nullable String variable) {
      this.variable = variable;
    }
    @JsonProperty("sortOrder")
    public void setSortOrder(int sortOrder) {
      this.sortOrder = sortOrder;
      this.sortOrderIsSet = true;
    }
    @JsonProperty("values")
    public void setValues(@Nullable String values) {
      this.values = values;
    }
    @JsonProperty("regexCheck")
    public void setRegexCheck(@Nullable String regexCheck) {
      this.regexCheck = regexCheck;
    }
    @JsonProperty("hint")
    public void setHint(@Nullable String hint) {
      this.hint = hint;
    }
    @JsonProperty("defaultValue")
    public void setDefaultValue(@Nullable String defaultValue) {
      this.defaultValue = defaultValue;
    }
    @JsonProperty("fixed")
    public void setFixed(boolean fixed) {
      this.fixed = fixed;
      this.fixedIsSet = true;
    }
    @JsonProperty("contentTypeId")
    public void setContentTypeId(@Nullable String contentTypeId) {
      this.contentTypeId = contentTypeId;
    }
    @JsonProperty("dbColumn")
    public void setDbColumn(@Nullable String dbColumn) {
      this.dbColumn = dbColumn;
    }
    @JsonProperty("iDate")
    public void setIDate(Date iDate) {
      this.iDate = iDate;
    }
    @Override
    public List<DataTypes> acceptedDataTypes() { throw new UnsupportedOperationException(); }
    @Override
    public DataTypes dataType() { throw new UnsupportedOperationException(); }
    @Override
    public boolean searchable() { throw new UnsupportedOperationException(); }
    @Override
    public boolean unique() { throw new UnsupportedOperationException(); }
    @Override
    public boolean indexed() { throw new UnsupportedOperationException(); }
    @Override
    public boolean listed() { throw new UnsupportedOperationException(); }
    @Override
    public boolean readOnly() { throw new UnsupportedOperationException(); }
    @Override
    public String owner() { throw new UnsupportedOperationException(); }
    @Override
    public String id() { throw new UnsupportedOperationException(); }
    @Override
    public Date modDate() { throw new UnsupportedOperationException(); }
    @Override
    public String name() { throw new UnsupportedOperationException(); }
    @Override
    public String typeName() { throw new UnsupportedOperationException(); }
    @Override
    public String relationType() { throw new UnsupportedOperationException(); }
    @Override
    public boolean required() { throw new UnsupportedOperationException(); }
    @Override
    public String variable() { throw new UnsupportedOperationException(); }
    @Override
    public int sortOrder() { throw new UnsupportedOperationException(); }
    @Override
    public String values() { throw new UnsupportedOperationException(); }
    @Override
    public String regexCheck() { throw new UnsupportedOperationException(); }
    @Override
    public String hint() { throw new UnsupportedOperationException(); }
    @Override
    public String defaultValue() { throw new UnsupportedOperationException(); }
    @Override
    public boolean fixed() { throw new UnsupportedOperationException(); }
    @Override
    public String contentTypeId() { throw new UnsupportedOperationException(); }
    @Override
    public String dbColumn() { throw new UnsupportedOperationException(); }
    @Override
    public Date iDate() { throw new UnsupportedOperationException(); }
    @Override
    public String inode() { throw new UnsupportedOperationException(); }
    @Override
    public List<SelectableValue> selectableValues() { throw new UnsupportedOperationException(); }
    @Override
    public List<FieldVariable> fieldVariables() { throw new UnsupportedOperationException(); }
    @Override
    public Map<String, FieldVariable> fieldVariablesMap() { throw new UnsupportedOperationException(); }
    @Override
    public FieldFormRenderer formRenderer() { throw new UnsupportedOperationException(); }
    @Override
    public FieldValueRenderer valueRenderer() { throw new UnsupportedOperationException(); }
    @Override
    public FieldValueRenderer listRenderer() { throw new UnsupportedOperationException(); }
  }

  /**
   * @param json A JSON-bindable data structure
   * @return An immutable value type
   * @deprecated Do not use this method directly, it exists only for the <em>Jackson</em>-binding infrastructure
   */
  @Deprecated
  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  static ImmutablePermissionTabField fromJson(Json json) {
    ImmutablePermissionTabField.Builder builder = ImmutablePermissionTabField.builder();
    if (json.dataType != null) {
      builder.dataType(json.dataType);
    }
    if (json.searchableIsSet) {
      builder.searchable(json.searchable);
    }
    if (json.uniqueIsSet) {
      builder.unique(json.unique);
    }
    if (json.indexedIsSet) {
      builder.indexed(json.indexed);
    }
    if (json.listedIsSet) {
      builder.listed(json.listed);
    }
    if (json.readOnlyIsSet) {
      builder.readOnly(json.readOnly);
    }
    if (json.owner != null) {
      builder.owner(json.owner);
    }
    if (json.id != null) {
      builder.id(json.id);
    }
    if (json.modDate != null) {
      builder.modDate(json.modDate);
    }
    if (json.name != null) {
      builder.name(json.name);
    }
    if (json.relationType != null) {
      builder.relationType(json.relationType);
    }
    if (json.requiredIsSet) {
      builder.required(json.required);
    }
    if (json.variable != null) {
      builder.variable(json.variable);
    }
    if (json.sortOrderIsSet) {
      builder.sortOrder(json.sortOrder);
    }
    if (json.values != null) {
      builder.values(json.values);
    }
    if (json.regexCheck != null) {
      builder.regexCheck(json.regexCheck);
    }
    if (json.hint != null) {
      builder.hint(json.hint);
    }
    if (json.defaultValue != null) {
      builder.defaultValue(json.defaultValue);
    }
    if (json.fixedIsSet) {
      builder.fixed(json.fixed);
    }
    if (json.contentTypeId != null) {
      builder.contentTypeId(json.contentTypeId);
    }
    if (json.dbColumn != null) {
      builder.dbColumn(json.dbColumn);
    }
    if (json.iDate != null) {
      builder.iDate(json.iDate);
    }
    return builder.build();
  }

  private transient volatile long lazyInitBitmap;

  private static final long INODE_LAZY_INIT_BIT = 0x1L;

  private transient String inode;

  /**
   * {@inheritDoc}
   * <p>
   * Returns a lazily initialized value of the {@link PermissionTabField#inode() inode} attribute.
   * Initialized once and only once and stored for subsequent access with proper synchronization.
   * @return A lazily initialized value of the {@code l.name} attribute
   */
  @Override
  public String inode() {
    if ((lazyInitBitmap & INODE_LAZY_INIT_BIT) == 0) {
      synchronized (this) {
        if ((lazyInitBitmap & INODE_LAZY_INIT_BIT) == 0) {
          this.inode = Preconditions.checkNotNull(super.inode(), "inode");
          lazyInitBitmap |= INODE_LAZY_INIT_BIT;
        }
      }
    }
    return inode;
  }

  private static final long SELECTABLE_VALUES_LAZY_INIT_BIT = 0x2L;

  private transient List<SelectableValue> selectableValues;

  /**
   * {@inheritDoc}
   * <p>
   * Returns a lazily initialized value of the {@link PermissionTabField#selectableValues() selectableValues} attribute.
   * Initialized once and only once and stored for subsequent access with proper synchronization.
   * @return A lazily initialized value of the {@code l.name} attribute
   */
  @Override
  public List<SelectableValue> selectableValues() {
    if ((lazyInitBitmap & SELECTABLE_VALUES_LAZY_INIT_BIT) == 0) {
      synchronized (this) {
        if ((lazyInitBitmap & SELECTABLE_VALUES_LAZY_INIT_BIT) == 0) {
          this.selectableValues = Preconditions.checkNotNull(super.selectableValues(), "selectableValues");
          lazyInitBitmap |= SELECTABLE_VALUES_LAZY_INIT_BIT;
        }
      }
    }
    return selectableValues;
  }

  private static final long FIELD_VARIABLES_LAZY_INIT_BIT = 0x4L;

  private transient List<FieldVariable> fieldVariables;

  /**
   * {@inheritDoc}
   * <p>
   * Returns a lazily initialized value of the {@link PermissionTabField#fieldVariables() fieldVariables} attribute.
   * Initialized once and only once and stored for subsequent access with proper synchronization.
   * @return A lazily initialized value of the {@code l.name} attribute
   */
  @Override
  public List<FieldVariable> fieldVariables() {
    if ((lazyInitBitmap & FIELD_VARIABLES_LAZY_INIT_BIT) == 0) {
      synchronized (this) {
        if ((lazyInitBitmap & FIELD_VARIABLES_LAZY_INIT_BIT) == 0) {
          this.fieldVariables = Preconditions.checkNotNull(super.fieldVariables(), "fieldVariables");
          lazyInitBitmap |= FIELD_VARIABLES_LAZY_INIT_BIT;
        }
      }
    }
    return fieldVariables;
  }

  private static final long FIELD_VARIABLES_MAP_LAZY_INIT_BIT = 0x8L;

  private transient Map<String, FieldVariable> fieldVariablesMap;

  /**
   * {@inheritDoc}
   * <p>
   * Returns a lazily initialized value of the {@link PermissionTabField#fieldVariablesMap() fieldVariablesMap} attribute.
   * Initialized once and only once and stored for subsequent access with proper synchronization.
   * @return A lazily initialized value of the {@code l.name} attribute
   */
  @Override
  public Map<String, FieldVariable> fieldVariablesMap() {
    if ((lazyInitBitmap & FIELD_VARIABLES_MAP_LAZY_INIT_BIT) == 0) {
      synchronized (this) {
        if ((lazyInitBitmap & FIELD_VARIABLES_MAP_LAZY_INIT_BIT) == 0) {
          this.fieldVariablesMap = Preconditions.checkNotNull(super.fieldVariablesMap(), "fieldVariablesMap");
          lazyInitBitmap |= FIELD_VARIABLES_MAP_LAZY_INIT_BIT;
        }
      }
    }
    return fieldVariablesMap;
  }

  private static final long FORM_RENDERER_LAZY_INIT_BIT = 0x10L;

  private transient FieldFormRenderer formRenderer;

  /**
   * {@inheritDoc}
   * <p>
   * Returns a lazily initialized value of the {@link PermissionTabField#formRenderer() formRenderer} attribute.
   * Initialized once and only once and stored for subsequent access with proper synchronization.
   * @return A lazily initialized value of the {@code l.name} attribute
   */
  @Override
  public FieldFormRenderer formRenderer() {
    if ((lazyInitBitmap & FORM_RENDERER_LAZY_INIT_BIT) == 0) {
      synchronized (this) {
        if ((lazyInitBitmap & FORM_RENDERER_LAZY_INIT_BIT) == 0) {
          this.formRenderer = Preconditions.checkNotNull(super.formRenderer(), "formRenderer");
          lazyInitBitmap |= FORM_RENDERER_LAZY_INIT_BIT;
        }
      }
    }
    return formRenderer;
  }

  private static final long VALUE_RENDERER_LAZY_INIT_BIT = 0x20L;

  private transient FieldValueRenderer valueRenderer;

  /**
   * {@inheritDoc}
   * <p>
   * Returns a lazily initialized value of the {@link PermissionTabField#valueRenderer() valueRenderer} attribute.
   * Initialized once and only once and stored for subsequent access with proper synchronization.
   * @return A lazily initialized value of the {@code l.name} attribute
   */
  @Override
  public FieldValueRenderer valueRenderer() {
    if ((lazyInitBitmap & VALUE_RENDERER_LAZY_INIT_BIT) == 0) {
      synchronized (this) {
        if ((lazyInitBitmap & VALUE_RENDERER_LAZY_INIT_BIT) == 0) {
          this.valueRenderer = Preconditions.checkNotNull(super.valueRenderer(), "valueRenderer");
          lazyInitBitmap |= VALUE_RENDERER_LAZY_INIT_BIT;
        }
      }
    }
    return valueRenderer;
  }

  private static final long LIST_RENDERER_LAZY_INIT_BIT = 0x40L;

  private transient FieldValueRenderer listRenderer;

  /**
   * {@inheritDoc}
   * <p>
   * Returns a lazily initialized value of the {@link PermissionTabField#listRenderer() listRenderer} attribute.
   * Initialized once and only once and stored for subsequent access with proper synchronization.
   * @return A lazily initialized value of the {@code l.name} attribute
   */
  @Override
  public FieldValueRenderer listRenderer() {
    if ((lazyInitBitmap & LIST_RENDERER_LAZY_INIT_BIT) == 0) {
      synchronized (this) {
        if ((lazyInitBitmap & LIST_RENDERER_LAZY_INIT_BIT) == 0) {
          this.listRenderer = Preconditions.checkNotNull(super.listRenderer(), "listRenderer");
          lazyInitBitmap |= LIST_RENDERER_LAZY_INIT_BIT;
        }
      }
    }
    return listRenderer;
  }

  private static ImmutablePermissionTabField validate(ImmutablePermissionTabField instance) {
    instance.check();
    return instance;
  }

  /**
   * Creates an immutable copy of a {@link PermissionTabField} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable PermissionTabField instance
   */
  public static ImmutablePermissionTabField copyOf(PermissionTabField instance) {
    if (instance instanceof ImmutablePermissionTabField) {
      return (ImmutablePermissionTabField) instance;
    }
    return ImmutablePermissionTabField.builder()
        .from(instance)
        .build();
  }

  private static final long serialVersionUID = 1L;

  private Object readResolve() throws ObjectStreamException {
    return validate(this);
  }

  /**
   * Creates a builder for {@link ImmutablePermissionTabField ImmutablePermissionTabField}.
   * @return A new ImmutablePermissionTabField builder
   */
  public static ImmutablePermissionTabField.Builder builder() {
    return new ImmutablePermissionTabField.Builder();
  }

  /**
   * Builds instances of type {@link ImmutablePermissionTabField ImmutablePermissionTabField}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  @NotThreadSafe
  public static final class Builder 
      extends PermissionTabField.Builder {
    private static final long INIT_BIT_NAME = 0x1L;
    private static final long OPT_BIT_SEARCHABLE = 0x1L;
    private static final long OPT_BIT_UNIQUE = 0x2L;
    private static final long OPT_BIT_INDEXED = 0x4L;
    private static final long OPT_BIT_LISTED = 0x8L;
    private static final long OPT_BIT_READ_ONLY = 0x10L;
    private static final long OPT_BIT_REQUIRED = 0x20L;
    private static final long OPT_BIT_SORT_ORDER = 0x40L;
    private static final long OPT_BIT_FIXED = 0x80L;
    private long initBits = 0x1L;
    private long optBits;

    private @javax.annotation.Nullable DataTypes dataType;
    private boolean searchable;
    private boolean unique;
    private boolean indexed;
    private boolean listed;
    private boolean readOnly;
    private @javax.annotation.Nullable String owner;
    private @javax.annotation.Nullable String id;
    private @javax.annotation.Nullable Date modDate;
    private @javax.annotation.Nullable String name;
    private @javax.annotation.Nullable String relationType;
    private boolean required;
    private @javax.annotation.Nullable String variable;
    private int sortOrder;
    private @javax.annotation.Nullable String values;
    private @javax.annotation.Nullable String regexCheck;
    private @javax.annotation.Nullable String hint;
    private @javax.annotation.Nullable String defaultValue;
    private boolean fixed;
    private @javax.annotation.Nullable String contentTypeId;
    private @javax.annotation.Nullable String dbColumn;
    private @javax.annotation.Nullable Date iDate;

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code com.dotcms.contenttype.model.field.Field} instance.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(Field instance) {
      Preconditions.checkNotNull(instance, "instance");
      from((Object) instance);
      return this;
    }

    /**
     * Fill a builder with attribute values from the provided {@code com.dotcms.contenttype.model.field.PermissionTabField} instance.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(PermissionTabField instance) {
      Preconditions.checkNotNull(instance, "instance");
      from((Object) instance);
      return this;
    }

    private void from(Object object) {
      long bits = 0;
      if (object instanceof Field) {
        Field instance = (Field) object;
        String ownerValue = instance.owner();
        if (ownerValue != null) {
          owner(ownerValue);
        }
        String relationTypeValue = instance.relationType();
        if (relationTypeValue != null) {
          relationType(relationTypeValue);
        }
        modDate(instance.modDate());
        indexed(instance.indexed());
        String defaultValueValue = instance.defaultValue();
        if (defaultValueValue != null) {
          defaultValue(defaultValueValue);
        }
        if ((bits & 0x1L) == 0) {
          dataType(instance.dataType());
          bits |= 0x1L;
        }
        String valuesValue = instance.values();
        if (valuesValue != null) {
          values(valuesValue);
        }
        String regexCheckValue = instance.regexCheck();
        if (regexCheckValue != null) {
          regexCheck(regexCheckValue);
        }
        String contentTypeIdValue = instance.contentTypeId();
        if (contentTypeIdValue != null) {
          contentTypeId(contentTypeIdValue);
        }
        readOnly(instance.readOnly());
        searchable(instance.searchable());
        required(instance.required());
        String dbColumnValue = instance.dbColumn();
        if (dbColumnValue != null) {
          dbColumn(dbColumnValue);
        }
        listed(instance.listed());
        unique(instance.unique());
        sortOrder(instance.sortOrder());
        String hintValue = instance.hint();
        if (hintValue != null) {
          hint(hintValue);
        }
        name(instance.name());
        String variableValue = instance.variable();
        if (variableValue != null) {
          variable(variableValue);
        }
        fixed(instance.fixed());
        String idValue = instance.id();
        if (idValue != null) {
          id(idValue);
        }
        iDate(instance.iDate());
      }
      if (object instanceof PermissionTabField) {
        PermissionTabField instance = (PermissionTabField) object;
        if ((bits & 0x1L) == 0) {
          dataType(instance.dataType());
          bits |= 0x1L;
        }
      }
    }

    /**
     * Initializes the value for the {@link PermissionTabField#dataType() dataType} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link PermissionTabField#dataType() dataType}.</em>
     * @param dataType The value for dataType 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder dataType(DataTypes dataType) {
      this.dataType = Preconditions.checkNotNull(dataType, "dataType");
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#searchable() searchable} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link PermissionTabField#searchable() searchable}.</em>
     * @param searchable The value for searchable 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder searchable(boolean searchable) {
      this.searchable = searchable;
      optBits |= OPT_BIT_SEARCHABLE;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#unique() unique} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link PermissionTabField#unique() unique}.</em>
     * @param unique The value for unique 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder unique(boolean unique) {
      this.unique = unique;
      optBits |= OPT_BIT_UNIQUE;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#indexed() indexed} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link PermissionTabField#indexed() indexed}.</em>
     * @param indexed The value for indexed 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder indexed(boolean indexed) {
      this.indexed = indexed;
      optBits |= OPT_BIT_INDEXED;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#listed() listed} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link PermissionTabField#listed() listed}.</em>
     * @param listed The value for listed 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder listed(boolean listed) {
      this.listed = listed;
      optBits |= OPT_BIT_LISTED;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#readOnly() readOnly} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link PermissionTabField#readOnly() readOnly}.</em>
     * @param readOnly The value for readOnly 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder readOnly(boolean readOnly) {
      this.readOnly = readOnly;
      optBits |= OPT_BIT_READ_ONLY;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#owner() owner} attribute.
     * @param owner The value for owner (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder owner(@Nullable String owner) {
      this.owner = owner;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#id() id} attribute.
     * @param id The value for id (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder id(@Nullable String id) {
      this.id = id;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#modDate() modDate} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link PermissionTabField#modDate() modDate}.</em>
     * @param modDate The value for modDate 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder modDate(Date modDate) {
      this.modDate = Preconditions.checkNotNull(modDate, "modDate");
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#name() name} attribute.
     * @param name The value for name 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder name(String name) {
      this.name = Preconditions.checkNotNull(name, "name");
      initBits &= ~INIT_BIT_NAME;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#relationType() relationType} attribute.
     * @param relationType The value for relationType (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder relationType(@Nullable String relationType) {
      this.relationType = relationType;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#required() required} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link PermissionTabField#required() required}.</em>
     * @param required The value for required 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder required(boolean required) {
      this.required = required;
      optBits |= OPT_BIT_REQUIRED;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#variable() variable} attribute.
     * @param variable The value for variable (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder variable(@Nullable String variable) {
      this.variable = variable;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#sortOrder() sortOrder} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link PermissionTabField#sortOrder() sortOrder}.</em>
     * @param sortOrder The value for sortOrder 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder sortOrder(int sortOrder) {
      this.sortOrder = sortOrder;
      optBits |= OPT_BIT_SORT_ORDER;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#values() values} attribute.
     * @param values The value for values (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder values(@Nullable String values) {
      this.values = values;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#regexCheck() regexCheck} attribute.
     * @param regexCheck The value for regexCheck (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder regexCheck(@Nullable String regexCheck) {
      this.regexCheck = regexCheck;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#hint() hint} attribute.
     * @param hint The value for hint (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder hint(@Nullable String hint) {
      this.hint = hint;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#defaultValue() defaultValue} attribute.
     * @param defaultValue The value for defaultValue (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder defaultValue(@Nullable String defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#fixed() fixed} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link PermissionTabField#fixed() fixed}.</em>
     * @param fixed The value for fixed 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder fixed(boolean fixed) {
      this.fixed = fixed;
      optBits |= OPT_BIT_FIXED;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#contentTypeId() contentTypeId} attribute.
     * @param contentTypeId The value for contentTypeId (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder contentTypeId(@Nullable String contentTypeId) {
      this.contentTypeId = contentTypeId;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#dbColumn() dbColumn} attribute.
     * @param dbColumn The value for dbColumn (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder dbColumn(@Nullable String dbColumn) {
      this.dbColumn = dbColumn;
      return this;
    }

    /**
     * Initializes the value for the {@link PermissionTabField#iDate() iDate} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link PermissionTabField#iDate() iDate}.</em>
     * @param iDate The value for iDate 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder iDate(Date iDate) {
      this.iDate = Preconditions.checkNotNull(iDate, "iDate");
      return this;
    }

    /**
     * Builds a new {@link ImmutablePermissionTabField ImmutablePermissionTabField}.
     * @return An immutable instance of PermissionTabField
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutablePermissionTabField build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return ImmutablePermissionTabField.validate(new ImmutablePermissionTabField(this));
    }

    private boolean searchableIsSet() {
      return (optBits & OPT_BIT_SEARCHABLE) != 0;
    }

    private boolean uniqueIsSet() {
      return (optBits & OPT_BIT_UNIQUE) != 0;
    }

    private boolean indexedIsSet() {
      return (optBits & OPT_BIT_INDEXED) != 0;
    }

    private boolean listedIsSet() {
      return (optBits & OPT_BIT_LISTED) != 0;
    }

    private boolean readOnlyIsSet() {
      return (optBits & OPT_BIT_READ_ONLY) != 0;
    }

    private boolean requiredIsSet() {
      return (optBits & OPT_BIT_REQUIRED) != 0;
    }

    private boolean sortOrderIsSet() {
      return (optBits & OPT_BIT_SORT_ORDER) != 0;
    }

    private boolean fixedIsSet() {
      return (optBits & OPT_BIT_FIXED) != 0;
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = Lists.newArrayList();
      if ((initBits & INIT_BIT_NAME) != 0) attributes.add("name");
      return "Cannot build PermissionTabField, some of required attributes are not set " + attributes;
    }
  }
}
