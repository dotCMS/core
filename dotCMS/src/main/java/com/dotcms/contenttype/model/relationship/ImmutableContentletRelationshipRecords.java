package com.dotcms.contenttype.model.relationship;

import com.google.common.base.Preconditions;
import javax.annotation.Generated;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Immutable implementation of {@link ContentletRelationshipRecords}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutableContentletRelationshipRecords.builder()}.
 */
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@Generated({"Immutables.generator", "ContentletRelationshipRecords"})
@Immutable
public final class ImmutableContentletRelationshipRecords
    extends ContentletRelationshipRecords {

  private ImmutableContentletRelationshipRecords(ImmutableContentletRelationshipRecords.Builder builder) {
  }

  /**
   * This instance is equal to all instances of {@code ImmutableContentletRelationshipRecords} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@Nullable Object another) {
    if (this == another) return true;
    return another instanceof ImmutableContentletRelationshipRecords
        && equalTo((ImmutableContentletRelationshipRecords) another);
  }

  private boolean equalTo(ImmutableContentletRelationshipRecords another) {
    return true;
  }

  /**
   * Returns a constant hash code value.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    return -428579726;
  }

  /**
   * Prints the immutable value {@code ContentletRelationshipRecords}.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return "ContentletRelationshipRecords{}";
  }

  /**
   * Creates an immutable copy of a {@link ContentletRelationshipRecords} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable ContentletRelationshipRecords instance
   */
  public static ImmutableContentletRelationshipRecords copyOf(ContentletRelationshipRecords instance) {
    if (instance instanceof ImmutableContentletRelationshipRecords) {
      return (ImmutableContentletRelationshipRecords) instance;
    }
    return ImmutableContentletRelationshipRecords.builder()
        .from(instance)
        .build();
  }

  private static final long serialVersionUID = 1L;

  /**
   * Creates a builder for {@link ImmutableContentletRelationshipRecords ImmutableContentletRelationshipRecords}.
   * @return A new ImmutableContentletRelationshipRecords builder
   */
  public static ImmutableContentletRelationshipRecords.Builder builder() {
    return new ImmutableContentletRelationshipRecords.Builder();
  }

  /**
   * Builds instances of type {@link ImmutableContentletRelationshipRecords ImmutableContentletRelationshipRecords}.
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
     * Fill a builder with attribute values from the provided {@code ContentletRelationshipRecords} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(ContentletRelationshipRecords instance) {
      Preconditions.checkNotNull(instance, "instance");
      return this;
    }

    /**
     * Builds a new {@link ImmutableContentletRelationshipRecords ImmutableContentletRelationshipRecords}.
     * @return An immutable instance of ContentletRelationshipRecords
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutableContentletRelationshipRecords build() {
      return new ImmutableContentletRelationshipRecords(this);
    }
  }
}
