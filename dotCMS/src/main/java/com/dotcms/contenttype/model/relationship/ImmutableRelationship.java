package com.dotcms.contenttype.model.relationship;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.Booleans;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Immutable implementation of {@link Relationship}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code ImmutableRelationship.builder()}.
 */
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@Generated({"Immutables.generator", "Relationship"})
@Immutable
public final class ImmutableRelationship extends Relationship {
  private final String parentStructureInode;
  private final String childStructureInode;
  private final String parentRelationName;
  private final String childRelationName;
  private final String relationTypeValue;
  private final String type;
  private final int cardinality;
  private final boolean isParentRequired;
  private final boolean isChildRequired;
  private final boolean isFixed;

  private ImmutableRelationship(ImmutableRelationship.Builder builder) {
    this.parentStructureInode = builder.parentStructureInode;
    this.childStructureInode = builder.childStructureInode;
    this.parentRelationName = builder.parentRelationName;
    this.childRelationName = builder.childRelationName;
    this.relationTypeValue = builder.relationTypeValue;
    this.cardinality = builder.cardinality;
    this.isParentRequired = builder.isParentRequired;
    this.isChildRequired = builder.isChildRequired;
    this.isFixed = builder.isFixed;
    this.type = builder.type != null
        ? builder.type
        : Preconditions.checkNotNull(super.getType(), "type");
  }

  private ImmutableRelationship(
      String parentStructureInode,
      String childStructureInode,
      String parentRelationName,
      String childRelationName,
      String relationTypeValue,
      String type,
      int cardinality,
      boolean isParentRequired,
      boolean isChildRequired,
      boolean isFixed) {
    this.parentStructureInode = parentStructureInode;
    this.childStructureInode = childStructureInode;
    this.parentRelationName = parentRelationName;
    this.childRelationName = childRelationName;
    this.relationTypeValue = relationTypeValue;
    this.type = type;
    this.cardinality = cardinality;
    this.isParentRequired = isParentRequired;
    this.isChildRequired = isChildRequired;
    this.isFixed = isFixed;
  }

  /**
   * @return The value of the {@code parentStructureInode} attribute
   */
  @Override
  public String getParentStructureInode() {
    return parentStructureInode;
  }

  /**
   * @return The value of the {@code childStructureInode} attribute
   */
  @Override
  public String getChildStructureInode() {
    return childStructureInode;
  }

  /**
   * @return The value of the {@code parentRelationName} attribute
   */
  @Override
  public String getParentRelationName() {
    return parentRelationName;
  }

  /**
   * @return The value of the {@code childRelationName} attribute
   */
  @Override
  public String getChildRelationName() {
    return childRelationName;
  }

  /**
   * @return The value of the {@code relationTypeValue} attribute
   */
  @Override
  public String getRelationTypeValue() {
    return relationTypeValue;
  }

  /**
   * @return The value of the {@code type} attribute
   */
  @Override
  String getType() {
    return type;
  }

  /**
   * @return The value of the {@code cardinality} attribute
   */
  @Override
  public int getCardinality() {
    return cardinality;
  }

  /**
   * @return The value of the {@code isParentRequired} attribute
   */
  @Override
  public boolean isParentRequired() {
    return isParentRequired;
  }

  /**
   * @return The value of the {@code isChildRequired} attribute
   */
  @Override
  public boolean isChildRequired() {
    return isChildRequired;
  }

