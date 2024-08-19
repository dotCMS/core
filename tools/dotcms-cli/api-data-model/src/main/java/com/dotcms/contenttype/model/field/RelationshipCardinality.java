package com.dotcms.contenttype.model.field;

/**
 * The RelationshipCardinality enum represents the cardinality of a relationship between entities.
 * The enum constants are:
 * <p>
 * <ul>
 * <li> ONE_TO_MANY: Indicates a one-to-many relationship.</li>
 * <li> MANY_TO_MANY: Indicates a many-to-many relationship.</li>
 * <li> ONE_TO_ONE: Indicates a one-to-one relationship.</li>
 * <li> MANY_TO_ONE: Indicates a many-to-one relationship.</li>
 * </ul>
 * <p>
 * The RelationshipCardinality class provides static methods for retrieving enum constants based on
 * their ordinal or name, and for retrieving the ordinal value or name of an enum constant.
 * <p>
 * Usage example:
 * <p>
 * <pre>{@code
 * RelationshipCardinality cardinality = RelationshipCardinality.ONE_TO_MANY;
 * String name = RelationshipCardinality.getNameByOrdinal(cardinality.ordinal());
 * int ordinal = RelationshipCardinality.getOrdinalByName(name);
 * System.out.println(name); // Output: "ONE_TO_MANY"
 * System.out.println(ordinal); // Output: 0
 * }</pre>
 *
 * @throws IllegalArgumentException if the ordinal, name or enum constant is invalid or not found
 */
public enum RelationshipCardinality {

    ONE_TO_MANY,
    MANY_TO_MANY,
    ONE_TO_ONE,
    MANY_TO_ONE;

    /**
     * Returns the name of an enum constant in the RelationshipCardinality enum, based on its
     * ordinal.
     *
     * @param ordinal the ordinal of the enum constant
     * @return the name of the enum constant
     * @throws IllegalArgumentException if the given ordinal is invalid
     */
    public static String getNameByOrdinal(int ordinal) {
        for (RelationshipCardinality rc : values()) {
            if (rc.ordinal() == ordinal) {
                return rc.name();
            }
        }
        throw new IllegalArgumentException("Invalid ordinal: " + ordinal);
    }

    /**
     * Returns the RelationshipCardinality enum constant based on the given name.
     *
     * @param name the name of the enum constant
     * @return the enum constant with the given name
     * @throws IllegalArgumentException if no enum constant with the given name is found
     */
    public static RelationshipCardinality fromName(String name) {
        for (RelationshipCardinality rc : values()) {
            if (rc.name().equals(name)) {
                return rc;
            }
        }
        throw new IllegalArgumentException("Invalid name: " + name);
    }

    /**
     * Returns the RelationshipCardinality enum constant based on its ordinal value.
     *
     * @param ordinal the ordinal value of the enum constant
     * @return the enum constant with the given ordinal
     * @throws IllegalArgumentException if no enum constant with the given ordinal is found
     */
    public static RelationshipCardinality fromOrdinal(int ordinal) {
        for (RelationshipCardinality rc : values()) {
            if (rc.ordinal() == ordinal) {
                return rc;
            }
        }
        throw new IllegalArgumentException("Invalid ordinal: " + ordinal);
    }

    /**
     * Returns the ordinal value of an enum constant in the RelationshipCardinality enum, based on
     * its name.
     *
     * @param name the name of the enum constant
     * @return the ordinal value of the enum constant
     * @throws IllegalArgumentException if no enum constant with the given name is found
     */
    public static int getOrdinalByName(String name) {
        return fromName(name).ordinal();
    }

}
