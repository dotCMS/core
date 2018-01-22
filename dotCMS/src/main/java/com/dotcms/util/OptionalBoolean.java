package com.dotcms.util;


import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Encapsulate an optional boolean. A simplest and bit diff of an {@link java.util.Optional}
 *
 * Example:
 * <code>
 *     return OptionalBoolean.of(evalCondition(request, response))
 *           .ifTrue(()  -> doSomethingOnTrue(request, response))
 *           .orElse(() -> doSomethingOnFalse(request, response))
 *       .get();
 * </code>
 *
 * @author jsancas
 */
public class OptionalBoolean {

    private static final OptionalBoolean EMPTY = new OptionalBoolean(null);
    private final Boolean value;

    private OptionalBoolean(final Boolean value) {
        this.value = value;
    }

    /**
     * @see {@link Optional#get()}
     */
    public Boolean get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    /**
     * @see {@link Optional#isPresent()}
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * @see {@link Optional#ifPresent(Consumer)} ()}
     */
    public void ifPresent(Consumer<Boolean> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    /**
     * @see {@link Optional#filter(Predicate)} ()}
     */
    public OptionalBoolean filter(Predicate<Boolean> predicate) {
        Objects.requireNonNull(predicate);
        return  (!isPresent())?this:
                predicate.test(value) ? this : EMPTY;
    }

    /**
     * If true calls the supplier
     * @param other
     * @return OptionalBoolean to continue the chain
     */
    public OptionalBoolean ifTrue(FunctionUtils.Callback other) {

        if  (value != null && value) {
            other.call();
        }

        return this;
    }

    /**
     * If false call the supplier
     * @param other
     * @return OptionalBoolean to continue the chain
     */
    public OptionalBoolean orElse(FunctionUtils.Callback other) {

        if  (value == null || !value) {
            other.call();
        }

        return this;
    }

    /**
     * If true calls the supplier
     * @param other
     * @return Boolean returns the value
     */
    public Boolean ifTrueGet(FunctionUtils.Callback other) {

        if  (value != null && value) {
            other.call();
        }

        return value;
    }


    /**
     * If false calls the supplier
     * @param other
     * @return Boolean returns the value
     */
    public Boolean orElseGet(FunctionUtils.Callback other) {
        if  (value == null || !value) {
            other.call();
        }

        return value;
    }


    public static  OptionalBoolean of(Boolean value) {
        return new OptionalBoolean(value);
    }
} // E:O:F:OptionalBoolean.