  /**
   * @return The value of the {@code isFixed} attribute
   */
  @Override
  public boolean isFixed() {
    return isFixed;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Relationship#getParentStructureInode() parentStructureInode} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for parentStructureInode
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableRelationship withParentStructureInode(String value) {
    if (this.parentStructureInode.equals(value)) return this;
    String newValue = Preconditions.checkNotNull(value, "parentStructureInode");
    return new ImmutableRelationship(
        newValue,
        this.childStructureInode,
        this.parentRelationName,
        this.childRelationName,
        this.relationTypeValue,
        this.type,
        this.cardinality,
        this.isParentRequired,
        this.isChildRequired,
        this.isFixed);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Relationship#getChildStructureInode() childStructureInode} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for childStructureInode
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableRelationship withChildStructureInode(String value) {
    if (this.childStructureInode.equals(value)) return this;
    String newValue = Preconditions.checkNotNull(value, "childStructureInode");
    return new ImmutableRelationship(
        this.parentStructureInode,
        newValue,
        this.parentRelationName,
        this.childRelationName,
        this.relationTypeValue,
        this.type,
        this.cardinality,
        this.isParentRequired,
        this.isChildRequired,
        this.isFixed);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Relationship#getParentRelationName() parentRelationName} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for parentRelationName
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableRelationship withParentRelationName(String value) {
    if (this.parentRelationName.equals(value)) return this;
    String newValue = Preconditions.checkNotNull(value, "parentRelationName");
    return new ImmutableRelationship(
        this.parentStructureInode,
        this.childStructureInode,
        newValue,
        this.childRelationName,
        this.relationTypeValue,
        this.type,
        this.cardinality,
        this.isParentRequired,
        this.isChildRequired,
        this.isFixed);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Relationship#getChildRelationName() childRelationName} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for childRelationName
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableRelationship withChildRelationName(String value) {
    if (this.childRelationName.equals(value)) return this;
    String newValue = Preconditions.checkNotNull(value, "childRelationName");
    return new ImmutableRelationship(
        this.parentStructureInode,
        this.childStructureInode,
        this.parentRelationName,
        newValue,
        this.relationTypeValue,
        this.type,
        this.cardinality,
        this.isParentRequired,
        this.isChildRequired,
        this.isFixed);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Relationship#getRelationTypeValue() relationTypeValue} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for relationTypeValue
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableRelationship withRelationTypeValue(String value) {
    if (this.relationTypeValue.equals(value)) return this;
    String newValue = Preconditions.checkNotNull(value, "relationTypeValue");
    return new ImmutableRelationship(
        this.parentStructureInode,
        this.childStructureInode,
        this.parentRelationName,
        this.childRelationName,
        newValue,
        this.type,
        this.cardinality,
        this.isParentRequired,
        this.isChildRequired,
        this.isFixed);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Relationship#getType() type} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for type
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableRelationship withType(String value) {
    if (this.type.equals(value)) return this;
    String newValue = Preconditions.checkNotNull(value, "type");
    return new ImmutableRelationship(
        this.parentStructureInode,
        this.childStructureInode,
        this.parentRelationName,
        this.childRelationName,
        this.relationTypeValue,
        newValue,
        this.cardinality,
        this.isParentRequired,
        this.isChildRequired,
        this.isFixed);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Relationship#getCardinality() cardinality} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for cardinality
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableRelationship withCardinality(int value) {
    if (this.cardinality == value) return this;
    return new ImmutableRelationship(
        this.parentStructureInode,
        this.childStructureInode,
        this.parentRelationName,
        this.childRelationName,
        this.relationTypeValue,
        this.type,
        value,
        this.isParentRequired,
        this.isChildRequired,
        this.isFixed);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Relationship#isParentRequired() isParentRequired} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for isParentRequired
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableRelationship withIsParentRequired(boolean value) {
    if (this.isParentRequired == value) return this;
    return new ImmutableRelationship(
        this.parentStructureInode,
        this.childStructureInode,
        this.parentRelationName,
        this.childRelationName,
        this.relationTypeValue,
        this.type,
        this.cardinality,
        value,
        this.isChildRequired,
        this.isFixed);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Relationship#isChildRequired() isChildRequired} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for isChildRequired
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableRelationship withIsChildRequired(boolean value) {
    if (this.isChildRequired == value) return this;
    return new ImmutableRelationship(
        this.parentStructureInode,
        this.childStructureInode,
        this.parentRelationName,
        this.childRelationName,
        this.relationTypeValue,
        this.type,
        this.cardinality,
        this.isParentRequired,
        value,
        this.isFixed);
  }

  /**
   * Copy the current immutable object by setting a value for the {@link Relationship#isFixed() isFixed} attribute.
   * A value equality check is used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for isFixed
   * @return A modified copy of the {@code this} object
   */
  public final ImmutableRelationship withIsFixed(boolean value) {
    if (this.isFixed == value) return this;
    return new ImmutableRelationship(
        this.parentStructureInode,
        this.childStructureInode,
        this.parentRelationName,
        this.childRelationName,
        this.relationTypeValue,
        this.type,
        this.cardinality,
        this.isParentRequired,
        this.isChildRequired,
        value);
  }

  /**
   * This instance is equal to all instances of {@code ImmutableRelationship} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@Nullable Object another) {
    if (this == another) return true;
    return another instanceof ImmutableRelationship
        && equalTo((ImmutableRelationship) another);
  }

  private boolean equalTo(ImmutableRelationship another) {
    return parentStructureInode.equals(another.parentStructureInode)
        && childStructureInode.equals(another.childStructureInode)
        && parentRelationName.equals(another.parentRelationName)
        && childRelationName.equals(another.childRelationName)
        && relationTypeValue.equals(another.relationTypeValue)
        && type.equals(another.type)
        && cardinality == another.cardinality
        && isParentRequired == another.isParentRequired
        && isChildRequired == another.isChildRequired
        && isFixed == another.isFixed;
  }

  /**
   * Computes a hash code from attributes: {@code parentStructureInode}, {@code childStructureInode}, {@code parentRelationName}, {@code childRelationName}, {@code relationTypeValue}, {@code type}, {@code cardinality}, {@code isParentRequired}, {@code isChildRequired}, {@code isFixed}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 31;
    h = h * 17 + parentStructureInode.hashCode();
    h = h * 17 + childStructureInode.hashCode();
    h = h * 17 + parentRelationName.hashCode();
    h = h * 17 + childRelationName.hashCode();
    h = h * 17 + relationTypeValue.hashCode();
    h = h * 17 + type.hashCode();
    h = h * 17 + cardinality;
    h = h * 17 + Booleans.hashCode(isParentRequired);
    h = h * 17 + Booleans.hashCode(isChildRequired);
    h = h * 17 + Booleans.hashCode(isFixed);
    return h;
  }

  /**
   * Prints the immutable value {@code Relationship} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper("Relationship")
        .omitNullValues()
        .add("parentStructureInode", parentStructureInode)
        .add("childStructureInode", childStructureInode)
        .add("parentRelationName", parentRelationName)
        .add("childRelationName", childRelationName)
        .add("relationTypeValue", relationTypeValue)
        .add("type", type)
        .add("cardinality", cardinality)
        .add("isParentRequired", isParentRequired)
        .add("isChildRequired", isChildRequired)
        .add("isFixed", isFixed)
        .toString();
  }

  /**
   * Creates an immutable copy of a {@link Relationship} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable Relationship instance
   */
  public static ImmutableRelationship copyOf(Relationship instance) {
    if (instance instanceof ImmutableRelationship) {
      return (ImmutableRelationship) instance;
    }
    return ImmutableRelationship.builder()
        .from(instance)
        .build();
  }

  private static final long serialVersionUID = 1L;

  /**
   * Creates a builder for {@link ImmutableRelationship ImmutableRelationship}.
   * @return A new ImmutableRelationship builder
   */
  public static ImmutableRelationship.Builder builder() {
    return new ImmutableRelationship.Builder();
  }

  /**
   * Builds instances of type {@link ImmutableRelationship ImmutableRelationship}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  @NotThreadSafe
  public static final class Builder {
    private static final long INIT_BIT_PARENT_STRUCTURE_INODE = 0x1L;
    private static final long INIT_BIT_CHILD_STRUCTURE_INODE = 0x2L;
    private static final long INIT_BIT_PARENT_RELATION_NAME = 0x4L;
    private static final long INIT_BIT_CHILD_RELATION_NAME = 0x8L;
    private static final long INIT_BIT_RELATION_TYPE_VALUE = 0x10L;
    private static final long INIT_BIT_CARDINALITY = 0x20L;
    private static final long INIT_BIT_IS_PARENT_REQUIRED = 0x40L;
    private static final long INIT_BIT_IS_CHILD_REQUIRED = 0x80L;
    private static final long INIT_BIT_IS_FIXED = 0x100L;
    private long initBits = 0x1ffL;

    private @Nullable String parentStructureInode;
    private @Nullable String childStructureInode;
    private @Nullable String parentRelationName;
    private @Nullable String childRelationName;
    private @Nullable String relationTypeValue;
    private @Nullable String type;
    private int cardinality;
    private boolean isParentRequired;
    private boolean isChildRequired;
    private boolean isFixed;

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code Relationship} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(Relationship instance) {
      Preconditions.checkNotNull(instance, "instance");
      parentStructureInode(instance.getParentStructureInode());
      childStructureInode(instance.getChildStructureInode());
      parentRelationName(instance.getParentRelationName());
      childRelationName(instance.getChildRelationName());
      relationTypeValue(instance.getRelationTypeValue());
      type(instance.getType());
      cardinality(instance.getCardinality());
      isParentRequired(instance.isParentRequired());
      isChildRequired(instance.isChildRequired());
      isFixed(instance.isFixed());
      return this;
    }

    /**
     * Initializes the value for the {@link Relationship#getParentStructureInode() parentStructureInode} attribute.
     * @param parentStructureInode The value for parentStructureInode 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder parentStructureInode(String parentStructureInode) {
      this.parentStructureInode = Preconditions.checkNotNull(parentStructureInode, "parentStructureInode");
      initBits &= ~INIT_BIT_PARENT_STRUCTURE_INODE;
      return this;
    }

    /**
     * Initializes the value for the {@link Relationship#getChildStructureInode() childStructureInode} attribute.
     * @param childStructureInode The value for childStructureInode 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder childStructureInode(String childStructureInode) {
      this.childStructureInode = Preconditions.checkNotNull(childStructureInode, "childStructureInode");
      initBits &= ~INIT_BIT_CHILD_STRUCTURE_INODE;
      return this;
    }

    /**
     * Initializes the value for the {@link Relationship#getParentRelationName() parentRelationName} attribute.
     * @param parentRelationName The value for parentRelationName 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder parentRelationName(String parentRelationName) {
      this.parentRelationName = Preconditions.checkNotNull(parentRelationName, "parentRelationName");
      initBits &= ~INIT_BIT_PARENT_RELATION_NAME;
      return this;
    }

    /**
     * Initializes the value for the {@link Relationship#getChildRelationName() childRelationName} attribute.
     * @param childRelationName The value for childRelationName 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder childRelationName(String childRelationName) {
      this.childRelationName = Preconditions.checkNotNull(childRelationName, "childRelationName");
      initBits &= ~INIT_BIT_CHILD_RELATION_NAME;
      return this;
    }

    /**
     * Initializes the value for the {@link Relationship#getRelationTypeValue() relationTypeValue} attribute.
     * @param relationTypeValue The value for relationTypeValue 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder relationTypeValue(String relationTypeValue) {
      this.relationTypeValue = Preconditions.checkNotNull(relationTypeValue, "relationTypeValue");
      initBits &= ~INIT_BIT_RELATION_TYPE_VALUE;
      return this;
    }

    /**
     * Initializes the value for the {@link Relationship#getType() type} attribute.
     * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link Relationship#getType() type}.</em>
     * @param type The value for type 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder type(String type) {
      this.type = Preconditions.checkNotNull(type, "type");
      return this;
    }

    /**
     * Initializes the value for the {@link Relationship#getCardinality() cardinality} attribute.
     * @param cardinality The value for cardinality 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder cardinality(int cardinality) {
      this.cardinality = cardinality;
      initBits &= ~INIT_BIT_CARDINALITY;
      return this;
    }

    /**
     * Initializes the value for the {@link Relationship#isParentRequired() isParentRequired} attribute.
     * @param isParentRequired The value for isParentRequired 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder isParentRequired(boolean isParentRequired) {
      this.isParentRequired = isParentRequired;
      initBits &= ~INIT_BIT_IS_PARENT_REQUIRED;
      return this;
    }

    /**
     * Initializes the value for the {@link Relationship#isChildRequired() isChildRequired} attribute.
     * @param isChildRequired The value for isChildRequired 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder isChildRequired(boolean isChildRequired) {
      this.isChildRequired = isChildRequired;
      initBits &= ~INIT_BIT_IS_CHILD_REQUIRED;
      return this;
    }

    /**
     * Initializes the value for the {@link Relationship#isFixed() isFixed} attribute.
     * @param isFixed The value for isFixed 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder isFixed(boolean isFixed) {
      this.isFixed = isFixed;
      initBits &= ~INIT_BIT_IS_FIXED;
      return this;
    }

    /**
     * Builds a new {@link ImmutableRelationship ImmutableRelationship}.
     * @return An immutable instance of Relationship
     * @throws java.lang.IllegalStateException if any required attributes are missing
     */
    public ImmutableRelationship build() {
      if (initBits != 0) {
        throw new IllegalStateException(formatRequiredAttributesMessage());
      }
      return new ImmutableRelationship(this);
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = Lists.newArrayList();
      if ((initBits & INIT_BIT_PARENT_STRUCTURE_INODE) != 0) attributes.add("parentStructureInode");
      if ((initBits & INIT_BIT_CHILD_STRUCTURE_INODE) != 0) attributes.add("childStructureInode");
      if ((initBits & INIT_BIT_PARENT_RELATION_NAME) != 0) attributes.add("parentRelationName");
      if ((initBits & INIT_BIT_CHILD_RELATION_NAME) != 0) attributes.add("childRelationName");
      if ((initBits & INIT_BIT_RELATION_TYPE_VALUE) != 0) attributes.add("relationTypeValue");
      if ((initBits & INIT_BIT_CARDINALITY) != 0) attributes.add("cardinality");
      if ((initBits & INIT_BIT_IS_PARENT_REQUIRED) != 0) attributes.add("isParentRequired");
      if ((initBits & INIT_BIT_IS_CHILD_REQUIRED) != 0) attributes.add("isChildRequired");
      if ((initBits & INIT_BIT_IS_FIXED) != 0) attributes.add("isFixed");
      return "Cannot build Relationship, some of required attributes are not set " + attributes;
    }
  }
}
