package com.dotcms.contenttype.model.type;

import com.google.common.base.Preconditions;
import java.io.ObjectStreamException;
import javax.annotation.Generated;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Immutable implementation of {@link DbStorageType}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutableDbStorageType.builder()}.
 * Use the static factory method to get the default singleton instance:
 * {@code ImmutableDbStorageType.of()}.
 */
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@Generated({"Immutables.generator", "DbStorageType"})
@Immutable
public final class ImmutableDbStorageType extends DbStorageType {

  private ImmutableDbStorageType() {}

  /**
   * This instance is equal to all instances of {@code ImmutableDbStorageType} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@Nullable Object another) {
    if (this == another) return true;
    return another instanceof ImmutableDbStorageType
        && equalTo((ImmutableDbStorageType) another);
  }

  private boolean equalTo(ImmutableDbStorageType another) {
    return true;
  }

  /**
   * Returns a constant hash code value.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    return -763899037;
  }

  /**
   * Prints the immutable value {@code DbStorageType}.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return "DbStorageType{}";
  }

  private static final ImmutableDbStorageType INSTANCE = validate(new ImmutableDbStorageType());

  /**
   * Returns the default immutable singleton value of {@code DbStorageType}
   * @return An immutable instance of DbStorageType
   */
  public static ImmutableDbStorageType of() {
    return INSTANCE;
  }

  private static ImmutableDbStorageType validate(ImmutableDbStorageType instance) {
    return INSTANCE != null && INSTANCE.equalTo(instance) ? INSTANCE : instance;
  }

  /**
   * Creates an immutable copy of a {@link DbStorageType} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable DbStorageType instance
   */
  public static ImmutableDbStorageType copyOf(DbStorageType instance) {
    if (instance instanceof ImmutableDbStorageType) {
      return (ImmutableDbStorageType) instance;
    }
    return ImmutableDbStorageType.builder()
        .from(instance)
        .build();
  }

  private static final long serialVersionUID = 1L;

  private Object readResolve() throws ObjectStreamException {
    return validate(this);
  }

  /**
   * Creates a builder for {@link ImmutableDbStorageType ImmutableDbStorageType}.
   * @return A new ImmutableDbStorageType builder
   */
  public static ImmutableDbStorageType.Builder builder() {
    return new ImmutableDbStorageType.Builder();
  }

  /**
   * Builds instances of type {@link ImmutableDbStorageType ImmutableDbStorageType}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  @NotThreadSafe
  public static final class Builder {

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code DbStorageType} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(DbStorageType instance) {
      Preconditions.checkNotNull(instance, "instance");
      return this;
    }

    /**
     * Builds a new {@link ImmutableDbStorageType ImmutableDbStorageType}.
     * @return An immutable instance of DbStorageType
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutableDbStorageType build() {
      return ImmutableDbStorageType.of();
    }
  }
}
