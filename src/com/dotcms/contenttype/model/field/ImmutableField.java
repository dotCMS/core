package com.dotcms.contenttype.model.field;

import com.dotcms.contenttype.model.decorator.FieldDecorator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.annotation.Generated;
import org.elasticsearch.common.Nullable;

/**
 * Immutable implementation of {@link Field}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutableField.builder()}.
 */
@SuppressWarnings("all")
@Generated({"Immutables.generator", "Field"})
public final class ImmutableField extends Field {
  private final String type;
  private final @Nullable String owner;
  private final @Nullable String inode;
  private final Date modDate;
  private final String name;
  private final @Nullable String relationType;
  private final String variable;
  private final int sortOrder;
  private final @Nullable String values;
  private final @Nullable String regexCheck;
  private final @Nullable String hint;
  private final @Nullable String defaultValue;
  private final boolean indexed;
  private final boolean listed;
  private final boolean fixed;
  private final boolean readOnly;
  private final boolean searchable;
  private final boolean unique;
  private final List<FieldDecorator> fieldDecorators;
  private final List<DataTypes> acceptedDataTypes;
  private final DataTypes dataType;
  private final String contentTypeId;
  private final FieldTypes fieldType;
  private final @Nullable String dbColumn;

  private ImmutableField(ImmutableField.Builder builder) {
    this.owner = builder.owner;
    this.inode = builder.inode;
    this.name = builder.name;
    this.relationType = builder.relationType;
    this.variable = builder.variable;
    this.values = builder.values;
    this.regexCheck = builder.regexCheck;
    this.hint = builder.hint;
    this.defaultValue = builder.defaultValue;
    this.acceptedDataTypes = createUnmodifiableList(true, builder.acceptedDataTypes);
    this.dataType = builder.dataType;
    this.contentTypeId = builder.contentTypeId;
    this.fieldType = builder.fieldType;
    this.dbColumn = builder.dbColumn;
    if (builder.modDate != null) {
      initShim.modDate(builder.modDate);
    }
    if (builder.sortOrderIsSet()) {
      initShim.sortOrder(builder.sortOrder);
    }
    if (builder.indexedIsSet()) {
      initShim.indexed(builder.indexed);
    }
    if (builder.listedIsSet()) {
      initShim.listed(builder.listed);
    }
    if (builder.fixedIsSet()) {
      initShim.fixed(builder.fixed);
    }
    if (builder.readOnlyIsSet()) {
      initShim.readOnly(builder.readOnly);
    }
    if (builder.searchableIsSet()) {
      initShim.searchable(builder.searchable);
    }
    if (builder.uniqueIsSet()) {
      initShim.unique(builder.unique);
    }
    if (builder.fieldDecoratorsIsSet()) {
      initShim.fieldDecorators(createUnmodifiableList(true, builder.fieldDecorators));
    }
    this.modDate = initShim.modDate();
    this.sortOrder = initShim.sortOrder();
    this.indexed = initShim.indexed();
    this.listed = initShim.listed();
    this.fixed = initShim.fixed();
    this.readOnly = initShim.readOnly();
    this.searchable = initShim.searchable();
    this.unique = initShim.unique();
    this.fieldDecorators = initShim.fieldDecorators();
    this.type = initShim.type();
    this.initShim = null;
  }

  private ImmutableField(
      @Nullable String owner,
      @Nullable String inode,
      Date modDate,
      String name,
      @Nullable String relationType,
      String variable,
      int sortOrder,
      @Nullable String values,
      @Nullable String regexCheck,
      @Nullable String hint,
      @Nullable String defaultValue,
      boolean indexed,
      boolean listed,
      boolean fixed,
      boolean readOnly,
      boolean searchable,
      boolean unique,
      List<FieldDecorator> fieldDecorators,
      List<DataTypes> acceptedDataTypes,
      DataTypes dataType,
      String contentTypeId,
      FieldTypes fieldType,
      @Nullable String dbColumn) {
    this.owner = owner;
    this.inode = inode;
    this.modDate = modDate;
    this.name = name;
    this.relationType = relationType;
    this.variable = variable;
    this.sortOrder = sortOrder;
    this.values = values;
    this.regexCheck = regexCheck;
    this.hint = hint;
    this.defaultValue = defaultValue;
    this.indexed = indexed;
    this.listed = listed;
    this.fixed = fixed;
    this.readOnly = readOnly;
    this.searchable = searchable;
    this.unique = unique;
    this.fieldDecorators = fieldDecorators;
    this.acceptedDataTypes = acceptedDataTypes;
    this.dataType = dataType;
    this.contentTypeId = contentTypeId;
    this.fieldType = fieldType;
    this.dbColumn = dbColumn;
    initShim.modDate(this.modDate);
    initShim.sortOrder(this.sortOrder);
    initShim.indexed(this.indexed);
    initShim.listed(this.listed);
    initShim.fixed(this.fixed);
    initShim.readOnly(this.readOnly);
    initShim.searchable(this.searchable);
    initShim.unique(this.unique);
    initShim.fieldDecorators(this.fieldDecorators);
    this.type = initShim.type();
    this.initShim = null;
  }

