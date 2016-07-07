package com.dotcms.contenttype.model.type;

import com.dotcms.contenttype.model.field.Field;
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
 * Immutable implementation of {@link ContentType}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutableContentType.builder()}.
 */
@SuppressWarnings("all")
@Generated({"Immutables.generator", "ContentType"})
public final class ImmutableContentType extends ContentType {
  private final String name;
  private final @Nullable String inode;
  private final @Nullable String description;
  private final boolean defaultStructure;
  private final StorageType storageType;
  private final @Nullable String pagedetail;
  private final boolean fixed;
  private final Date iDate;
  private final boolean system;
  private final boolean versionable;
  private final boolean multilingualable;
  private final String velocityVarName;
  private final @Nullable String urlMapPattern;
  private final @Nullable String publishDateVar;
  private final @Nullable String expireDateVar;
  private final @Nullable String owner;
  private final Date modDate;
  private final BaseContentTypes baseType;
  private final String host;
  private final String folder;
  private final List<Field> requiredFields;

  private ImmutableContentType(ImmutableContentType.Builder builder) {
    this.name = builder.name;
    this.inode = builder.inode;
    this.description = builder.description;
    this.pagedetail = builder.pagedetail;
    this.velocityVarName = builder.velocityVarName;
    this.urlMapPattern = builder.urlMapPattern;
    this.publishDateVar = builder.publishDateVar;
    this.expireDateVar = builder.expireDateVar;
    this.owner = builder.owner;
    this.baseType = builder.baseType;
    if (builder.defaultStructureIsSet()) {
      initShim.defaultStructure(builder.defaultStructure);
    }
    if (builder.storageType != null) {
      initShim.storageType(builder.storageType);
    }
    if (builder.fixedIsSet()) {
      initShim.fixed(builder.fixed);
    }
    if (builder.iDate != null) {
      initShim.iDate(builder.iDate);
    }
    if (builder.systemIsSet()) {
      initShim.system(builder.system);
    }
    if (builder.versionableIsSet()) {
      initShim.versionable(builder.versionable);
    }
    if (builder.multilingualableIsSet()) {
      initShim.multilingualable(builder.multilingualable);
    }
    if (builder.modDate != null) {
      initShim.modDate(builder.modDate);
    }
    if (builder.host != null) {
      initShim.host(builder.host);
    }
    if (builder.folder != null) {
      initShim.folder(builder.folder);
    }
    if (builder.requiredFieldsIsSet()) {
      initShim.requiredFields(createUnmodifiableList(true, builder.requiredFields));
    }
    this.defaultStructure = initShim.defaultStructure();
    this.storageType = initShim.storageType();
    this.fixed = initShim.fixed();
    this.iDate = initShim.iDate();
    this.system = initShim.system();
    this.versionable = initShim.versionable();
    this.multilingualable = initShim.multilingualable();
    this.modDate = initShim.modDate();
    this.host = initShim.host();
    this.folder = initShim.folder();
    this.requiredFields = initShim.requiredFields();
    this.initShim = null;
  }

  private ImmutableContentType(
      String name,
      @Nullable String inode,
      @Nullable String description,
      boolean defaultStructure,
      StorageType storageType,
      @Nullable String pagedetail,
      boolean fixed,
      Date iDate,
      boolean system,
      boolean versionable,
      boolean multilingualable,
      String velocityVarName,
      @Nullable String urlMapPattern,
      @Nullable String publishDateVar,
      @Nullable String expireDateVar,
      @Nullable String owner,
      Date modDate,
      BaseContentTypes baseType,
      String host,
      String folder,
      List<Field> requiredFields) {
    this.name = name;
    this.inode = inode;
    this.description = description;
    this.defaultStructure = defaultStructure;
    this.storageType = storageType;
    this.pagedetail = pagedetail;
    this.fixed = fixed;
    this.iDate = iDate;
    this.system = system;
    this.versionable = versionable;
    this.multilingualable = multilingualable;
    this.velocityVarName = velocityVarName;
    this.urlMapPattern = urlMapPattern;
    this.publishDateVar = publishDateVar;
    this.expireDateVar = expireDateVar;
    this.owner = owner;
    this.modDate = modDate;
    this.baseType = baseType;
    this.host = host;
    this.folder = folder;
    this.requiredFields = requiredFields;
    this.initShim = null;
  }

