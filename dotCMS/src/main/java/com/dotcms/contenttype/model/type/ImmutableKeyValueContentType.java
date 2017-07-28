package com.dotcms.contenttype.model.type;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.Permissionable;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
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
 * Immutable implementation of {@link KeyValueContentType}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutableKeyValueContentType.builder()}.
 */
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@Generated({"Immutables.generator", "KeyValueContentType"})
@Immutable
public final class ImmutableKeyValueContentType extends KeyValueContentType {
  private final boolean multilingualable;
  private final String name;
  private final @Nullable String id;
  private final @Nullable String description;
  private final boolean defaultType;
  private final @Nullable String detailPage;
  private final boolean fixed;
  private final Date iDate;
  private final boolean system;
  private final boolean versionable;
  private final @Nullable String variable;
  private final @Nullable String urlMapPattern;
  private final @Nullable String publishDateVar;
  private final @Nullable String expireDateVar;
  private final @Nullable String owner;
  private final Date modDate;
  private final String host;
  private final String folder;

  private ImmutableKeyValueContentType(ImmutableKeyValueContentType.Builder builder) {
    this.name = builder.name;
    this.id = builder.id;
    this.description = builder.description;
    this.variable = builder.variable;
    if (builder.multilingualableIsSet()) {
      initShim.multilingualable(builder.multilingualable);
    }
    if (builder.defaultTypeIsSet()) {
      initShim.defaultType(builder.defaultType);
    }
    if (builder.detailPageIsSet()) {
      initShim.detailPage(builder.detailPage);
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
    if (builder.urlMapPatternIsSet()) {
      initShim.urlMapPattern(builder.urlMapPattern);
    }
    if (builder.publishDateVarIsSet()) {
      initShim.publishDateVar(builder.publishDateVar);
    }
    if (builder.expireDateVarIsSet()) {
      initShim.expireDateVar(builder.expireDateVar);
    }
    if (builder.ownerIsSet()) {
      initShim.owner(builder.owner);
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
    this.multilingualable = initShim.multilingualable();
    this.defaultType = initShim.defaultType();
    this.detailPage = initShim.detailPage();
    this.fixed = initShim.fixed();
    this.iDate = initShim.iDate();
    this.system = initShim.system();
    this.versionable = initShim.versionable();
    this.urlMapPattern = initShim.urlMapPattern();
    this.publishDateVar = initShim.publishDateVar();
    this.expireDateVar = initShim.expireDateVar();
    this.owner = initShim.owner();
    this.modDate = initShim.modDate();
    this.host = initShim.host();
    this.folder = initShim.folder();
    this.initShim = null;
  }

  private ImmutableKeyValueContentType(
      boolean multilingualable,
      String name,
      @Nullable String id,
      @Nullable String description,
      boolean defaultType,
      @Nullable String detailPage,
      boolean fixed,
      Date iDate,
      boolean system,
      boolean versionable,
      @Nullable String variable,
      @Nullable String urlMapPattern,
      @Nullable String publishDateVar,
      @Nullable String expireDateVar,
      @Nullable String owner,
      Date modDate,
      String host,
      String folder) {
    this.multilingualable = multilingualable;
    this.name = name;
    this.id = id;
    this.description = description;
    this.defaultType = defaultType;
    this.detailPage = detailPage;
    this.fixed = fixed;
    this.iDate = iDate;
    this.system = system;
    this.versionable = versionable;
    this.variable = variable;
    this.urlMapPattern = urlMapPattern;
    this.publishDateVar = publishDateVar;
    this.expireDateVar = expireDateVar;
    this.owner = owner;
    this.modDate = modDate;
    this.host = host;
    this.folder = folder;
    this.initShim = null;
  }

  private static final int STAGE_INITIALIZING = -1;
  private static final int STAGE_UNINITIALIZED = 0;
  private static final int STAGE_INITIALIZED = 1;
  private transient volatile InitShim initShim = new InitShim();

  private final class InitShim {
    private boolean multilingualable;
    private int multilingualableBuildStage;

    boolean multilingualable() {
      if (multilingualableBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (multilingualableBuildStage == STAGE_UNINITIALIZED) {
        multilingualableBuildStage = STAGE_INITIALIZING;
        this.multilingualable = ImmutableKeyValueContentType.super.multilingualable();
        multilingualableBuildStage = STAGE_INITIALIZED;
      }
      return this.multilingualable;
    }

    void multilingualable(boolean multilingualable) {
      this.multilingualable = multilingualable;
      multilingualableBuildStage = STAGE_INITIALIZED;
    }
    private boolean defaultType;
    private int defaultTypeBuildStage;

    boolean defaultType() {
      if (defaultTypeBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (defaultTypeBuildStage == STAGE_UNINITIALIZED) {
        defaultTypeBuildStage = STAGE_INITIALIZING;
        this.defaultType = ImmutableKeyValueContentType.super.defaultType();
        defaultTypeBuildStage = STAGE_INITIALIZED;
      }
      return this.defaultType;
    }

    void defaultType(boolean defaultType) {
      this.defaultType = defaultType;
      defaultTypeBuildStage = STAGE_INITIALIZED;
    }
    private String detailPage;
    private int detailPageBuildStage;

    String detailPage() {
      if (detailPageBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (detailPageBuildStage == STAGE_UNINITIALIZED) {
        detailPageBuildStage = STAGE_INITIALIZING;
        this.detailPage = ImmutableKeyValueContentType.super.detailPage();
        detailPageBuildStage = STAGE_INITIALIZED;
      }
      return this.detailPage;
    }

    void detailPage(String detailPage) {
      this.detailPage = detailPage;
      detailPageBuildStage = STAGE_INITIALIZED;
    }
    private boolean fixed;
    private int fixedBuildStage;

    boolean fixed() {
      if (fixedBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (fixedBuildStage == STAGE_UNINITIALIZED) {
        fixedBuildStage = STAGE_INITIALIZING;
        this.fixed = ImmutableKeyValueContentType.super.fixed();
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
        this.iDate = Preconditions.checkNotNull(ImmutableKeyValueContentType.super.iDate(), "iDate");
        iDateBuildStage = STAGE_INITIALIZED;
      }
      return this.iDate;
    }

    void iDate(Date iDate) {
      this.iDate = iDate;
      iDateBuildStage = STAGE_INITIALIZED;
    }
    private boolean system;
    private int systemBuildStage;

    boolean system() {
      if (systemBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (systemBuildStage == STAGE_UNINITIALIZED) {
        systemBuildStage = STAGE_INITIALIZING;
        this.system = ImmutableKeyValueContentType.super.system();
        systemBuildStage = STAGE_INITIALIZED;
      }
      return this.system;
    }

    void system(boolean system) {
      this.system = system;
      systemBuildStage = STAGE_INITIALIZED;
    }
    private boolean versionable;
    private int versionableBuildStage;

    boolean versionable() {
      if (versionableBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (versionableBuildStage == STAGE_UNINITIALIZED) {
        versionableBuildStage = STAGE_INITIALIZING;
        this.versionable = ImmutableKeyValueContentType.super.versionable();
        versionableBuildStage = STAGE_INITIALIZED;
      }
      return this.versionable;
    }

    void versionable(boolean versionable) {
      this.versionable = versionable;
      versionableBuildStage = STAGE_INITIALIZED;
    }
    private String urlMapPattern;
    private int urlMapPatternBuildStage;

    String urlMapPattern() {
      if (urlMapPatternBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (urlMapPatternBuildStage == STAGE_UNINITIALIZED) {
        urlMapPatternBuildStage = STAGE_INITIALIZING;
        this.urlMapPattern = ImmutableKeyValueContentType.super.urlMapPattern();
        urlMapPatternBuildStage = STAGE_INITIALIZED;
      }
      return this.urlMapPattern;
    }

    void urlMapPattern(String urlMapPattern) {
      this.urlMapPattern = urlMapPattern;
      urlMapPatternBuildStage = STAGE_INITIALIZED;
    }
    private String publishDateVar;
    private int publishDateVarBuildStage;

    String publishDateVar() {
      if (publishDateVarBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (publishDateVarBuildStage == STAGE_UNINITIALIZED) {
        publishDateVarBuildStage = STAGE_INITIALIZING;
        this.publishDateVar = ImmutableKeyValueContentType.super.publishDateVar();
        publishDateVarBuildStage = STAGE_INITIALIZED;
      }
      return this.publishDateVar;
    }

    void publishDateVar(String publishDateVar) {
      this.publishDateVar = publishDateVar;
      publishDateVarBuildStage = STAGE_INITIALIZED;
    }
    private String expireDateVar;
    private int expireDateVarBuildStage;

    String expireDateVar() {
      if (expireDateVarBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (expireDateVarBuildStage == STAGE_UNINITIALIZED) {
        expireDateVarBuildStage = STAGE_INITIALIZING;
        this.expireDateVar = ImmutableKeyValueContentType.super.expireDateVar();
        expireDateVarBuildStage = STAGE_INITIALIZED;
      }
      return this.expireDateVar;
    }

    void expireDateVar(String expireDateVar) {
      this.expireDateVar = expireDateVar;
      expireDateVarBuildStage = STAGE_INITIALIZED;
    }
    private String owner;
    private int ownerBuildStage;

    String owner() {
      if (ownerBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (ownerBuildStage == STAGE_UNINITIALIZED) {
        ownerBuildStage = STAGE_INITIALIZING;
        this.owner = ImmutableKeyValueContentType.super.owner();
        ownerBuildStage = STAGE_INITIALIZED;
      }
      return this.owner;
    }

    void owner(String owner) {
      this.owner = owner;
      ownerBuildStage = STAGE_INITIALIZED;
    }
    private Date modDate;
    private int modDateBuildStage;

    Date modDate() {
      if (modDateBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (modDateBuildStage == STAGE_UNINITIALIZED) {
        modDateBuildStage = STAGE_INITIALIZING;
        this.modDate = Preconditions.checkNotNull(ImmutableKeyValueContentType.super.modDate(), "modDate");
        modDateBuildStage = STAGE_INITIALIZED;
      }
      return this.modDate;
    }

    void modDate(Date modDate) {
      this.modDate = modDate;
      modDateBuildStage = STAGE_INITIALIZED;
    }
    private String host;
    private int hostBuildStage;

    String host() {
      if (hostBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (hostBuildStage == STAGE_UNINITIALIZED) {
        hostBuildStage = STAGE_INITIALIZING;
        this.host = Preconditions.checkNotNull(ImmutableKeyValueContentType.super.host(), "host");
        hostBuildStage = STAGE_INITIALIZED;
      }
      return this.host;
    }

    void host(String host) {
      this.host = host;
      hostBuildStage = STAGE_INITIALIZED;
    }
    private String folder;
    private int folderBuildStage;

    String folder() {
      if (folderBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
      if (folderBuildStage == STAGE_UNINITIALIZED) {
        folderBuildStage = STAGE_INITIALIZING;
        this.folder = Preconditions.checkNotNull(ImmutableKeyValueContentType.super.folder(), "folder");
        folderBuildStage = STAGE_INITIALIZED;
      }
      return this.folder;
    }

    void folder(String folder) {
      this.folder = folder;
      folderBuildStage = STAGE_INITIALIZED;
    }

    private String formatInitCycleMessage() {
      ArrayList<String> attributes = Lists.newArrayList();
      if (multilingualableBuildStage == STAGE_INITIALIZING) attributes.add("multilingualable");
      if (defaultTypeBuildStage == STAGE_INITIALIZING) attributes.add("defaultType");
      if (detailPageBuildStage == STAGE_INITIALIZING) attributes.add("detailPage");
      if (fixedBuildStage == STAGE_INITIALIZING) attributes.add("fixed");
      if (iDateBuildStage == STAGE_INITIALIZING) attributes.add("iDate");
      if (systemBuildStage == STAGE_INITIALIZING) attributes.add("system");
      if (versionableBuildStage == STAGE_INITIALIZING) attributes.add("versionable");
      if (urlMapPatternBuildStage == STAGE_INITIALIZING) attributes.add("urlMapPattern");
      if (publishDateVarBuildStage == STAGE_INITIALIZING) attributes.add("publishDateVar");
      if (expireDateVarBuildStage == STAGE_INITIALIZING) attributes.add("expireDateVar");
      if (ownerBuildStage == STAGE_INITIALIZING) attributes.add("owner");
      if (modDateBuildStage == STAGE_INITIALIZING) attributes.add("modDate");
      if (hostBuildStage == STAGE_INITIALIZING) attributes.add("host");
      if (folderBuildStage == STAGE_INITIALIZING) attributes.add("folder");
      return "Cannot build KeyValueContentType, attribute initializers form cycle" + attributes;
    }
  }

  /**
   * @return The value of the {@code multilingualable} attribute
   */
  @JsonProperty("multilingualable")
  @Override
  public boolean multilingualable() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.multilingualable()
        : this.multilingualable;
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
   * @return The value of the {@code id} attribute
   */
  @JsonProperty("id")
  @Override
  public @Nullable String id() {
    return id;
  }

  /**
   * @return The value of the {@code description} attribute
   */
  @JsonProperty("description")
  @Override
  public @Nullable String description() {
    return description;
  }

  /**
   * @return The value of the {@code defaultType} attribute
   */
  @JsonProperty("defaultType")
  @Override
  public boolean defaultType() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.defaultType()
        : this.defaultType;
  }

  /**
   * @return The value of the {@code detailPage} attribute
   */
  @JsonProperty("detailPage")
  @Override
  public @Nullable String detailPage() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.detailPage()
        : this.detailPage;
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
   * @return The value of the {@code system} attribute
   */
  @JsonProperty("system")
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
  @JsonProperty("versionable")
  @Override
  public boolean versionable() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.versionable()
        : this.versionable;
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
   * @return The value of the {@code urlMapPattern} attribute
   */
  @JsonProperty("urlMapPattern")
  @Override
  public @Nullable String urlMapPattern() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.urlMapPattern()
        : this.urlMapPattern;
  }

  /**
   * @return The value of the {@code publishDateVar} attribute
   */
  @JsonProperty("publishDateVar")
  @Override
  public @Nullable String publishDateVar() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.publishDateVar()
        : this.publishDateVar;
  }

  /**
   * @return The value of the {@code expireDateVar} attribute
   */
  @JsonProperty("expireDateVar")
  @Override
  public @Nullable String expireDateVar() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.expireDateVar()
        : this.expireDateVar;
  }

  /**
   * @return The value of the {@code owner} attribute
   */
  @JsonProperty("owner")
  @Override
  public @Nullable String owner() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.owner()
        : this.owner;
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
   * @return The value of the {@code host} attribute
   */
  @JsonProperty("host")
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
  @JsonProperty("folder")
  @Override
  public String folder() {
    InitShim shim = this.initShim;
    return shim != null
        ? shim.folder()
        : this.folder;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#multilingualable() multilingualable} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for multilingualable
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withMultilingualable(boolean value) {
    if (this.multilingualable == value) return this;
    return validate(new ImmutableKeyValueContentType(
        value,
        this.name,
        this.id,
        this.description,
        this.defaultType,
        this.detailPage,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#name() name} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for name
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withName(String value) {
    if (this.name.equals(value)) return this;
    String newValue = Preconditions.checkNotNull(value, "name");
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        newValue,
        this.id,
        this.description,
        this.defaultType,
        this.detailPage,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#id() id} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for id (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withId(@Nullable String value) {
    if (Objects.equal(this.id, value)) return this;
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        value,
        this.description,
        this.defaultType,
        this.detailPage,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#description() description} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for description (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withDescription(@Nullable String value) {
    if (Objects.equal(this.description, value)) return this;
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        value,
        this.defaultType,
        this.detailPage,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#defaultType() defaultType} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for defaultType
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withDefaultType(boolean value) {
    if (this.defaultType == value) return this;
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        this.description,
        value,
        this.detailPage,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#detailPage() detailPage} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for detailPage (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withDetailPage(@Nullable String value) {
    if (Objects.equal(this.detailPage, value)) return this;
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        this.description,
        this.defaultType,
        value,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#fixed() fixed} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for fixed
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withFixed(boolean value) {
    if (this.fixed == value) return this;
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        this.description,
        this.defaultType,
        this.detailPage,
        value,
        this.iDate,
        this.system,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#iDate() iDate} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for iDate
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withIDate(Date value) {
    if (this.iDate == value) return this;
    Date newValue = Preconditions.checkNotNull(value, "iDate");
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        this.description,
        this.defaultType,
        this.detailPage,
        this.fixed,
        newValue,
        this.system,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#system() system} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for system
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withSystem(boolean value) {
    if (this.system == value) return this;
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        this.description,
        this.defaultType,
        this.detailPage,
        this.fixed,
        this.iDate,
        value,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#versionable() versionable} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for versionable
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withVersionable(boolean value) {
    if (this.versionable == value) return this;
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        this.description,
        this.defaultType,
        this.detailPage,
        this.fixed,
        this.iDate,
        this.system,
        value,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#variable() variable} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for variable (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withVariable(@Nullable String value) {
    if (Objects.equal(this.variable, value)) return this;
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        this.description,
        this.defaultType,
        this.detailPage,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        value,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#urlMapPattern() urlMapPattern} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for urlMapPattern (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withUrlMapPattern(@Nullable String value) {
    if (Objects.equal(this.urlMapPattern, value)) return this;
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        this.description,
        this.defaultType,
        this.detailPage,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.variable,
        value,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#publishDateVar() publishDateVar} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for publishDateVar (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withPublishDateVar(@Nullable String value) {
    if (Objects.equal(this.publishDateVar, value)) return this;
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        this.description,
        this.defaultType,
        this.detailPage,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        value,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#expireDateVar() expireDateVar} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for expireDateVar (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withExpireDateVar(@Nullable String value) {
    if (Objects.equal(this.expireDateVar, value)) return this;
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        this.description,
        this.defaultType,
        this.detailPage,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        value,
        this.owner,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#owner() owner} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for owner (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withOwner(@Nullable String value) {
    if (Objects.equal(this.owner, value)) return this;
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        this.description,
        this.defaultType,
        this.detailPage,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        value,
        this.modDate,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#modDate() modDate} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for modDate
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withModDate(Date value) {
    if (this.modDate == value) return this;
    Date newValue = Preconditions.checkNotNull(value, "modDate");
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        this.description,
        this.defaultType,
        this.detailPage,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        newValue,
        this.host,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#host() host} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for host
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withHost(String value) {
    if (this.host.equals(value)) return this;
    String newValue = Preconditions.checkNotNull(value, "host");
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        this.description,
        this.defaultType,
        this.detailPage,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        newValue,
        this.folder));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link KeyValueContentType#folder() folder} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for folder
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableKeyValueContentType withFolder(String value) {
    if (this.folder.equals(value)) return this;
    String newValue = Preconditions.checkNotNull(value, "folder");
    return validate(new ImmutableKeyValueContentType(
        this.multilingualable,
        this.name,
        this.id,
        this.description,
        this.defaultType,
        this.detailPage,
        this.fixed,
        this.iDate,
        this.system,
        this.versionable,
        this.variable,
        this.urlMapPattern,
        this.publishDateVar,
        this.expireDateVar,
        this.owner,
        this.modDate,
        this.host,
        newValue));
  }

  /**
   * This instance is equal to all instances of {@code ImmutableKeyValueContentType} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@javax.annotation.Nullable Object another) {
    if (this == another) return true;
    return another instanceof ImmutableKeyValueContentType
        && equalTo((ImmutableKeyValueContentType) another);
  }

  private boolean equalTo(ImmutableKeyValueContentType another) {
    return multilingualable == another.multilingualable
        && name.equals(another.name)
        && Objects.equal(id, another.id)
        && Objects.equal(description, another.description)
        && defaultType == another.defaultType
        && Objects.equal(detailPage, another.detailPage)
        && fixed == another.fixed
        && iDate.equals(another.iDate)
        && system == another.system
        && versionable == another.versionable
        && Objects.equal(variable, another.variable)
        && Objects.equal(urlMapPattern, another.urlMapPattern)
        && Objects.equal(publishDateVar, another.publishDateVar)
        && Objects.equal(expireDateVar, another.expireDateVar)
        && Objects.equal(owner, another.owner)
        && modDate.equals(another.modDate)
        && host.equals(another.host)
        && folder.equals(another.folder);
  }

  /**
   * Computes a hash code from attributes: {@code multilingualable}, {@code name}, {@code id}, {@code description}, {@code defaultType}, {@code detailPage}, {@code fixed}, {@code iDate}, {@code system}, {@code versionable}, {@code variable}, {@code urlMapPattern}, {@code publishDateVar}, {@code expireDateVar}, {@code owner}, {@code modDate}, {@code host}, {@code folder}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 31;
    h = h * 17 + Booleans.hashCode(multilingualable);
    h = h * 17 + name.hashCode();
    h = h * 17 + Objects.hashCode(id);
    h = h * 17 + Objects.hashCode(description);
    h = h * 17 + Booleans.hashCode(defaultType);
    h = h * 17 + Objects.hashCode(detailPage);
    h = h * 17 + Booleans.hashCode(fixed);
    h = h * 17 + iDate.hashCode();
    h = h * 17 + Booleans.hashCode(system);
    h = h * 17 + Booleans.hashCode(versionable);
    h = h * 17 + Objects.hashCode(variable);
    h = h * 17 + Objects.hashCode(urlMapPattern);
    h = h * 17 + Objects.hashCode(publishDateVar);
    h = h * 17 + Objects.hashCode(expireDateVar);
    h = h * 17 + Objects.hashCode(owner);
    h = h * 17 + modDate.hashCode();
    h = h * 17 + host.hashCode();
    h = h * 17 + folder.hashCode();
    return h;
  }

  /**
   * Prints the immutable value {@code KeyValueContentType} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper("KeyValueContentType")
        .omitNullValues()
        .add("multilingualable", multilingualable)
        .add("name", name)
        .add("id", id)
        .add("description", description)
        .add("defaultType", defaultType)
        .add("detailPage", detailPage)
        .add("fixed", fixed)
        .add("iDate", iDate)
        .add("system", system)
        .add("versionable", versionable)
        .add("variable", variable)
        .add("urlMapPattern", urlMapPattern)
        .add("publishDateVar", publishDateVar)
        .add("expireDateVar", expireDateVar)
        .add("owner", owner)
        .add("modDate", modDate)
        .add("host", host)
        .add("folder", folder)
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
  static final class Json extends KeyValueContentType {
    boolean multilingualable;
    boolean multilingualableIsSet;
    @javax.annotation.Nullable String name;
    @javax.annotation.Nullable String id;
    @javax.annotation.Nullable String description;
    boolean defaultType;
    boolean defaultTypeIsSet;
    @javax.annotation.Nullable String detailPage;
    boolean detailPageIsSet;
    boolean fixed;
    boolean fixedIsSet;
    @javax.annotation.Nullable Date iDate;
    boolean system;
    boolean systemIsSet;
    boolean versionable;
    boolean versionableIsSet;
    @javax.annotation.Nullable String variable;
    @javax.annotation.Nullable String urlMapPattern;
    boolean urlMapPatternIsSet;
    @javax.annotation.Nullable String publishDateVar;
    boolean publishDateVarIsSet;
    @javax.annotation.Nullable String expireDateVar;
    boolean expireDateVarIsSet;
    @javax.annotation.Nullable String owner;
    boolean ownerIsSet;
    @javax.annotation.Nullable Date modDate;
    @javax.annotation.Nullable String host;
    @javax.annotation.Nullable String folder;
    @JsonProperty("multilingualable")
    public void setMultilingualable(boolean multilingualable) {
      this.multilingualable = multilingualable;
      this.multilingualableIsSet = true;
    }
    @JsonProperty("name")
    public void setName(String name) {
      this.name = name;
    }
    @JsonProperty("id")
    public void setId(@Nullable String id) {
      this.id = id;
    }
    @JsonProperty("description")
    public void setDescription(@Nullable String description) {
      this.description = description;
    }
    @JsonProperty("defaultType")
    public void setDefaultType(boolean defaultType) {
      this.defaultType = defaultType;
      this.defaultTypeIsSet = true;
    }
    @JsonProperty("detailPage")
    public void setDetailPage(@Nullable String detailPage) {
      this.detailPage = detailPage;
      this.detailPageIsSet = true;
    }
    @JsonProperty("fixed")
    public void setFixed(boolean fixed) {
      this.fixed = fixed;
      this.fixedIsSet = true;
    }
    @JsonProperty("iDate")
    public void setIDate(Date iDate) {
      this.iDate = iDate;
    }
    @JsonProperty("system")
    public void setSystem(boolean system) {
      this.system = system;
      this.systemIsSet = true;
    }
    @JsonProperty("versionable")
    public void setVersionable(boolean versionable) {
      this.versionable = versionable;
      this.versionableIsSet = true;
    }
    @JsonProperty("variable")
    public void setVariable(@Nullable String variable) {
      this.variable = variable;
    }
    @JsonProperty("urlMapPattern")
    public void setUrlMapPattern(@Nullable String urlMapPattern) {
      this.urlMapPattern = urlMapPattern;
      this.urlMapPatternIsSet = true;
    }
    @JsonProperty("publishDateVar")
    public void setPublishDateVar(@Nullable String publishDateVar) {
      this.publishDateVar = publishDateVar;
      this.publishDateVarIsSet = true;
    }
    @JsonProperty("expireDateVar")
    public void setExpireDateVar(@Nullable String expireDateVar) {
      this.expireDateVar = expireDateVar;
      this.expireDateVarIsSet = true;
    }
    @JsonProperty("owner")
    public void setOwner(@Nullable String owner) {
      this.owner = owner;
      this.ownerIsSet = true;
    }
    @JsonProperty("modDate")
    public void setModDate(Date modDate) {
      this.modDate = modDate;
    }
    @JsonProperty("host")
    public void setHost(String host) {
      this.host = host;
    }
    @JsonProperty("folder")
    public void setFolder(String folder) {
      this.folder = folder;
    }
    @Override
    public boolean multilingualable() { throw new UnsupportedOperationException(); }
    @Override
    public String name() { throw new UnsupportedOperationException(); }
    @Override
    public String id() { throw new UnsupportedOperationException(); }
    @Override
    public String description() { throw new UnsupportedOperationException(); }
    @Override
    public boolean defaultType() { throw new UnsupportedOperationException(); }
    @Override
    public String detailPage() { throw new UnsupportedOperationException(); }
    @Override
    public boolean fixed() { throw new UnsupportedOperationException(); }
    @Override
    public Date iDate() { throw new UnsupportedOperationException(); }
    @Override
    public boolean system() { throw new UnsupportedOperationException(); }
    @Override
    public boolean versionable() { throw new UnsupportedOperationException(); }
    @Override
    public String variable() { throw new UnsupportedOperationException(); }
    @Override
    public String urlMapPattern() { throw new UnsupportedOperationException(); }
    @Override
    public String publishDateVar() { throw new UnsupportedOperationException(); }
    @Override
    public String expireDateVar() { throw new UnsupportedOperationException(); }
    @Override
    public String owner() { throw new UnsupportedOperationException(); }
    @Override
    public Date modDate() { throw new UnsupportedOperationException(); }
    @Override
    public String host() { throw new UnsupportedOperationException(); }
    @Override
    public String folder() { throw new UnsupportedOperationException(); }
    @Override
    public String inode() { throw new UnsupportedOperationException(); }
    @Override
    public List<Field> fields() { throw new UnsupportedOperationException(); }
    @Override
    public Map<String, Field> fieldMap() { throw new UnsupportedOperationException(); }
    @Override
    public Permissionable getParentPermissionable() { throw new UnsupportedOperationException(); }
  }

  /**
   * @param json A JSON-bindable data structure
   * @return An immutable value type
   * @deprecated Do not use this method directly, it exists only for the <em>Jackson</em>-binding infrastructure
   */
  @Deprecated
  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  static ImmutableKeyValueContentType fromJson(Json json) {
    ImmutableKeyValueContentType.Builder builder = ImmutableKeyValueContentType.builder();
    if (json.multilingualableIsSet) {
      builder.multilingualable(json.multilingualable);
    }
    if (json.name != null) {
      builder.name(json.name);
    }
    if (json.id != null) {
      builder.id(json.id);
    }
    if (json.description != null) {
      builder.description(json.description);
    }
    if (json.defaultTypeIsSet) {
      builder.defaultType(json.defaultType);
    }
    if (json.detailPageIsSet) {
      builder.detailPage(json.detailPage);
    }
    if (json.fixedIsSet) {
      builder.fixed(json.fixed);
    }
    if (json.iDate != null) {
      builder.iDate(json.iDate);
    }
    if (json.systemIsSet) {
      builder.system(json.system);
    }
    if (json.versionableIsSet) {
      builder.versionable(json.versionable);
    }
    if (json.variable != null) {
      builder.variable(json.variable);
    }
    if (json.urlMapPatternIsSet) {
      builder.urlMapPattern(json.urlMapPattern);
    }
    if (json.publishDateVarIsSet) {
      builder.publishDateVar(json.publishDateVar);
    }
    if (json.expireDateVarIsSet) {
      builder.expireDateVar(json.expireDateVar);
    }
    if (json.ownerIsSet) {
      builder.owner(json.owner);
    }
    if (json.modDate != null) {
      builder.modDate(json.modDate);
    }
    if (json.host != null) {
      builder.host(json.host);
    }
    if (json.folder != null) {
      builder.folder(json.folder);
    }
    return builder.build();
  }

  private transient volatile long lazyInitBitmap;

  private static final long INODE_LAZY_INIT_BIT = 0x1L;

  private transient String inode;

  /**
   * {@inheritDoc}
   * <p>
   * Returns a lazily initialized value of the {@link KeyValueContentType#inode() inode} attribute.
   * Initialized once and only once and stored for subsequent access with proper synchronization.
   * @return A lazily initialized value of the {@code l.name} attribute
   */
  @Override
  public String inode() {
    if ((lazyInitBitmap & INODE_LAZY_INIT_BIT) == 0) {
      synchronized (this) {
        if ((lazyInitBitmap & INODE_LAZY_INIT_BIT) == 0) {
          this.inode = super.inode();
          lazyInitBitmap |= INODE_LAZY_INIT_BIT;
        }
      }
    }
    return inode;
  }

  private static final long FIELDS_LAZY_INIT_BIT = 0x2L;

  private transient List<Field> fields;

  /**
   * {@inheritDoc}
   * <p>
   * Returns a lazily initialized value of the {@link KeyValueContentType#fields() fields} attribute.
   * Initialized once and only once and stored for subsequent access with proper synchronization.
   * @return A lazily initialized value of the {@code l.name} attribute
   */
  @Override
  public List<Field> fields() {
    if ((lazyInitBitmap & FIELDS_LAZY_INIT_BIT) == 0) {
      synchronized (this) {
        if ((lazyInitBitmap & FIELDS_LAZY_INIT_BIT) == 0) {
          this.fields = Preconditions.checkNotNull(super.fields(), "fields");
          lazyInitBitmap |= FIELDS_LAZY_INIT_BIT;
        }
      }
    }
    return fields;
  }

  private static final long FIELD_MAP_LAZY_INIT_BIT = 0x4L;

  private transient Map<String, Field> fieldMap;

  /**
   * {@inheritDoc}
   * <p>
   * Returns a lazily initialized value of the {@link KeyValueContentType#fieldMap() fieldMap} attribute.
   * Initialized once and only once and stored for subsequent access with proper synchronization.
   * @return A lazily initialized value of the {@code l.name} attribute
   */
  @Override
  public Map<String, Field> fieldMap() {
    if ((lazyInitBitmap & FIELD_MAP_LAZY_INIT_BIT) == 0) {
      synchronized (this) {
        if ((lazyInitBitmap & FIELD_MAP_LAZY_INIT_BIT) == 0) {
          this.fieldMap = Preconditions.checkNotNull(super.fieldMap(), "fieldMap");
          lazyInitBitmap |= FIELD_MAP_LAZY_INIT_BIT;
        }
      }
    }
    return fieldMap;
  }

  private static final long PARENT_PERMISSIONABLE_LAZY_INIT_BIT = 0x8L;

  private transient Permissionable parentPermissionable;

  /**
   * {@inheritDoc}
   * <p>
   * Returns a lazily initialized value of the {@link KeyValueContentType#getParentPermissionable() parentPermissionable} attribute.
   * Initialized once and only once and stored for subsequent access with proper synchronization.
   * @return A lazily initialized value of the {@code l.name} attribute
   */
  @Override
  public Permissionable getParentPermissionable() {
    if ((lazyInitBitmap & PARENT_PERMISSIONABLE_LAZY_INIT_BIT) == 0) {
      synchronized (this) {
        if ((lazyInitBitmap & PARENT_PERMISSIONABLE_LAZY_INIT_BIT) == 0) {
          this.parentPermissionable = Preconditions.checkNotNull(super.getParentPermissionable(), "parentPermissionable");
          lazyInitBitmap |= PARENT_PERMISSIONABLE_LAZY_INIT_BIT;
        }
      }
    }
    return parentPermissionable;
  }

  private static ImmutableKeyValueContentType validate(ImmutableKeyValueContentType instance) {
    instance.check();
    return instance;
  }

  /**
   * Creates an immutable copy of a {@link KeyValueContentType} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable KeyValueContentType instance
   */
  public static ImmutableKeyValueContentType copyOf(KeyValueContentType instance) {
    if (instance instanceof ImmutableKeyValueContentType) {
      return (ImmutableKeyValueContentType) instance;
    }
    return ImmutableKeyValueContentType.builder()
        .from(instance)
        .build();
  }

  private static final long serialVersionUID = 1L;

  private Object readResolve() throws ObjectStreamException {
    return validate(this);
  }

  /**
   * Creates a builder for {@link ImmutableKeyValueContentType ImmutableKeyValueContentType}.
   * @return A new ImmutableKeyValueContentType builder
   */
  public static ImmutableKeyValueContentType.Builder builder() {
    return new ImmutableKeyValueContentType.Builder();
  }

  /**
   * Builds instances of type {@link ImmutableKeyValueContentType ImmutableKeyValueContentType}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  @NotThreadSafe
  public static final class Builder 
      extends KeyValueContentType.Builder {
    private static final long INIT_BIT_NAME = 0x1L;
    private static final long OPT_BIT_MULTILINGUALABLE = 0x1L;
    private static final long OPT_BIT_DEFAULT_TYPE = 0x2L;
    private static final long OPT_BIT_DETAIL_PAGE = 0x4L;
    private static final long OPT_BIT_FIXED = 0x8L;
    private static final long OPT_BIT_SYSTEM = 0x10L;
    private static final long OPT_BIT_VERSIONABLE = 0x20L;
    private static final long OPT_BIT_URL_MAP_PATTERN = 0x40L;
    private static final long OPT_BIT_PUBLISH_DATE_VAR = 0x80L;
    private static final long OPT_BIT_EXPIRE_DATE_VAR = 0x100L;
    private static final long OPT_BIT_OWNER = 0x200L;
    private long initBits = 0x1L;
    private long optBits;

    private boolean multilingualable;
    private @javax.annotation.Nullable String name;
    private @javax.annotation.Nullable String id;
    private @javax.annotation.Nullable String description;
    private boolean defaultType;
    private @javax.annotation.Nullable String detailPage;
    private boolean fixed;
    private @javax.annotation.Nullable Date iDate;
    private boolean system;
    private boolean versionable;
    private @javax.annotation.Nullable String variable;
    private @javax.annotation.Nullable String urlMapPattern;
    private @javax.annotation.Nullable String publishDateVar;
    private @javax.annotation.Nullable String expireDateVar;
    private @javax.annotation.Nullable String owner;
    private @javax.annotation.Nullable Date modDate;
    private @javax.annotation.Nullable String host;
    private @javax.annotation.Nullable String folder;

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code com.dotcms.contenttype.model.type.KeyValueContentType} instance.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(KeyValueContentType instance) {
      Preconditions.checkNotNull(instance, "instance");
      from((Object) instance);
      return this;
    }

    /**
     * Fill a builder with attribute values from the provided {@code com.dotcms.contenttype.model.type.ContentTypeIf} instance.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(ContentTypeIf instance) {
      Preconditions.checkNotNull(instance, "instance");
      from((Object) instance);
      return this;
    }

    /**
     * Fill a builder with attribute values from the provided {@code com.dotcms.contenttype.model.type.ContentType} instance.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(ContentType instance) {
      Preconditions.checkNotNull(instance, "instance");
      from((Object) instance);
      return this;
    }

    private void from(Object object) {
      long bits = 0;
      if (object instanceof KeyValueContentType) {
        KeyValueContentType instance = (KeyValueContentType) object;
        if ((bits & 0x1L) == 0) {
          multilingualable(instance.multilingualable());
          bits |= 0x1L;
        }
      }
      if (object instanceof ContentTypeIf) {
        ContentTypeIf instance = (ContentTypeIf) object;
        if ((bits & 0x2L) == 0) {
          String idValue = instance.id();
          if (idValue != null) {
            id(idValue);
          }
          bits |= 0x2L;
        }
      }
      if (object instanceof ContentType) {
        ContentType instance = (ContentType) object;
        String ownerValue = instance.owner();
        if (ownerValue != null) {
          owner(ownerValue);
        }
        if ((bits & 0x1L) == 0) {
          multilingualable(instance.multilingualable());
          bits |= 0x1L;
        }
        modDate(instance.modDate());
        String descriptionValue = instance.description();
        if (descriptionValue != null) {
          description(descriptionValue);
        }
        versionable(instance.versionable());
        String expireDateVarValue = instance.expireDateVar();
        if (expireDateVarValue != null) {
          expireDateVar(expireDateVarValue);
        }
        defaultType(instance.defaultType());
        String urlMapPatternValue = instance.urlMapPattern();
        if (urlMapPatternValue != null) {
          urlMapPattern(urlMapPatternValue);
        }
        system(instance.system());
        folder(instance.folder());
        String publishDateVarValue = instance.publishDateVar();
        if (publishDateVarValue != null) {
          publishDateVar(publishDateVarValue);
        }
        name(instance.name());
        String variableValue = instance.variable();
        if (variableValue != null) {
          variable(variableValue);
        }
        host(instance.host());
        String detailPageValue = instance.detailPage();
        if (detailPageValue != null) {
          detailPage(detailPageValue);
        }
        fixed(instance.fixed());
        if ((bits & 0x2L) == 0) {
          String idValue = instance.id();
          if (idValue != null) {
            id(idValue);
          }
          bits |= 0x2L;
        }
        iDate(instance.iDate());
      }
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#multilingualable() multilingualable} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link KeyValueContentType#multilingualable() multilingualable}.</em>
     * @param multilingualable The value for multilingualable 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder multilingualable(boolean multilingualable) {
      this.multilingualable = multilingualable;
      optBits |= OPT_BIT_MULTILINGUALABLE;
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#name() name} attribute.
     * @param name The value for name 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder name(String name) {
      this.name = Preconditions.checkNotNull(name, "name");
      initBits &= ~INIT_BIT_NAME;
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#id() id} attribute.
     * @param id The value for id (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder id(@Nullable String id) {
      this.id = id;
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#description() description} attribute.
     * @param description The value for description (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder description(@Nullable String description) {
      this.description = description;
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#defaultType() defaultType} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link KeyValueContentType#defaultType() defaultType}.</em>
     * @param defaultType The value for defaultType 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder defaultType(boolean defaultType) {
      this.defaultType = defaultType;
      optBits |= OPT_BIT_DEFAULT_TYPE;
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#detailPage() detailPage} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link KeyValueContentType#detailPage() detailPage}.</em>
     * @param detailPage The value for detailPage (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder detailPage(@Nullable String detailPage) {
      this.detailPage = detailPage;
      optBits |= OPT_BIT_DETAIL_PAGE;
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#fixed() fixed} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link KeyValueContentType#fixed() fixed}.</em>
     * @param fixed The value for fixed 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder fixed(boolean fixed) {
      this.fixed = fixed;
      optBits |= OPT_BIT_FIXED;
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#iDate() iDate} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link KeyValueContentType#iDate() iDate}.</em>
     * @param iDate The value for iDate 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder iDate(Date iDate) {
      this.iDate = Preconditions.checkNotNull(iDate, "iDate");
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#system() system} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link KeyValueContentType#system() system}.</em>
     * @param system The value for system 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder system(boolean system) {
      this.system = system;
      optBits |= OPT_BIT_SYSTEM;
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#versionable() versionable} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link KeyValueContentType#versionable() versionable}.</em>
     * @param versionable The value for versionable 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder versionable(boolean versionable) {
      this.versionable = versionable;
      optBits |= OPT_BIT_VERSIONABLE;
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#variable() variable} attribute.
     * @param variable The value for variable (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder variable(@Nullable String variable) {
      this.variable = variable;
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#urlMapPattern() urlMapPattern} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link KeyValueContentType#urlMapPattern() urlMapPattern}.</em>
     * @param urlMapPattern The value for urlMapPattern (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder urlMapPattern(@Nullable String urlMapPattern) {
      this.urlMapPattern = urlMapPattern;
      optBits |= OPT_BIT_URL_MAP_PATTERN;
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#publishDateVar() publishDateVar} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link KeyValueContentType#publishDateVar() publishDateVar}.</em>
     * @param publishDateVar The value for publishDateVar (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder publishDateVar(@Nullable String publishDateVar) {
      this.publishDateVar = publishDateVar;
      optBits |= OPT_BIT_PUBLISH_DATE_VAR;
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#expireDateVar() expireDateVar} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link KeyValueContentType#expireDateVar() expireDateVar}.</em>
     * @param expireDateVar The value for expireDateVar (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder expireDateVar(@Nullable String expireDateVar) {
      this.expireDateVar = expireDateVar;
      optBits |= OPT_BIT_EXPIRE_DATE_VAR;
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#owner() owner} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link KeyValueContentType#owner() owner}.</em>
     * @param owner The value for owner (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder owner(@Nullable String owner) {
      this.owner = owner;
      optBits |= OPT_BIT_OWNER;
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#modDate() modDate} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link KeyValueContentType#modDate() modDate}.</em>
     * @param modDate The value for modDate 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder modDate(Date modDate) {
      this.modDate = Preconditions.checkNotNull(modDate, "modDate");
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#host() host} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link KeyValueContentType#host() host}.</em>
     * @param host The value for host 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder host(String host) {
      this.host = Preconditions.checkNotNull(host, "host");
      return this;
    }

    /**
     * Initializes the value for the {@link KeyValueContentType#folder() folder} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link KeyValueContentType#folder() folder}.</em>
     * @param folder The value for folder 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder folder(String folder) {
      this.folder = Preconditions.checkNotNull(folder, "folder");
      return this;
    }

    /**
     * Builds a new {@link ImmutableKeyValueContentType ImmutableKeyValueContentType}.
     * @return An immutable instance of KeyValueContentType
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutableKeyValueContentType build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return ImmutableKeyValueContentType.validate(new ImmutableKeyValueContentType(this));
    }

    private boolean multilingualableIsSet() {
      return (optBits & OPT_BIT_MULTILINGUALABLE) != 0;
    }

    private boolean defaultTypeIsSet() {
      return (optBits & OPT_BIT_DEFAULT_TYPE) != 0;
    }

    private boolean detailPageIsSet() {
      return (optBits & OPT_BIT_DETAIL_PAGE) != 0;
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

    private boolean urlMapPatternIsSet() {
      return (optBits & OPT_BIT_URL_MAP_PATTERN) != 0;
    }

    private boolean publishDateVarIsSet() {
      return (optBits & OPT_BIT_PUBLISH_DATE_VAR) != 0;
    }

    private boolean expireDateVarIsSet() {
      return (optBits & OPT_BIT_EXPIRE_DATE_VAR) != 0;
    }

    private boolean ownerIsSet() {
      return (optBits & OPT_BIT_OWNER) != 0;
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = Lists.newArrayList();
      if ((initBits & INIT_BIT_NAME) != 0) attributes.add("name");
      return "Cannot build KeyValueContentType, some of required attributes are not set " + attributes;
    }
  }
}
