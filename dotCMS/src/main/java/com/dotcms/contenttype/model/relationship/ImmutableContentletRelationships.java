package com.dotcms.contenttype.model.relationship;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Immutable implementation of {@link ContentletRelationships}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutableContentletRelationships.builder()}.
 */
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@Generated({"Immutables.generator", "ContentletRelationships"})
@Immutable
public final class ImmutableContentletRelationships
    extends ContentletRelationships {
  private final Contentlet contentlet;
  private final ImmutableList<ContentletRelationshipRecords> relationshipsRecords;

  private ImmutableContentletRelationships(
      Contentlet contentlet,
      ImmutableList<ContentletRelationshipRecords> relationshipsRecords) {
    this.contentlet = contentlet;
    this.relationshipsRecords = relationshipsRecords;
  }

  /**
   * @return The value of the {@code contentlet} attribute
   */
  @Override
  public Contentlet getContentlet() {
    return contentlet;
  }

  /**
   * @return The value of the {@code relationshipsRecords} attribute
   */
  @Override
  public ImmutableList<ContentletRelationshipRecords> getRelationshipsRecords() {
    return relationshipsRecords;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link ContentletRelationships#getContentlet() contentlet} attribute.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for contentlet
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableContentletRelationships withContentlet(Contentlet value) {
    if (this.contentlet == value) return this;
    Contentlet newValue = Preconditions.checkNotNull(value, "contentlet");
    return new ImmutableContentletRelationships(newValue, this.relationshipsRecords);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link ContentletRelationships#getRelationshipsRecords() relationshipsRecords}.
   * @param elements The elements to set
   * @return A modified copy of {@code this} object
   */
  public final ImmutableContentletRelationships withRelationshipsRecords(ContentletRelationshipRecords... elements) {
    ImmutableList<ContentletRelationshipRecords> newValue = ImmutableList.copyOf(elements);
    return new ImmutableContentletRelationships(this.contentlet, newValue);
  }

  /**
   * Copy the current immutable object with elements that replace the content of {@link ContentletRelationships#getRelationshipsRecords() relationshipsRecords}.
   * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
   * @param elements An iterable of relationshipsRecords elements to set
   * @return A modified copy of {@code this} object
   */
  public final ImmutableContentletRelationships withRelationshipsRecords(Iterable<? extends ContentletRelationshipRecords> elements) {
    if (this.relationshipsRecords == elements) return this;
    ImmutableList<ContentletRelationshipRecords> newValue = ImmutableList.copyOf(elements);
    return new ImmutableContentletRelationships(this.contentlet, newValue);
  }

  /**
   * This instance is equal to all instances of {@code ImmutableContentletRelationships} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@Nullable Object another) {
    if (this == another) return true;
    return another instanceof ImmutableContentletRelationships
        && equalTo((ImmutableContentletRelationships) another);
  }

  private boolean equalTo(ImmutableContentletRelationships another) {
    return contentlet.equals(another.contentlet)
        && relationshipsRecords.equals(another.relationshipsRecords);
  }

  /**
   * Computes a hash code from attributes: {@code contentlet}, {@code relationshipsRecords}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 31;
    h = h * 17 + contentlet.hashCode();
    h = h * 17 + relationshipsRecords.hashCode();
    return h;
  }

  /**
   * Prints the immutable value {@code ContentletRelationships} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper("ContentletRelationships")
        .omitNullValues()
        .add("contentlet", contentlet)
        .add("relationshipsRecords", relationshipsRecords)
        .toString();
  }

  /**
   * Creates an immutable copy of a {@link ContentletRelationships} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable ContentletRelationships instance
   */
  public static ImmutableContentletRelationships copyOf(ContentletRelationships instance) {
    if (instance instanceof ImmutableContentletRelationships) {
      return (ImmutableContentletRelationships) instance;
    }
    return ImmutableContentletRelationships.builder()
        .from(instance)
        .build();
  }

  private static final long serialVersionUID = 1L;

  /**
   * Creates a builder for {@link ImmutableContentletRelationships ImmutableContentletRelationships}.
   * @return A new ImmutableContentletRelationships builder
   */
  public static ImmutableContentletRelationships.Builder builder() {
    return new ImmutableContentletRelationships.Builder();
  }

  /**
   * Builds instances of type {@link ImmutableContentletRelationships ImmutableContentletRelationships}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  @NotThreadSafe
  public static final class Builder {
    private static final long INIT_BIT_CONTENTLET = 0x1L;
    private long initBits = 0x1L;

    private @Nullable Contentlet contentlet;
    private ImmutableList.Builder<ContentletRelationshipRecords> relationshipsRecords = ImmutableList.builder();

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code ContentletRelationships} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * Collection elements and entries will be added, not replaced.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(ContentletRelationships instance) {
      Preconditions.checkNotNull(instance, "instance");
      contentlet(instance.getContentlet());
      addAllRelationshipsRecords(instance.getRelationshipsRecords());
      return this;
    }

    /**
     * Initializes the value for the {@link ContentletRelationships#getContentlet() contentlet} attribute.
     * @param contentlet The value for contentlet 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder contentlet(Contentlet contentlet) {
      this.contentlet = Preconditions.checkNotNull(contentlet, "contentlet");
      initBits &= ~INIT_BIT_CONTENTLET;
      return this;
    }

    /**
     * Adds one element to {@link ContentletRelationships#getRelationshipsRecords() relationshipsRecords} list.
     * @param element A relationshipsRecords element
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addRelationshipsRecords(ContentletRelationshipRecords element) {
      this.relationshipsRecords.add(element);
      return this;
    }

    /**
     * Adds elements to {@link ContentletRelationships#getRelationshipsRecords() relationshipsRecords} list.
     * @param elements An array of relationshipsRecords elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addRelationshipsRecords(ContentletRelationshipRecords... elements) {
      this.relationshipsRecords.add(elements);
      return this;
    }

    /**
     * Sets or replaces all elements for {@link ContentletRelationships#getRelationshipsRecords() relationshipsRecords} list.
     * @param elements An iterable of relationshipsRecords elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder relationshipsRecords(Iterable<? extends ContentletRelationshipRecords> elements) {
      this.relationshipsRecords = ImmutableList.builder();
      return addAllRelationshipsRecords(elements);
    }

    /**
     * Adds elements to {@link ContentletRelationships#getRelationshipsRecords() relationshipsRecords} list.
     * @param elements An iterable of relationshipsRecords elements
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder addAllRelationshipsRecords(Iterable<? extends ContentletRelationshipRecords> elements) {
      this.relationshipsRecords.addAll(elements);
      return this;
    }

    /**
     * Builds a new {@link ImmutableContentletRelationships ImmutableContentletRelationships}.
     * @return An immutable instance of ContentletRelationships
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutableContentletRelationships build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return new ImmutableContentletRelationships(contentlet, relationshipsRecords.build());
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = Lists.newArrayList();
      if ((initBits & INIT_BIT_CONTENTLET) != 0) attributes.add("contentlet");
      return "Cannot build ContentletRelationships, some of required attributes are not set " + attributes;
    }
  }
}