  private static final int STAGE_INITIALIZING = -1;
  private static final int STAGE_UNINITIALIZED = 0;
  private static final int STAGE_INITIALIZED = 1;
  private transient volatile InitShim initShim = new InitShim();

  private final class InitShim {
    private String type;
    private int typeStage;

    String type() {
      if (typeStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (typeStage == STAGE_UNINITIALIZED) {
        typeStage = STAGE_INITIALIZING;
        this.type = Objects.requireNonNull(ImmutableField.super.type(), "type");
        typeStage = STAGE_INITIALIZED;
      }
      return this.type;
    }
    private Date modDate;
    private int modDateStage;

    Date modDate() {
      if (modDateStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (modDateStage == STAGE_UNINITIALIZED) {
        modDateStage = STAGE_INITIALIZING;
        this.modDate = Objects.requireNonNull(ImmutableField.super.modDate(), "modDate");
        modDateStage = STAGE_INITIALIZED;
      }
      return this.modDate;
    }

    void modDate(Date modDate) {
      this.modDate = modDate;
      modDateStage = STAGE_INITIALIZED;
    }
    private int sortOrder;
    private int sortOrderStage;

    int sortOrder() {
      if (sortOrderStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (sortOrderStage == STAGE_UNINITIALIZED) {
        sortOrderStage = STAGE_INITIALIZING;
        this.sortOrder = ImmutableField.super.sortOrder();
        sortOrderStage = STAGE_INITIALIZED;
      }
      return this.sortOrder;
    }

    void sortOrder(int sortOrder) {
      this.sortOrder = sortOrder;
      sortOrderStage = STAGE_INITIALIZED;
    }
    private boolean indexed;
    private int indexedStage;

    boolean indexed() {
      if (indexedStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (indexedStage == STAGE_UNINITIALIZED) {
        indexedStage = STAGE_INITIALIZING;
        this.indexed = ImmutableField.super.indexed();
        indexedStage = STAGE_INITIALIZED;
      }
      return this.indexed;
    }

    void indexed(boolean indexed) {
      this.indexed = indexed;
      indexedStage = STAGE_INITIALIZED;
    }
    private boolean listed;
    private int listedStage;

    boolean listed() {
      if (listedStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (listedStage == STAGE_UNINITIALIZED) {
        listedStage = STAGE_INITIALIZING;
        this.listed = ImmutableField.super.listed();
        listedStage = STAGE_INITIALIZED;
      }
      return this.listed;
    }

    void listed(boolean listed) {
      this.listed = listed;
      listedStage = STAGE_INITIALIZED;
    }
    private boolean fixed;
    private int fixedStage;

    boolean fixed() {
      if (fixedStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (fixedStage == STAGE_UNINITIALIZED) {
        fixedStage = STAGE_INITIALIZING;
        this.fixed = ImmutableField.super.fixed();
        fixedStage = STAGE_INITIALIZED;
      }
      return this.fixed;
    }

    void fixed(boolean fixed) {
      this.fixed = fixed;
      fixedStage = STAGE_INITIALIZED;
    }
    private boolean readOnly;
    private int readOnlyStage;

    boolean readOnly() {
      if (readOnlyStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (readOnlyStage == STAGE_UNINITIALIZED) {
        readOnlyStage = STAGE_INITIALIZING;
        this.readOnly = ImmutableField.super.readOnly();
        readOnlyStage = STAGE_INITIALIZED;
      }
      return this.readOnly;
    }

    void readOnly(boolean readOnly) {
      this.readOnly = readOnly;
      readOnlyStage = STAGE_INITIALIZED;
    }
    private boolean searchable;
    private int searchableStage;

    boolean searchable() {
      if (searchableStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (searchableStage == STAGE_UNINITIALIZED) {
        searchableStage = STAGE_INITIALIZING;
        this.searchable = ImmutableField.super.searchable();
        searchableStage = STAGE_INITIALIZED;
      }
      return this.searchable;
    }

    void searchable(boolean searchable) {
      this.searchable = searchable;
      searchableStage = STAGE_INITIALIZED;
    }
    private boolean unique;
    private int uniqueStage;

    boolean unique() {
      if (uniqueStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (uniqueStage == STAGE_UNINITIALIZED) {
        uniqueStage = STAGE_INITIALIZING;
        this.unique = ImmutableField.super.unique();
        uniqueStage = STAGE_INITIALIZED;
      }
      return this.unique;
    }

    void unique(boolean unique) {
      this.unique = unique;
      uniqueStage = STAGE_INITIALIZED;
    }
    private List<FieldDecorator> fieldDecorators;
    private int fieldDecoratorsStage;

    List<FieldDecorator> fieldDecorators() {
      if (fieldDecoratorsStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (fieldDecoratorsStage == STAGE_UNINITIALIZED) {
        fieldDecoratorsStage = STAGE_INITIALIZING;
        this.fieldDecorators = createUnmodifiableList(false, createSafeList(ImmutableField.super.fieldDecorators(), true, false));
        fieldDecoratorsStage = STAGE_INITIALIZED;
      }
      return this.fieldDecorators;
    }

    void fieldDecorators(List<FieldDecorator> fieldDecorators) {
      this.fieldDecorators = fieldDecorators;
      fieldDecoratorsStage = STAGE_INITIALIZED;
    }

    private String formatInitCycleMessage() {
      ArrayList<String> attributes = new ArrayList<String>();
      if (typeStage == STAGE_INITIALIZING) attributes.add("type");
      if (modDateStage == STAGE_INITIALIZING) attributes.add("modDate");
      if (sortOrderStage == STAGE_INITIALIZING) attributes.add("sortOrder");
      if (indexedStage == STAGE_INITIALIZING) attributes.add("indexed");
      if (listedStage == STAGE_INITIALIZING) attributes.add("listed");
      if (fixedStage == STAGE_INITIALIZING) attributes.add("fixed");
      if (readOnlyStage == STAGE_INITIALIZING) attributes.add("readOnly");
      if (searchableStage == STAGE_INITIALIZING) attributes.add("searchable");
      if (uniqueStage == STAGE_INITIALIZING) attributes.add("unique");
      if (fieldDecoratorsStage == STAGE_INITIALIZING) attributes.add("fieldDecorators");
      return "Cannot build Field, attribute initializers form cycle" + attributes;
    }
  }

  /**
   * @return The computed-at-construction value of the {@code type} attribute
   */
  @Override
  public String type() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.type()
        : this.type;
  }

  /**
   * @return The value of the {@code owner} attribute
   */
  @Override
  public @Nullable String owner() {
    return owner;
  }

  /**
   * @return The value of the {@code inode} attribute
   */
  @Override
  public @Nullable String inode() {
    return inode;
  }

  /**
   * @return The value of the {@code modDate} attribute
   */
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
  @Override
  public String name() {
    return name;
  }

  /**
   * @return The value of the {@code relationType} attribute
   */
  @Override
  public @Nullable String relationType() {
    return relationType;
  }

  /**
   * @return The value of the {@code variable} attribute
   */
  @Override
  public String variable() {
    return variable;
  }

  /**
   * @return The value of the {@code sortOrder} attribute
   */
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
  @Override
  public @Nullable String values() {
    return values;
  }

  /**
   * @return The value of the {@code regexCheck} attribute
   */
  @Override
  public @Nullable String regexCheck() {
    return regexCheck;
  }

  /**
   * @return The value of the {@code hint} attribute
   */
  @Override
  public @Nullable String hint() {
    return hint;
  }

  /**
   * @return The value of the {@code defaultValue} attribute
   */
  @Override
  public @Nullable String defaultValue() {
    return defaultValue;
  }

  /**
   * @return The value of the {@code indexed} attribute
   */
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
  @Override
  public boolean listed() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.listed()
        : this.listed;
  }

  /**
   * @return The value of the {@code fixed} attribute
   */
  @Override
  public boolean fixed() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.fixed()
        : this.fixed;
  }

  /**
   * @return The value of the {@code readOnly} attribute
   */
  @Override
  public boolean readOnly() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.readOnly()
        : this.readOnly;
  }

  /**
   * @return The value of the {@code searchable} attribute
   */
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
  @Override
  public boolean unique() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.unique()
        : this.unique;
  }

  /**
   * @return The value of the {@code fieldDecorators} attribute
   */
  @Override
  public List<FieldDecorator> fieldDecorators() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.fieldDecorators()
        : this.fieldDecorators;
  }

  /**
   * @return The value of the {@code acceptedDataTypes} attribute
   */
  @Override
  public List<DataTypes> acceptedDataTypes() {
    return acceptedDataTypes;
  }

  /**
   * @return The value of the {@code dataType} attribute
   */
  @Override
  public DataTypes dataType() {
    return dataType;
  }

  /**
   * @return The value of the {@code contentTypeId} attribute
   */
  @Override
  public String contentTypeId() {
    return contentTypeId;
  }

  /**
   * @return The value of the {@code fieldType} attribute
   */
  @Override
  public FieldTypes fieldType() {
    return fieldType;
  }

  /**
   * @return The value of the {@code dbColumn} attribute
   */
  @Override
  public @Nullable String dbColumn() {
    return dbColumn;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#owner() owner} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param owner A new value for owner (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withOwner(@Nullable String owner) {
    if (Objects.equals(this.owner, owner)) return this;
    return new ImmutableField(
        owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#inode() inode} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param inode A new value for inode (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withInode(@Nullable String inode) {
    if (Objects.equals(this.inode, inode)) return this;
    return new ImmutableField(
        this.owner,
        inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#modDate() modDate} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param modDate A new value for modDate
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withModDate(Date modDate) {
    if (this.modDate == modDate) return this;
    Date newValue = Objects.requireNonNull(modDate, "modDate");
    return new ImmutableField(
        this.owner,
        this.inode,
        newValue,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#name() name} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param name A new value for name
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withName(String name) {
    if (this.name.equals(name)) return this;
    String newValue = Objects.requireNonNull(name, "name");
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        newValue,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#relationType() relationType} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param relationType A new value for relationType (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withRelationType(@Nullable String relationType) {
    if (Objects.equals(this.relationType, relationType)) return this;
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#variable() variable} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param variable A new value for variable
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withVariable(String variable) {
    if (this.variable.equals(variable)) return this;
    String newValue = Objects.requireNonNull(variable, "variable");
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        newValue,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#sortOrder() sortOrder} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param sortOrder A new value for sortOrder
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withSortOrder(int sortOrder) {
    if (this.sortOrder == sortOrder) return this;
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#values() values} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param values A new value for values (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withValues(@Nullable String values) {
    if (Objects.equals(this.values, values)) return this;
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#regexCheck() regexCheck} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param regexCheck A new value for regexCheck (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withRegexCheck(@Nullable String regexCheck) {
    if (Objects.equals(this.regexCheck, regexCheck)) return this;
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#hint() hint} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param hint A new value for hint (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withHint(@Nullable String hint) {
    if (Objects.equals(this.hint, hint)) return this;
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#defaultValue() defaultValue} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param defaultValue A new value for defaultValue (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withDefaultValue(@Nullable String defaultValue) {
    if (Objects.equals(this.defaultValue, defaultValue)) return this;
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#indexed() indexed} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param indexed A new value for indexed
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withIndexed(boolean indexed) {
    if (this.indexed == indexed) return this;
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#listed() listed} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param listed A new value for listed
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withListed(boolean listed) {
    if (this.listed == listed) return this;
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#fixed() fixed} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param fixed A new value for fixed
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withFixed(boolean fixed) {
    if (this.fixed == fixed) return this;
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#readOnly() readOnly} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param readOnly A new value for readOnly
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withReadOnly(boolean readOnly) {
    if (this.readOnly == readOnly) return this;
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#searchable() searchable} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param searchable A new value for searchable
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withSearchable(boolean searchable) {
    if (this.searchable == searchable) return this;
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#unique() unique} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param unique A new value for unique
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withUnique(boolean unique) {
    if (this.unique == unique) return this;
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link Field#fieldDecorators() fieldDecorators}.
   * @param elements The elements to set
   * @return A modified copy of {@code this} object
   */
  public final ImmutableField withFieldDecorators(FieldDecorator... elements) {
    List<FieldDecorator> newValue = createUnmodifiableList(false, createSafeList(Arrays.asList(elements), true, false));
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        newValue,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link Field#fieldDecorators() fieldDecorators}.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param elements An iterable of fieldDecorators elements to set
   * @return A modified copy of {@code this} object
   */
  public final ImmutableField withFieldDecorators(Iterable<? extends FieldDecorator> elements) {
    if (this.fieldDecorators == elements) return this;
    List<FieldDecorator> newValue = createUnmodifiableList(false, createSafeList(elements, true, false));
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        newValue,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link Field#acceptedDataTypes() acceptedDataTypes}.
   * @param elements The elements to set
   * @return A modified copy of {@code this} object
   */
  public final ImmutableField withAcceptedDataTypes(DataTypes... elements) {
    List<DataTypes> newValue = createUnmodifiableList(false, createSafeList(Arrays.asList(elements), true, false));
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        newValue,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link Field#acceptedDataTypes() acceptedDataTypes}.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param elements An iterable of acceptedDataTypes elements to set
   * @return A modified copy of {@code this} object
   */
  public final ImmutableField withAcceptedDataTypes(Iterable<? extends DataTypes> elements) {
    if (this.acceptedDataTypes == elements) return this;
    List<DataTypes> newValue = createUnmodifiableList(false, createSafeList(elements, true, false));
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        newValue,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#dataType() dataType} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param dataType A new value for dataType
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withDataType(DataTypes dataType) {
    if (this.dataType == dataType) return this;
    DataTypes newValue = Objects.requireNonNull(dataType, "dataType");
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        newValue,
        this.contentTypeId,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#contentTypeId() contentTypeId} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param contentTypeId A new value for contentTypeId
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withContentTypeId(String contentTypeId) {
    if (this.contentTypeId.equals(contentTypeId)) return this;
    String newValue = Objects.requireNonNull(contentTypeId, "contentTypeId");
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        newValue,
        this.fieldType,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#fieldType() fieldType} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param fieldType A new value for fieldType
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withFieldType(FieldTypes fieldType) {
    if (this.fieldType == fieldType) return this;
    FieldTypes newValue = Objects.requireNonNull(fieldType, "fieldType");
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        newValue,
        this.dbColumn);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Field#dbColumn() dbColumn} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param dbColumn A new value for dbColumn (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableField withDbColumn(@Nullable String dbColumn) {
    if (Objects.equals(this.dbColumn, dbColumn)) return this;
    return new ImmutableField(
        this.owner,
        this.inode,
        this.modDate,
        this.name,
        this.relationType,
        this.variable,
        this.sortOrder,
        this.values,
        this.regexCheck,
        this.hint,
        this.defaultValue,
        this.indexed,
        this.listed,
        this.fixed,
        this.readOnly,
        this.searchable,
        this.unique,
        this.fieldDecorators,
        this.acceptedDataTypes,
        this.dataType,
        this.contentTypeId,
        this.fieldType,
        dbColumn);
  }

  /**
   * This instance is equal to all instances of {@code ImmutableField} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(Object another) {
    if (this == another) return true;
    return another instanceof ImmutableField
        && equalTo((ImmutableField) another);
  }

  private boolean equalTo(ImmutableField another) {
    return type.equals(another.type)
        && Objects.equals(owner, another.owner)
        && Objects.equals(inode, another.inode)
        && modDate.equals(another.modDate)
        && name.equals(another.name)
        && Objects.equals(relationType, another.relationType)
        && variable.equals(another.variable)
        && sortOrder == another.sortOrder
        && Objects.equals(values, another.values)
        && Objects.equals(regexCheck, another.regexCheck)
        && Objects.equals(hint, another.hint)
        && Objects.equals(defaultValue, another.defaultValue)
        && indexed == another.indexed
        && listed == another.listed
        && fixed == another.fixed
        && readOnly == another.readOnly
        && searchable == another.searchable
        && unique == another.unique
        && fieldDecorators.equals(another.fieldDecorators)
        && acceptedDataTypes.equals(another.acceptedDataTypes)
        && dataType.equals(another.dataType)
        && contentTypeId.equals(another.contentTypeId)
        && fieldType.equals(another.fieldType)
        && Objects.equals(dbColumn, another.dbColumn);
  }

  /**
   * Computes a hash code from attributes: {@code type}, {@code owner}, {@code inode}, {@code modDate}, {@code name}, {@code relationType}, {@code variable}, {@code sortOrder}, {@code values}, {@code regexCheck}, {@code hint}, {@code defaultValue}, {@code indexed}, {@code listed}, {@code fixed}, {@code readOnly}, {@code searchable}, {@code unique}, {@code fieldDecorators}, {@code acceptedDataTypes}, {@code dataType}, {@code contentTypeId}, {@code fieldType}, {@code dbColumn}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 31;
    h = h * 17 + type.hashCode();
    h = h * 17 + Objects.hashCode(owner);
    h = h * 17 + Objects.hashCode(inode);
    h = h * 17 + modDate.hashCode();
    h = h * 17 + name.hashCode();
    h = h * 17 + Objects.hashCode(relationType);
    h = h * 17 + variable.hashCode();
    h = h * 17 + sortOrder;
    h = h * 17 + Objects.hashCode(values);
    h = h * 17 + Objects.hashCode(regexCheck);
    h = h * 17 + Objects.hashCode(hint);
    h = h * 17 + Objects.hashCode(defaultValue);
    h = h * 17 + (indexed ? 1231 : 1237);
    h = h * 17 + (listed ? 1231 : 1237);
    h = h * 17 + (fixed ? 1231 : 1237);
    h = h * 17 + (readOnly ? 1231 : 1237);
    h = h * 17 + (searchable ? 1231 : 1237);
    h = h * 17 + (unique ? 1231 : 1237);
    h = h * 17 + fieldDecorators.hashCode();
    h = h * 17 + acceptedDataTypes.hashCode();
    h = h * 17 + dataType.hashCode();
    h = h * 17 + contentTypeId.hashCode();
    h = h * 17 + fieldType.hashCode();
    h = h * 17 + Objects.hashCode(dbColumn);
    return h;
  }

  /**
   * Prints the immutable value {@code Field} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return "Field{"
        + "type=" + type
        + ", owner=" + owner
        + ", inode=" + inode
        + ", modDate=" + modDate
        + ", name=" + name
        + ", relationType=" + relationType
        + ", variable=" + variable
        + ", sortOrder=" + sortOrder
        + ", values=" + values
        + ", regexCheck=" + regexCheck
        + ", hint=" + hint
        + ", defaultValue=" + defaultValue
        + ", indexed=" + indexed
        + ", listed=" + listed
        + ", fixed=" + fixed
        + ", readOnly=" + readOnly
        + ", searchable=" + searchable
        + ", unique=" + unique
        + ", fieldDecorators=" + fieldDecorators
        + ", acceptedDataTypes=" + acceptedDataTypes
        + ", dataType=" + dataType
        + ", contentTypeId=" + contentTypeId
        + ", fieldType=" + fieldType
        + ", dbColumn=" + dbColumn
        + "}";
  }

  /**
   * Creates an immutable copy of a {@link Field} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable Field instance
   */
  public static ImmutableField copyOf(Field instance) {
    if (instance instanceof ImmutableField) {
      return (ImmutableField) instance;
    }
    return ImmutableField.builder()
        .from(instance)
        .build();
  }

  private static final long serialVersionUID = 1L;

  /**
   * Creates a builder for {@link ImmutableField ImmutableField}.
   * @return A new ImmutableField builder
   */
  public static ImmutableField.Builder builder() {
    return new ImmutableField.Builder();
  }

  /**
   * Builds instances of type {@link ImmutableField ImmutableField}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  public static final class Builder {
    private static final long INIT_BIT_NAME = 0x1L;
    private static final long INIT_BIT_VARIABLE = 0x2L;
    private static final long INIT_BIT_DATA_TYPE = 0x4L;
    private static final long INIT_BIT_CONTENT_TYPE_ID = 0x8L;
    private static final long INIT_BIT_FIELD_TYPE = 0x10L;
    private static final long OPT_BIT_SORT_ORDER = 0x1L;
    private static final long OPT_BIT_INDEXED = 0x2L;
    private static final long OPT_BIT_LISTED = 0x4L;
    private static final long OPT_BIT_FIXED = 0x8L;
    private static final long OPT_BIT_READ_ONLY = 0x10L;
    private static final long OPT_BIT_SEARCHABLE = 0x20L;
    private static final long OPT_BIT_UNIQUE = 0x40L;
    private static final long OPT_BIT_FIELD_DECORATORS = 0x80L;
    private long initBits = 0x1fL;
    private long optBits;

    private String owner;
    private String inode;
    private Date modDate;
    private String name;
    private String relationType;
    private String variable;
    private int sortOrder;
    private String values;
    private String regexCheck;
    private String hint;
    private String defaultValue;
    private boolean indexed;
    private boolean listed;
    private boolean fixed;
    private boolean readOnly;
    private boolean searchable;
    private boolean unique;
    private List<FieldDecorator> fieldDecorators = new ArrayList<FieldDecorator>();
    private List<DataTypes> acceptedDataTypes = new ArrayList<DataTypes>();
    private DataTypes dataType;
    private String contentTypeId;
    private FieldTypes fieldType;
    private String dbColumn;

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code Field} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * Collection elements and entries will be added, not replaced.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(Field instance) {
      Objects.requireNonNull(instance, "instance");
      String ownerValue = instance.owner();
      if (ownerValue != null) {
        owner(ownerValue);
      }
      String inodeValue = instance.inode();
      if (inodeValue != null) {
        inode(inodeValue);
      }
      modDate(instance.modDate());
      name(instance.name());
      String relationTypeValue = instance.relationType();
      if (relationTypeValue != null) {
        relationType(relationTypeValue);
      }
      variable(instance.variable());
      sortOrder(instance.sortOrder());
      String valuesValue = instance.values();
      if (valuesValue != null) {
        values(valuesValue);
      }
      String regexCheckValue = instance.regexCheck();
      if (regexCheckValue != null) {
        regexCheck(regexCheckValue);
      }
      String hintValue = instance.hint();
      if (hintValue != null) {
        hint(hintValue);
      }
      String defaultValueValue = instance.defaultValue();
      if (defaultValueValue != null) {
        defaultValue(defaultValueValue);
      }
      indexed(instance.indexed());
      listed(instance.listed());
      fixed(instance.fixed());
      readOnly(instance.readOnly());
      searchable(instance.searchable());
      unique(instance.unique());
      addAllFieldDecorators(instance.fieldDecorators());
      addAllAcceptedDataTypes(instance.acceptedDataTypes());
      dataType(instance.dataType());
      contentTypeId(instance.contentTypeId());
      fieldType(instance.fieldType());
      String dbColumnValue = instance.dbColumn();
      if (dbColumnValue != null) {
        dbColumn(dbColumnValue);
      }
      return this;
    }

    /**
     * Initializes the value for the {@link Field#owner() owner} attribute.
     * @param owner The value for owner (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder owner(@Nullable String owner) {
      this.owner = owner;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#inode() inode} attribute.
     * @param inode The value for inode (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder inode(@Nullable String inode) {
      this.inode = inode;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#modDate() modDate} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link Field#modDate() modDate}.</em>
     * @param modDate The value for modDate 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder modDate(Date modDate) {
      this.modDate = Objects.requireNonNull(modDate, "modDate");
      return this;
    }

    /**
     * Initializes the value for the {@link Field#name() name} attribute.
     * @param name The value for name 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder name(String name) {
      this.name = Objects.requireNonNull(name, "name");
      initBits &= ~INIT_BIT_NAME;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#relationType() relationType} attribute.
     * @param relationType The value for relationType (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder relationType(@Nullable String relationType) {
      this.relationType = relationType;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#variable() variable} attribute.
     * @param variable The value for variable 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder variable(String variable) {
      this.variable = Objects.requireNonNull(variable, "variable");
      initBits &= ~INIT_BIT_VARIABLE;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#sortOrder() sortOrder} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link Field#sortOrder() sortOrder}.</em>
     * @param sortOrder The value for sortOrder 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder sortOrder(int sortOrder) {
      this.sortOrder = sortOrder;
      optBits |= OPT_BIT_SORT_ORDER;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#values() values} attribute.
     * @param values The value for values (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder values(@Nullable String values) {
      this.values = values;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#regexCheck() regexCheck} attribute.
     * @param regexCheck The value for regexCheck (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder regexCheck(@Nullable String regexCheck) {
      this.regexCheck = regexCheck;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#hint() hint} attribute.
     * @param hint The value for hint (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder hint(@Nullable String hint) {
      this.hint = hint;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#defaultValue() defaultValue} attribute.
     * @param defaultValue The value for defaultValue (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder defaultValue(@Nullable String defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#indexed() indexed} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link Field#indexed() indexed}.</em>
     * @param indexed The value for indexed 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder indexed(boolean indexed) {
      this.indexed = indexed;
      optBits |= OPT_BIT_INDEXED;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#listed() listed} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link Field#listed() listed}.</em>
     * @param listed The value for listed 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder listed(boolean listed) {
      this.listed = listed;
      optBits |= OPT_BIT_LISTED;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#fixed() fixed} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link Field#fixed() fixed}.</em>
     * @param fixed The value for fixed 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder fixed(boolean fixed) {
      this.fixed = fixed;
      optBits |= OPT_BIT_FIXED;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#readOnly() readOnly} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link Field#readOnly() readOnly}.</em>
     * @param readOnly The value for readOnly 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder readOnly(boolean readOnly) {
      this.readOnly = readOnly;
      optBits |= OPT_BIT_READ_ONLY;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#searchable() searchable} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link Field#searchable() searchable}.</em>
     * @param searchable The value for searchable 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder searchable(boolean searchable) {
      this.searchable = searchable;
      optBits |= OPT_BIT_SEARCHABLE;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#unique() unique} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link Field#unique() unique}.</em>
     * @param unique The value for unique 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder unique(boolean unique) {
      this.unique = unique;
      optBits |= OPT_BIT_UNIQUE;
      return this;
    }

    /**
     * Adds one element to {@link Field#fieldDecorators() fieldDecorators} list.
     * @param element A fieldDecorators element
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addFieldDecorators(FieldDecorator element) {
      this.fieldDecorators.add(Objects.requireNonNull(element, "fieldDecorators element"));
      optBits |= OPT_BIT_FIELD_DECORATORS;
      return this;
    }

    /**
     * Adds elements to {@link Field#fieldDecorators() fieldDecorators} list.
     * @param elements An array of fieldDecorators elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addFieldDecorators(FieldDecorator... elements) {
      for (FieldDecorator element : elements) {
        this.fieldDecorators.add(Objects.requireNonNull(element, "fieldDecorators element"));
      }
      optBits |= OPT_BIT_FIELD_DECORATORS;
      return this;
    }

    /**
     * Sets or replaces all elements for {@link Field#fieldDecorators() fieldDecorators} list.
     * @param elements An iterable of fieldDecorators elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder fieldDecorators(Iterable<? extends FieldDecorator> elements) {
      this.fieldDecorators.clear();
      return addAllFieldDecorators(elements);
    }

    /**
     * Adds elements to {@link Field#fieldDecorators() fieldDecorators} list.
     * @param elements An iterable of fieldDecorators elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addAllFieldDecorators(Iterable<? extends FieldDecorator> elements) {
      for (FieldDecorator element : elements) {
        this.fieldDecorators.add(Objects.requireNonNull(element, "fieldDecorators element"));
      }
      optBits |= OPT_BIT_FIELD_DECORATORS;
      return this;
    }

    /**
     * Adds one element to {@link Field#acceptedDataTypes() acceptedDataTypes} list.
     * @param element A acceptedDataTypes element
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addAcceptedDataTypes(DataTypes element) {
      this.acceptedDataTypes.add(Objects.requireNonNull(element, "acceptedDataTypes element"));
      return this;
    }

    /**
     * Adds elements to {@link Field#acceptedDataTypes() acceptedDataTypes} list.
     * @param elements An array of acceptedDataTypes elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addAcceptedDataTypes(DataTypes... elements) {
      for (DataTypes element : elements) {
        this.acceptedDataTypes.add(Objects.requireNonNull(element, "acceptedDataTypes element"));
      }
      return this;
    }

    /**
     * Sets or replaces all elements for {@link Field#acceptedDataTypes() acceptedDataTypes} list.
     * @param elements An iterable of acceptedDataTypes elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder acceptedDataTypes(Iterable<? extends DataTypes> elements) {
      this.acceptedDataTypes.clear();
      return addAllAcceptedDataTypes(elements);
    }

    /**
     * Adds elements to {@link Field#acceptedDataTypes() acceptedDataTypes} list.
     * @param elements An iterable of acceptedDataTypes elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addAllAcceptedDataTypes(Iterable<? extends DataTypes> elements) {
      for (DataTypes element : elements) {
        this.acceptedDataTypes.add(Objects.requireNonNull(element, "acceptedDataTypes element"));
      }
      return this;
    }

    /**
     * Initializes the value for the {@link Field#dataType() dataType} attribute.
     * @param dataType The value for dataType 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder dataType(DataTypes dataType) {
      this.dataType = Objects.requireNonNull(dataType, "dataType");
      initBits &= ~INIT_BIT_DATA_TYPE;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#contentTypeId() contentTypeId} attribute.
     * @param contentTypeId The value for contentTypeId 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder contentTypeId(String contentTypeId) {
      this.contentTypeId = Objects.requireNonNull(contentTypeId, "contentTypeId");
      initBits &= ~INIT_BIT_CONTENT_TYPE_ID;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#fieldType() fieldType} attribute.
     * @param fieldType The value for fieldType 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder fieldType(FieldTypes fieldType) {
      this.fieldType = Objects.requireNonNull(fieldType, "fieldType");
      initBits &= ~INIT_BIT_FIELD_TYPE;
      return this;
    }

    /**
     * Initializes the value for the {@link Field#dbColumn() dbColumn} attribute.
     * @param dbColumn The value for dbColumn (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder dbColumn(@Nullable String dbColumn) {
      this.dbColumn = dbColumn;
      return this;
    }

    /**
     * Builds a new {@link ImmutableField ImmutableField}.
     * @return An immutable instance of Field
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutableField build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return new ImmutableField(this);
    }

    private boolean sortOrderIsSet() {
      return (optBits & OPT_BIT_SORT_ORDER) != 0;
    }

    private boolean indexedIsSet() {
      return (optBits & OPT_BIT_INDEXED) != 0;
    }

    private boolean listedIsSet() {
      return (optBits & OPT_BIT_LISTED) != 0;
    }

    private boolean fixedIsSet() {
      return (optBits & OPT_BIT_FIXED) != 0;
    }

    private boolean readOnlyIsSet() {
      return (optBits & OPT_BIT_READ_ONLY) != 0;
    }

    private boolean searchableIsSet() {
      return (optBits & OPT_BIT_SEARCHABLE) != 0;
    }

    private boolean uniqueIsSet() {
      return (optBits & OPT_BIT_UNIQUE) != 0;
    }

    private boolean fieldDecoratorsIsSet() {
      return (optBits & OPT_BIT_FIELD_DECORATORS) != 0;
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = new ArrayList<String>();
      if ((initBits & INIT_BIT_NAME) != 0) attributes.add("name");
      if ((initBits & INIT_BIT_VARIABLE) != 0) attributes.add("variable");
      if ((initBits & INIT_BIT_DATA_TYPE) != 0) attributes.add("dataType");
      if ((initBits & INIT_BIT_CONTENT_TYPE_ID) != 0) attributes.add("contentTypeId");
      if ((initBits & INIT_BIT_FIELD_TYPE) != 0) attributes.add("fieldType");
      return "Cannot build Field, some of required attributes are not set " + attributes;
    }
  }

  private static <T> List<T> createSafeList(Iterable<? extends T> iterable, boolean checkNulls, boolean skipNulls) {
    ArrayList<T> list;
    if (iterable instanceof Collection<?>) {
      int size = ((Collection<?>) iterable).size();
      if (size == 0) return Collections.emptyList();
      list = new ArrayList<T>();
    } else {
      list = new ArrayList<T>();
    }
    for (T element : iterable) {
      if (skipNulls && element == null) continue;
      if (checkNulls) Objects.requireNonNull(element, "element");
      list.add(element);
    }
    return list;
  }

  private static <T> List<T> createUnmodifiableList(boolean clone, List<T> list) {
    switch(list.size()) {
    case 0: return Collections.emptyList();
    case 1: return Collections.singletonList(list.get(0));
    default:
      if (clone) {
        return Collections.unmodifiableList(new ArrayList<T>(list));
      } else {
        if (list instanceof ArrayList<?>) {
          ((ArrayList<?>) list).trimToSize();
        }
        return Collections.unmodifiableList(list);
      }
    }
  }
}
