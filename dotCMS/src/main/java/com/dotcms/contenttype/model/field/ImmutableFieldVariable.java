package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.io.ObjectStreamException;
import java.util.Date;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import org.elasticsearch.common.Nullable;

/**
 * Immutable implementation of {@link FieldVariable}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutableFieldVariable.builder()}.
 */
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@Generated({"Immutables.generator", "FieldVariable"})
@Immutable
public final class ImmutableFieldVariable implements FieldVariable {
  private final @Nullable String id;
  private final @Nullable String fieldId;
  private final @Nullable String name;
  private final String key;
  private final String value;
  private final @Nullable String userId;
  private final Date modDate;

  private ImmutableFieldVariable(ImmutableFieldVariable.Builder builder) {
    this.id = builder.id;
    this.fieldId = builder.fieldId;
    this.name = builder.name;
    this.key = builder.key;
    this.value = builder.value;
    this.userId = builder.userId;
    this.modDate = builder.modDate != null
        ? builder.modDate
        : Preconditions.checkNotNull(FieldVariable.super.modDate(), "modDate");
  }

  private ImmutableFieldVariable(
      @Nullable String id,
      @Nullable String fieldId,
      @Nullable String name,
      String key,
      String value,
      @Nullable String userId,
      Date modDate) {
    this.id = id;
    this.fieldId = fieldId;
    this.name = name;
    this.key = key;
    this.value = value;
    this.userId = userId;
    this.modDate = modDate;
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
   * @return The value of the {@code fieldId} attribute
   */
  @JsonProperty("fieldId")
  @Override
  public @Nullable String fieldId() {
    return fieldId;
  }

  /**
   * @return The value of the {@code name} attribute
   */
  @JsonProperty("name")
  @Override
  public @Nullable String name() {
    return name;
  }

  /**
   * @return The value of the {@code key} attribute
   */
  @JsonProperty("key")
  @Override
  public String key() {
    return key;
  }

  /**
   * @return The value of the {@code value} attribute
   */
  @JsonProperty("value")
  @Override
  public String value() {
    return value;
  }

  /**
   * @return The value of the {@code userId} attribute
   */
  @JsonProperty("userId")
  @Override
  public @Nullable String userId() {
    return userId;
  }

  /**
   * @return The value of the {@code modDate} attribute
   */
  @JsonProperty("modDate")
  @Override
  public Date modDate() {
    return modDate;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link FieldVariable#id() id} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for id (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableFieldVariable withId(@Nullable String value) {
    if (Objects.equal(this.id, value)) return this;
    return validate(new ImmutableFieldVariable(value, this.fieldId, this.name, this.key, this.value, this.userId, this.modDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link FieldVariable#fieldId() fieldId} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for fieldId (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableFieldVariable withFieldId(@Nullable String value) {
    if (Objects.equal(this.fieldId, value)) return this;
    return validate(new ImmutableFieldVariable(this.id, value, this.name, this.key, this.value, this.userId, this.modDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link FieldVariable#name() name} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for name (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableFieldVariable withName(@Nullable String value) {
    if (Objects.equal(this.name, value)) return this;
    return validate(new ImmutableFieldVariable(this.id, this.fieldId, value, this.key, this.value, this.userId, this.modDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link FieldVariable#key() key} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for key
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableFieldVariable withKey(String value) {
    if (this.key.equals(value)) return this;
    String newValue = Preconditions.checkNotNull(value, "key");
    return validate(new ImmutableFieldVariable(this.id, this.fieldId, this.name, newValue, this.value, this.userId, this.modDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link FieldVariable#value() value} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for value
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableFieldVariable withValue(String value) {
    if (this.value.equals(value)) return this;
    String newValue = Preconditions.checkNotNull(value, "value");
    return validate(new ImmutableFieldVariable(this.id, this.fieldId, this.name, this.key, newValue, this.userId, this.modDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link FieldVariable#userId() userId} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for userId (can be {@code null})
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableFieldVariable withUserId(@Nullable String value) {
    if (Objects.equal(this.userId, value)) return this;
    return validate(new ImmutableFieldVariable(this.id, this.fieldId, this.name, this.key, this.value, value, this.modDate));
  }

  /**
   * Copy the current immutable object by setting a value for the {@link FieldVariable#modDate() modDate} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for modDate
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableFieldVariable withModDate(Date value) {
    if (this.modDate == value) return this;
    Date newValue = Preconditions.checkNotNull(value, "modDate");
    return validate(new ImmutableFieldVariable(this.id, this.fieldId, this.name, this.key, this.value, this.userId, newValue));
  }

  /**
   * This instance is equal to all instances of {@code ImmutableFieldVariable} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@javax.annotation.Nullable Object another) {
    if (this == another) return true;
    return another instanceof ImmutableFieldVariable
        && equalTo((ImmutableFieldVariable) another);
  }

  private boolean equalTo(ImmutableFieldVariable another) {
    return Objects.equal(id, another.id)
        && Objects.equal(fieldId, another.fieldId)
        && Objects.equal(name, another.name)
        && key.equals(another.key)
        && value.equals(another.value)
        && Objects.equal(userId, another.userId)
        && modDate.equals(another.modDate);
  }

  /**
   * Computes a hash code from attributes: {@code id}, {@code fieldId}, {@code name}, {@code key}, {@code value}, {@code userId}, {@code modDate}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 31;
    h = h * 17 + Objects.hashCode(id);
    h = h * 17 + Objects.hashCode(fieldId);
    h = h * 17 + Objects.hashCode(name);
    h = h * 17 + key.hashCode();
    h = h * 17 + value.hashCode();
    h = h * 17 + Objects.hashCode(userId);
    h = h * 17 + modDate.hashCode();
    return h;
  }

  /**
   * Prints the immutable value {@code FieldVariable} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper("FieldVariable")
        .omitNullValues()
        .add("id", id)
        .add("fieldId", fieldId)
        .add("name", name)
        .add("key", key)
        .add("value", value)
        .add("userId", userId)
        .add("modDate", modDate)
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
  static final class Json implements FieldVariable {
    @javax.annotation.Nullable String id;
    @javax.annotation.Nullable String fieldId;
    @javax.annotation.Nullable String name;
    @javax.annotation.Nullable String key;
    @javax.annotation.Nullable String value;
    @javax.annotation.Nullable String userId;
    @javax.annotation.Nullable Date modDate;
    @JsonProperty("id")
    public void setId(@Nullable String id) {
      this.id = id;
    }
    @JsonProperty("fieldId")
    public void setFieldId(@Nullable String fieldId) {
      this.fieldId = fieldId;
    }
    @JsonProperty("name")
    public void setName(@Nullable String name) {
      this.name = name;
    }
    @JsonProperty("key")
    public void setKey(String key) {
      this.key = key;
    }
    @JsonProperty("value")
    public void setValue(String value) {
      this.value = value;
    }
    @JsonProperty("userId")
    public void setUserId(@Nullable String userId) {
      this.userId = userId;
    }
    @JsonProperty("modDate")
    public void setModDate(Date modDate) {
      this.modDate = modDate;
    }
    @Override
    public String id() { throw new UnsupportedOperationException(); }
    @Override
    public String fieldId() { throw new UnsupportedOperationException(); }
    @Override
    public String name() { throw new UnsupportedOperationException(); }
    @Override
    public String key() { throw new UnsupportedOperationException(); }
    @Override
    public String value() { throw new UnsupportedOperationException(); }
    @Override
    public String userId() { throw new UnsupportedOperationException(); }
    @Override
    public Date modDate() { throw new UnsupportedOperationException(); }
  }

  /**
   * @param json A JSON-bindable data structure
   * @return An immutable value type
   * @deprecated Do not use this method directly, it exists only for the <em>Jackson</em>-binding infrastructure
   */
  @Deprecated
  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  static ImmutableFieldVariable fromJson(Json json) {
    ImmutableFieldVariable.Builder builder = ImmutableFieldVariable.builder();
    if (json.id != null) {
      builder.id(json.id);
    }
    if (json.fieldId != null) {
      builder.fieldId(json.fieldId);
    }
    if (json.name != null) {
      builder.name(json.name);
    }
    if (json.key != null) {
      builder.key(json.key);
    }
    if (json.value != null) {
      builder.value(json.value);
    }
    if (json.userId != null) {
      builder.userId(json.userId);
    }
    if (json.modDate != null) {
      builder.modDate(json.modDate);
    }
    return builder.build();
  }

  private static ImmutableFieldVariable validate(ImmutableFieldVariable instance) {
    instance.check();
    return instance;
  }

  /**
   * Creates an immutable copy of a {@link FieldVariable} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable FieldVariable instance
   */
  public static ImmutableFieldVariable copyOf(FieldVariable instance) {
    if (instance instanceof ImmutableFieldVariable) {
      return (ImmutableFieldVariable) instance;
    }
    return ImmutableFieldVariable.builder()
        .from(instance)
        .build();
  }

  private Object readResolve() throws ObjectStreamException {
    return validate(this);
  }

  /**
   * Creates a builder for {@link ImmutableFieldVariable ImmutableFieldVariable}.
   * @return A new ImmutableFieldVariable builder
   */
  public static ImmutableFieldVariable.Builder builder() {
    return new ImmutableFieldVariable.Builder();
  }

  /**
   * Builds instances of type {@link ImmutableFieldVariable ImmutableFieldVariable}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  @NotThreadSafe
  public static final class Builder {
    private static final long INIT_BIT_KEY = 0x1L;
    private static final long INIT_BIT_VALUE = 0x2L;
    private long initBits = 0x3L;

    private @javax.annotation.Nullable String id;
    private @javax.annotation.Nullable String fieldId;
    private @javax.annotation.Nullable String name;
    private @javax.annotation.Nullable String key;
    private @javax.annotation.Nullable String value;
    private @javax.annotation.Nullable String userId;
    private @javax.annotation.Nullable Date modDate;

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code FieldVariable} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(FieldVariable instance) {
      Preconditions.checkNotNull(instance, "instance");
      String idValue = instance.id();
      if (idValue != null) {
        id(idValue);
      }
      String fieldIdValue = instance.fieldId();
      if (fieldIdValue != null) {
        fieldId(fieldIdValue);
      }
      String nameValue = instance.name();
      if (nameValue != null) {
        name(nameValue);
      }
      key(instance.key());
      value(instance.value());
      String userIdValue = instance.userId();
      if (userIdValue != null) {
        userId(userIdValue);
      }
      modDate(instance.modDate());
      return this;
    }

    /**
     * Initializes the value for the {@link FieldVariable#id() id} attribute.
     * @param id The value for id (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder id(@Nullable String id) {
      this.id = id;
      return this;
    }

    /**
     * Initializes the value for the {@link FieldVariable#fieldId() fieldId} attribute.
     * @param fieldId The value for fieldId (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder fieldId(@Nullable String fieldId) {
      this.fieldId = fieldId;
      return this;
    }

    /**
     * Initializes the value for the {@link FieldVariable#name() name} attribute.
     * @param name The value for name (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder name(@Nullable String name) {
      this.name = name;
      return this;
    }

    /**
     * Initializes the value for the {@link FieldVariable#key() key} attribute.
     * @param key The value for key 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder key(String key) {
      this.key = Preconditions.checkNotNull(key, "key");
      initBits &= ~INIT_BIT_KEY;
      return this;
    }

    /**
     * Initializes the value for the {@link FieldVariable#value() value} attribute.
     * @param value The value for value 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder value(String value) {
      this.value = Preconditions.checkNotNull(value, "value");
      initBits &= ~INIT_BIT_VALUE;
      return this;
    }

    /**
     * Initializes the value for the {@link FieldVariable#userId() userId} attribute.
     * @param userId The value for userId (can be {@code null})
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder userId(@Nullable String userId) {
      this.userId = userId;
      return this;
    }

    /**
     * Initializes the value for the {@link FieldVariable#modDate() modDate} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link FieldVariable#modDate() modDate}.</em>
     * @param modDate The value for modDate 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder modDate(Date modDate) {
      this.modDate = Preconditions.checkNotNull(modDate, "modDate");
      return this;
    }

    /**
     * Builds a new {@link ImmutableFieldVariable ImmutableFieldVariable}.
     * @return An immutable instance of FieldVariable
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutableFieldVariable build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return ImmutableFieldVariable.validate(new ImmutableFieldVariable(this));
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = Lists.newArrayList();
      if ((initBits & INIT_BIT_KEY) != 0) attributes.add("key");
      if ((initBits & INIT_BIT_VALUE) != 0) attributes.add("value");
      return "Cannot build FieldVariable, some of required attributes are not set " + attributes;
    }
  }
}
