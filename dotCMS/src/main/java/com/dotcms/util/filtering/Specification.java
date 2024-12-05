package com.dotcms.util.filtering;

import java.io.Serializable;

/**
 * This class represents the base Specification interface for filtering objects that meet given
 * satisfaction criteria using the <b>Specification Pattern</b>, which is a behavioral pattern
 * often used to encapsulate filtering logic.
 * <p>By implementing this interface, you can create your own custom Specification class to filter
 * objects based on given criteria.</p>
 *
 * @param <T> The type of the object to be filtered.
 *
 * @author Jose Castro
 * @since Nov 29th, 2024
 */
public interface Specification<T> extends Serializable {

    /**
     * Determines whether the provided object meets the satisfaction criteria for your Specification
     * or not.
     *
     * @param item The object to be evaluated.
     *
     * @return If the object meets your criteria, returns {@code true}.
     */
    boolean isSatisfiedBy(final T item);

    /**
     * Allows you to chain multiple Specifications together using the logical AND operator.
     *
     * @param other The other Specification to be chained.
     *
     * @return If all chained Specifications are satisfied, returns {@code true}.
     */
    default Specification<T> and(final Specification<T> other) {
        return item -> Specification.this.isSatisfiedBy(item) && other.isSatisfiedBy(item);
    }

    /**
     * Allows you to chain multiple Specifications together using the logical OR operator.
     *
     * @param other The other Specification to be chained.
     *
     * @return If any of the chained Specifications are satisfied, returns {@code true}.
     */
    default Specification<T> or(final Specification<T> other) {
        return item -> this.isSatisfiedBy(item) || other.isSatisfiedBy(item);
    }

    /**
     * Allows you to chain multiple Specifications together using the logical NOT operator. This is
     * particularly useful for excluding objects from your result set.
     *
     * @return If the Specification is not satisfied, returns {@code true}.
     */
    default Specification<T> not() {
        return item -> !this.isSatisfiedBy(item);
    }

}