  private static final int STAGE_INITIALIZING = -1;
  private static final int STAGE_UNINITIALIZED = 0;
  private static final int STAGE_INITIALIZED = 1;
  private transient volatile InitShim initShim = new InitShim();

  private final class InitShim {
    private boolean defaultStructure;
    private int defaultStructureStage;

    boolean defaultStructure() {
      if (defaultStructureStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (defaultStructureStage == STAGE_UNINITIALIZED) {
        defaultStructureStage = STAGE_INITIALIZING;
        this.defaultStructure = ImmutableContentType.super.defaultStructure();
        defaultStructureStage = STAGE_INITIALIZED;
      }
      return this.defaultStructure;
    }

    void defaultStructure(boolean defaultStructure) {
      this.defaultStructure = defaultStructure;
      defaultStructureStage = STAGE_INITIALIZED;
    }
    private StorageType storageType;
    private int storageTypeStage;

    StorageType storageType() {
      if (storageTypeStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (storageTypeStage == STAGE_UNINITIALIZED) {
        storageTypeStage = STAGE_INITIALIZING;
        this.storageType = Objects.requireNonNull(ImmutableContentType.super.storageType(), "storageType");
        storageTypeStage = STAGE_INITIALIZED;
      }
      return this.storageType;
    }

    void storageType(StorageType storageType) {
      this.storageType = storageType;
      storageTypeStage = STAGE_INITIALIZED;
    }
    private boolean fixed;
    private int fixedStage;

    boolean fixed() {
      if (fixedStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (fixedStage == STAGE_UNINITIALIZED) {
        fixedStage = STAGE_INITIALIZING;
        this.fixed = ImmutableContentType.super.fixed();
        fixedStage = STAGE_INITIALIZED;
      }
      return this.fixed;
    }

    void fixed(boolean fixed) {
      this.fixed = fixed;
      fixedStage = STAGE_INITIALIZED;
    }
    private Date iDate;
    private int iDateStage;

    Date iDate() {
      if (iDateStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (iDateStage == STAGE_UNINITIALIZED) {
        iDateStage = STAGE_INITIALIZING;
        this.iDate = Objects.requireNonNull(ImmutableContentType.super.iDate(), "iDate");
        iDateStage = STAGE_INITIALIZED;
      }
      return this.iDate;
    }

    void iDate(Date iDate) {
      this.iDate = iDate;
      iDateStage = STAGE_INITIALIZED;
    }
    private boolean system;
    private int systemStage;

    boolean system() {
      if (systemStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (systemStage == STAGE_UNINITIALIZED) {
        systemStage = STAGE_INITIALIZING;
        this.system = ImmutableContentType.super.system();
        systemStage = STAGE_INITIALIZED;
      }
      return this.system;
    }

    void system(boolean system) {
      this.system = system;
      systemStage = STAGE_INITIALIZED;
    }
    private boolean versionable;
    private int versionableStage;

    boolean versionable() {
      if (versionableStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (versionableStage == STAGE_UNINITIALIZED) {
        versionableStage = STAGE_INITIALIZING;
        this.versionable = ImmutableContentType.super.versionable();
        versionableStage = STAGE_INITIALIZED;
      }
      return this.versionable;
    }

    void versionable(boolean versionable) {
      this.versionable = versionable;
      versionableStage = STAGE_INITIALIZED;
    }
    private boolean multilingualable;
    private int multilingualableStage;

    boolean multilingualable() {
      if (multilingualableStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (multilingualableStage == STAGE_UNINITIALIZED) {
        multilingualableStage = STAGE_INITIALIZING;
        this.multilingualable = ImmutableContentType.super.multilingualable();
        multilingualableStage = STAGE_INITIALIZED;
      }
      return this.multilingualable;
    }

    void multilingualable(boolean multilingualable) {
      this.multilingualable = multilingualable;
      multilingualableStage = STAGE_INITIALIZED;
    }
    private Date modDate;
    private int modDateStage;

    Date modDate() {
      if (modDateStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (modDateStage == STAGE_UNINITIALIZED) {
        modDateStage = STAGE_INITIALIZING;
        this.modDate = Objects.requireNonNull(ImmutableContentType.super.modDate(), "modDate");
        modDateStage = STAGE_INITIALIZED;
      }
      return this.modDate;
    }

    void modDate(Date modDate) {
      this.modDate = modDate;
      modDateStage = STAGE_INITIALIZED;
    }
    private String host;
    private int hostStage;

    String host() {
      if (hostStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (hostStage == STAGE_UNINITIALIZED) {
        hostStage = STAGE_INITIALIZING;
        this.host = Objects.requireNonNull(ImmutableContentType.super.host(), "host");
        hostStage = STAGE_INITIALIZED;
      }
      return this.host;
    }

    void host(String host) {
      this.host = host;
      hostStage = STAGE_INITIALIZED;
    }
    private String folder;
    private int folderStage;

    String folder() {
      if (folderStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (folderStage == STAGE_UNINITIALIZED) {
        folderStage = STAGE_INITIALIZING;
        this.folder = Objects.requireNonNull(ImmutableContentType.super.folder(), "folder");
        folderStage = STAGE_INITIALIZED;
      }
      return this.folder;
    }

    void folder(String folder) {
      this.folder = folder;
      folderStage = STAGE_INITIALIZED;
    }
    private List<Field> requiredFields;
    private int requiredFieldsStage;

    List<Field> requiredFields() {
      if (requiredFieldsStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (requiredFieldsStage == STAGE_UNINITIALIZED) {
        requiredFieldsStage = STAGE_INITIALIZING;
        this.requiredFields = createUnmodifiableList(false, createSafeList(ImmutableContentType.super.requiredFields(), true, false));
        requiredFieldsStage = STAGE_INITIALIZED;
      }
      return this.requiredFields;
    }

    void requiredFields(List<Field> requiredFields) {
      this.requiredFields = requiredFields;
      requiredFieldsStage = STAGE_INITIALIZED;
    }

    private String formatInitCycleMessage() {
      ArrayList<String> attributes = new ArrayList<String>();
      if (defaultStructureStage == STAGE_INITIALIZING) attributes.add("defaultStructure");
      if (storageTypeStage == STAGE_INITIALIZING) attributes.add("storageType");
      if (fixedStage == STAGE_INITIALIZING) attributes.add("fixed");
      if (iDateStage == STAGE_INITIALIZING) attributes.add("iDate");
      if (systemStage == STAGE_INITIALIZING) attributes.add("system");
      if (versionableStage == STAGE_INITIALIZING) attributes.add("versionable");
      if (multilingualableStage == STAGE_INITIALIZING) attributes.add("multilingualable");
      if (modDateStage == STAGE_INITIALIZING) attributes.add("modDate");
      if (hostStage == STAGE_INITIALIZING) attributes.add("host");
      if (folderStage == STAGE_INITIALIZING) attributes.add("folder");
      if (requiredFieldsStage == STAGE_INITIALIZING) attributes.add("requiredFields");
      return "Cannot build ContentType, attribute initializers form cycle" + attributes;
    }
  }

  /**
   * @return The value of the {@code name} attribute
   */
  @Override
  public String name() {
    return name;
  }

  /**
   * @return The value of the {@code inode} attribute
   */
  @Override
  public @Nullable String inode() {
    return inode;
  }

  /**
   * @return The value of the {@code description} attribute
   */
  @Override
  public @Nullable String description() {
    return description;
  }

  /**
   * @return The value of the {@code defaultStructure} attribute
   */
  @Override
  public boolean defaultStructure() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.defaultStructure()
        : this.defaultStructure;
  }

  /**
   * @return The value of the {@code storageType} attribute
   */
  @Override
  public StorageType storageType() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.storageType()
        : this.storageType;
  }

  /**
   * @return The value of the {@code pagedetail} attribute
   */
  @Override
  public @Nullable String pagedetail() {
    return pagedetail;
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
   * @return The value of the {@code iDate} attribute
   */
  @Override
  public Date iDate() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.iDate()
        : this.iDate;
  }

  /**
   * @return The value of the {@code system} attribute
   */
  @Override
  public boolean system() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.system()
        : this.system;
  }

  /**
   * @return The value of the {@code versionable} attribute
   */
  @Override
  public boolean versionable() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.versionable()
        : this.versionable;
  }

  /**
   * @return The value of the {@code multilingualable} attribute
   */
  @Override
  public boolean multilingualable() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.multilingualable()
        : this.multilingualable;
  }

  /**
   * @return The value of the {@code velocityVarName} attribute
   */
  @Override
  public String velocityVarName() {
    return velocityVarName;
  }

  /**
   * @return The value of the {@code urlMapPattern} attribute
   */
  @Override
  public @Nullable String urlMapPattern() {
    return urlMapPattern;
  }

  /**
   * @return The value of the {@code publishDateVar} attribute
   */
  @Override
  public @Nullable String publishDateVar() {
    return publishDateVar;
  }

  /**
   * @return The value of the {@code expireDateVar} attribute
   */
  @Override
  public @Nullable String expireDateVar() {
    return expireDateVar;
  }

  /**
   * @return The value of the {@code owner} attribute
   */
  @Override
  public @Nullable String owner() {
    return owner;
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
   * @return The value of the {@code baseType} attribute
   */
  @Override
  public BaseContentTypes baseType() {
    return baseType;
  }

  /**
   * @return The value of the {@code host} attribute
   */
  @Override
  public String host() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.host()
        : this.host;
  }

  /**
   * @return The value of the {@code folder} attribute
   */
  @Override
  public String folder() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.folder()
        : this.folder;
  }

  /**
   * @return The value of the {@code requiredFields} attribute
   */
  @Override
  public List<Field> requiredFields() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.requiredFields()
        : this.requiredFields;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#name() name} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param name A new value for name
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withName(String name) {
    if (this.name.equals(name)) return this;
    String newValue = Objects.requireNonNull(name, "name");
    return new ImmutableContentType(
        newValue,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#inode() inode} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param inode A new value for inode (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withInode(@Nullable String inode) {
    if (Objects.equals(this.inode, inode)) return this;
    return new ImmutableContentType(
        this.name,
        inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#description() description} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param description A new value for description (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withDescription(@Nullable String description) {
    if (Objects.equals(this.description, description)) return this;
    return new ImmutableContentType(
        this.name,
        this.inode,
        description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#defaultStructure() defaultStructure} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param defaultStructure A new value for defaultStructure
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withDefaultStructure(boolean defaultStructure) {
    if (this.defaultStructure == defaultStructure) return this;
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#storageType() storageType} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param storageType A new value for storageType
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withStorageType(StorageType storageType) {
    if (this.storageType == storageType) return this;
    StorageType newValue = Objects.requireNonNull(storageType, "storageType");
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        newValue,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#pagedetail() pagedetail} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param pagedetail A new value for pagedetail (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withPagedetail(@Nullable String pagedetail) {
    if (Objects.equals(this.pagedetail, pagedetail)) return this;
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#fixed() fixed} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param fixed A new value for fixed
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withFixed(boolean fixed) {
    if (this.fixed == fixed) return this;
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#iDate() iDate} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param iDate A new value for iDate
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withIDate(Date iDate) {
    if (this.iDate == iDate) return this;
    Date newValue = Objects.requireNonNull(iDate, "iDate");
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        newValue,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#system() system} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param system A new value for system
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withSystem(boolean system) {
    if (this.system == system) return this;
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#versionable() versionable} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param versionable A new value for versionable
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withVersionable(boolean versionable) {
    if (this.versionable == versionable) return this;
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#multilingualable() multilingualable} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param multilingualable A new value for multilingualable
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withMultilingualable(boolean multilingualable) {
    if (this.multilingualable == multilingualable) return this;
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#velocityVarName() velocityVarName} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param velocityVarName A new value for velocityVarName
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withVelocityVarName(String velocityVarName) {
    if (this.velocityVarName.equals(velocityVarName)) return this;
    String newValue = Objects.requireNonNull(velocityVarName, "velocityVarName");
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        newValue,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#urlMapPattern() urlMapPattern} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param urlMapPattern A new value for urlMapPattern (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withUrlMapPattern(@Nullable String urlMapPattern) {
    if (Objects.equals(this.urlMapPattern, urlMapPattern)) return this;
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#publishDateVar() publishDateVar} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param publishDateVar A new value for publishDateVar (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withPublishDateVar(@Nullable String publishDateVar) {
    if (Objects.equals(this.publishDateVar, publishDateVar)) return this;
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#expireDateVar() expireDateVar} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param expireDateVar A new value for expireDateVar (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withExpireDateVar(@Nullable String expireDateVar) {
    if (Objects.equals(this.expireDateVar, expireDateVar)) return this;
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#owner() owner} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param owner A new value for owner (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withOwner(@Nullable String owner) {
    if (Objects.equals(this.owner, owner)) return this;
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#modDate() modDate} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param modDate A new value for modDate
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withModDate(Date modDate) {
    if (this.modDate == modDate) return this;
    Date newValue = Objects.requireNonNull(modDate, "modDate");
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        newValue,
        this.baseType,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#baseType() baseType} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param baseType A new value for baseType
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withBaseType(BaseContentTypes baseType) {
    if (this.baseType == baseType) return this;
    BaseContentTypes newValue = Objects.requireNonNull(baseType, "baseType");
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        newValue,
        this.host,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#host() host} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param host A new value for host
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withHost(String host) {
    if (this.host.equals(host)) return this;
    String newValue = Objects.requireNonNull(host, "host");
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        newValue,
        this.folder,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentType#folder() folder} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param folder A new value for folder
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentType withFolder(String folder) {
    if (this.folder.equals(folder)) return this;
    String newValue = Objects.requireNonNull(folder, "folder");
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        newValue,
        this.requiredFields);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link ContentType#requiredFields() requiredFields}.
   * @param elements The elements to set
   * @return A modified copy of {@code this} object
   */
  public final ImmutableContentType withRequiredFields(Field... elements) {
    List<Field> newValue = createUnmodifiableList(false, createSafeList(Arrays.asList(elements), true, false));
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        newValue);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link ContentType#requiredFields() requiredFields}.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param elements An iterable of requiredFields elements to set
   * @return A modified copy of {@code this} object
   */
  public final ImmutableContentType withRequiredFields(Iterable<? extends Field> elements) {
    if (this.requiredFields == elements) return this;
    List<Field> newValue = createUnmodifiableList(false, createSafeList(elements, true, false));
    return new ImmutableContentType(
        this.name,
        this.inode,
        this.description,
        this.defaultStructure,
        this.storageType,
        this.pagedetail,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.multilingualable,
        this.velocityVarName,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.baseType,
        this.host,
        this.folder,
        newValue);
  }

  /**
   * This instance is equal to all instances of {@code ImmutableContentType} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(Object another) {
    if (this == another) return true;
    return another instanceof ImmutableContentType
        && equalTo((ImmutableContentType) another);
  }

  private boolean equalTo(ImmutableContentType another) {
    return name.equals(another.name)
        && Objects.equals(inode, another.inode)
        && Objects.equals(description, another.description)
        && defaultStructure == another.defaultStructure
        && storageType.equals(another.storageType)
        && Objects.equals(pagedetail, another.pagedetail)
        && fixed == another.fixed
        && iDate.equals(another.iDate)
        && system == another.system
        && versionable == another.versionable
        && multilingualable == another.multilingualable
        && velocityVarName.equals(another.velocityVarName)
        && Objects.equals(urlMapPattern, another.urlMapPattern)
        && Objects.equals(publishDateVar, another.publishDateVar)
        && Objects.equals(expireDateVar, another.expireDateVar)
        && Objects.equals(owner, another.owner)
        && modDate.equals(another.modDate)
        && baseType.equals(another.baseType)
        && host.equals(another.host)
        && folder.equals(another.folder)
        && requiredFields.equals(another.requiredFields);
  }

  /**
   * Computes a hash code from attributes: {@code name}, {@code inode}, {@code description}, {@code defaultStructure}, {@code storageType}, {@code pagedetail}, {@code fixed}, {@code iDate}, {@code system}, {@code versionable}, {@code multilingualable}, {@code velocityVarName}, {@code urlMapPattern}, {@code publishDateVar}, {@code expireDateVar}, {@code owner}, {@code modDate}, {@code baseType}, {@code host}, {@code folder}, {@code requiredFields}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 31;
    h = h * 17 + name.hashCode();
    h = h * 17 + Objects.hashCode(inode);
    h = h * 17 + Objects.hashCode(description);
    h = h * 17 + (defaultStructure ? 1231 : 1237);
    h = h * 17 + storageType.hashCode();
    h = h * 17 + Objects.hashCode(pagedetail);
    h = h * 17 + (fixed ? 1231 : 1237);
    h = h * 17 + iDate.hashCode();
    h = h * 17 + (system ? 1231 : 1237);
    h = h * 17 + (versionable ? 1231 : 1237);
    h = h * 17 + (multilingualable ? 1231 : 1237);
    h = h * 17 + velocityVarName.hashCode();
    h = h * 17 + Objects.hashCode(urlMapPattern);
    h = h * 17 + Objects.hashCode(publishDateVar);
    h = h * 17 + Objects.hashCode(expireDateVar);
    h = h * 17 + Objects.hashCode(owner);
    h = h * 17 + modDate.hashCode();
    h = h * 17 + baseType.hashCode();
    h = h * 17 + host.hashCode();
    h = h * 17 + folder.hashCode();
    h = h * 17 + requiredFields.hashCode();
    return h;
  }

  /**
   * Prints the immutable value {@code ContentType} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return "ContentType{"
        + "name=" + name
        + ", inode=" + inode
        + ", description=" + description
        + ", defaultStructure=" + defaultStructure
        + ", storageType=" + storageType
        + ", pagedetail=" + pagedetail
        + ", fixed=" + fixed
        + ", iDate=" + iDate
        + ", system=" + system
        + ", versionable=" + versionable
        + ", multilingualable=" + multilingualable
        + ", velocityVarName=" + velocityVarName
        + ", urlMapPattern=" + urlMapPattern
        + ", publishDateVar=" + publishDateVar
        + ", expireDateVar=" + expireDateVar
        + ", owner=" + owner
        + ", modDate=" + modDate
        + ", baseType=" + baseType
        + ", host=" + host
        + ", folder=" + folder
        + ", requiredFields=" + requiredFields
        + "}";
  }

  /**
   * Creates an immutable copy of a {@link ContentType} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable ContentType instance
   */
  public static ImmutableContentType copyOf(ContentType instance) {
    if (instance instanceof ImmutableContentType) {
      return (ImmutableContentType) instance;
    }
    return ImmutableContentType.builder()
        .from(instance)
        .build();
  }

  private static final long serialVersionUID = 1L;

  /**
   * Creates a builder for {@link ImmutableContentType ImmutableContentType}.
   * @return A new ImmutableContentType builder
   */
  public static ImmutableContentType.Builder builder() {
    return new ImmutableContentType.Builder();
  }

  /**
   * Builds instances of type {@link ImmutableContentType ImmutableContentType}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  public static final class Builder {
    private static final long INIT_BIT_NAME = 0x1L;
    private static final long INIT_BIT_VELOCITY_VAR_NAME = 0x2L;
    private static final long INIT_BIT_BASE_TYPE = 0x4L;
    private static final long OPT_BIT_DEFAULT_STRUCTURE = 0x1L;
    private static final long OPT_BIT_FIXED = 0x2L;
    private static final long OPT_BIT_SYSTEM = 0x4L;
    private static final long OPT_BIT_VERSIONABLE = 0x8L;
    private static final long OPT_BIT_MULTILINGUALABLE = 0x10L;
    private static final long OPT_BIT_REQUIRED_FIELDS = 0x20L;
    private long initBits = 0x7L;
    private long optBits;

    private String name;
    private String inode;
    private String description;
    private boolean defaultStructure;
    private StorageType storageType;
    private String pagedetail;
    private boolean fixed;
    private Date iDate;
    private boolean system;
    private boolean versionable;
    private boolean multilingualable;
    private String velocityVarName;
    private String urlMapPattern;
    private String publishDateVar;
    private String expireDateVar;
    private String owner;
    private Date modDate;
    private BaseContentTypes baseType;
    private String host;
    private String folder;
    private List<Field> requiredFields = new ArrayList<Field>();

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code ContentType} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * Collection elements and entries will be added, not replaced.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(ContentType instance) {
      Objects.requireNonNull(instance, "instance");
      name(instance.name());
      String inodeValue = instance.inode();
      if (inodeValue != null) {
        inode(inodeValue);
      }
      String descriptionValue = instance.description();
      if (descriptionValue != null) {
        description(descriptionValue);
      }
      defaultStructure(instance.defaultStructure());
      storageType(instance.storageType());
      String pagedetailValue = instance.pagedetail();
      if (pagedetailValue != null) {
        pagedetail(pagedetailValue);
      }
      fixed(instance.fixed());
      iDate(instance.iDate());
      system(instance.system());
      versionable(instance.versionable());
      multilingualable(instance.multilingualable());
      velocityVarName(instance.velocityVarName());
      String urlMapPatternValue = instance.urlMapPattern();
      if (urlMapPatternValue != null) {
        urlMapPattern(urlMapPatternValue);
      }
      String publishDateVarValue = instance.publishDateVar();
      if (publishDateVarValue != null) {
        publishDateVar(publishDateVarValue);
      }
      String expireDateVarValue = instance.expireDateVar();
      if (expireDateVarValue != null) {
        expireDateVar(expireDateVarValue);
      }
      String ownerValue = instance.owner();
      if (ownerValue != null) {
        owner(ownerValue);
      }
      modDate(instance.modDate());
      baseType(instance.baseType());
      host(instance.host());
      folder(instance.folder());
      addAllRequiredFields(instance.requiredFields());
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#name() name} attribute.
     * @param name The value for name 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder name(String name) {
      this.name = Objects.requireNonNull(name, "name");
      initBits &= ~INIT_BIT_NAME;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#inode() inode} attribute.
     * @param inode The value for inode (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder inode(@Nullable String inode) {
      this.inode = inode;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#description() description} attribute.
     * @param description The value for description (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder description(@Nullable String description) {
      this.description = description;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#defaultStructure() defaultStructure} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link ContentType#defaultStructure() defaultStructure}.</em>
     * @param defaultStructure The value for defaultStructure 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder defaultStructure(boolean defaultStructure) {
      this.defaultStructure = defaultStructure;
      optBits |= OPT_BIT_DEFAULT_STRUCTURE;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#storageType() storageType} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link ContentType#storageType() storageType}.</em>
     * @param storageType The value for storageType 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder storageType(StorageType storageType) {
      this.storageType = Objects.requireNonNull(storageType, "storageType");
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#pagedetail() pagedetail} attribute.
     * @param pagedetail The value for pagedetail (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder pagedetail(@Nullable String pagedetail) {
      this.pagedetail = pagedetail;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#fixed() fixed} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link ContentType#fixed() fixed}.</em>
     * @param fixed The value for fixed 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder fixed(boolean fixed) {
      this.fixed = fixed;
      optBits |= OPT_BIT_FIXED;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#iDate() iDate} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link ContentType#iDate() iDate}.</em>
     * @param iDate The value for iDate 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder iDate(Date iDate) {
      this.iDate = Objects.requireNonNull(iDate, "iDate");
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#system() system} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link ContentType#system() system}.</em>
     * @param system The value for system 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder system(boolean system) {
      this.system = system;
      optBits |= OPT_BIT_SYSTEM;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#versionable() versionable} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link ContentType#versionable() versionable}.</em>
     * @param versionable The value for versionable 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder versionable(boolean versionable) {
      this.versionable = versionable;
      optBits |= OPT_BIT_VERSIONABLE;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#multilingualable() multilingualable} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link ContentType#multilingualable() multilingualable}.</em>
     * @param multilingualable The value for multilingualable 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder multilingualable(boolean multilingualable) {
      this.multilingualable = multilingualable;
      optBits |= OPT_BIT_MULTILINGUALABLE;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#velocityVarName() velocityVarName} attribute.
     * @param velocityVarName The value for velocityVarName 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder velocityVarName(String velocityVarName) {
      this.velocityVarName = Objects.requireNonNull(velocityVarName, "velocityVarName");
      initBits &= ~INIT_BIT_VELOCITY_VAR_NAME;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#urlMapPattern() urlMapPattern} attribute.
     * @param urlMapPattern The value for urlMapPattern (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder urlMapPattern(@Nullable String urlMapPattern) {
      this.urlMapPattern = urlMapPattern;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#publishDateVar() publishDateVar} attribute.
     * @param publishDateVar The value for publishDateVar (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder publishDateVar(@Nullable String publishDateVar) {
      this.publishDateVar = publishDateVar;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#expireDateVar() expireDateVar} attribute.
     * @param expireDateVar The value for expireDateVar (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder expireDateVar(@Nullable String expireDateVar) {
      this.expireDateVar = expireDateVar;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#owner() owner} attribute.
     * @param owner The value for owner (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder owner(@Nullable String owner) {
      this.owner = owner;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#modDate() modDate} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link ContentType#modDate() modDate}.</em>
     * @param modDate The value for modDate 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder modDate(Date modDate) {
      this.modDate = Objects.requireNonNull(modDate, "modDate");
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#baseType() baseType} attribute.
     * @param baseType The value for baseType 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder baseType(BaseContentTypes baseType) {
      this.baseType = Objects.requireNonNull(baseType, "baseType");
      initBits &= ~INIT_BIT_BASE_TYPE;
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#host() host} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link ContentType#host() host}.</em>
     * @param host The value for host 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder host(String host) {
      this.host = Objects.requireNonNull(host, "host");
      return this;
    }

    /**
     * Initializes the value for the {@link ContentType#folder() folder} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link ContentType#folder() folder}.</em>
     * @param folder The value for folder 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder folder(String folder) {
      this.folder = Objects.requireNonNull(folder, "folder");
      return this;
    }

    /**
     * Adds one element to {@link ContentType#requiredFields() requiredFields} list.
     * @param element A requiredFields element
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addRequiredFields(Field element) {
      this.requiredFields.add(Objects.requireNonNull(element, "requiredFields element"));
      optBits |= OPT_BIT_REQUIRED_FIELDS;
      return this;
    }

    /**
     * Adds elements to {@link ContentType#requiredFields() requiredFields} list.
     * @param elements An array of requiredFields elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addRequiredFields(Field... elements) {
      for (Field element : elements) {
        this.requiredFields.add(Objects.requireNonNull(element, "requiredFields element"));
      }
      optBits |= OPT_BIT_REQUIRED_FIELDS;
      return this;
    }

    /**
     * Sets or replaces all elements for {@link ContentType#requiredFields() requiredFields} list.
     * @param elements An iterable of requiredFields elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder requiredFields(Iterable<? extends Field> elements) {
      this.requiredFields.clear();
      return addAllRequiredFields(elements);
    }

    /**
     * Adds elements to {@link ContentType#requiredFields() requiredFields} list.
     * @param elements An iterable of requiredFields elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addAllRequiredFields(Iterable<? extends Field> elements) {
      for (Field element : elements) {
        this.requiredFields.add(Objects.requireNonNull(element, "requiredFields element"));
      }
      optBits |= OPT_BIT_REQUIRED_FIELDS;
      return this;
    }

    /**
     * Builds a new {@link ImmutableContentType ImmutableContentType}.
     * @return An immutable instance of ContentType
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutableContentType build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return new ImmutableContentType(this);
    }

    private boolean defaultStructureIsSet() {
      return (optBits & OPT_BIT_DEFAULT_STRUCTURE) != 0;
    }

    private boolean fixedIsSet() {
      return (optBits & OPT_BIT_FIXED) != 0;
    }

    private boolean systemIsSet() {
      return (optBits & OPT_BIT_SYSTEM) != 0;
    }

    private boolean versionableIsSet() {
      return (optBits & OPT_BIT_VERSIONABLE) != 0;
    }

    private boolean multilingualableIsSet() {
      return (optBits & OPT_BIT_MULTILINGUALABLE) != 0;
    }

    private boolean requiredFieldsIsSet() {
      return (optBits & OPT_BIT_REQUIRED_FIELDS) != 0;
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = new ArrayList<String>();
      if ((initBits & INIT_BIT_NAME) != 0) attributes.add("name");
      if ((initBits & INIT_BIT_VELOCITY_VAR_NAME) != 0) attributes.add("velocityVarName");
      if ((initBits & INIT_BIT_BASE_TYPE) != 0) attributes.add("baseType");
      return "Cannot build ContentType, some of required attributes are not set " + attributes;
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
