package com.dotmarketing.portlets.rules.parameter.comparison;

import com.dotcms.repackage.com.google.common.base.Objects;
import com.dotcms.repackage.org.apache.commons.lang.NotImplementedException;

import java.time.LocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.Collection;

public class Comparison<T> {

    public static final Comparison<Object> EXISTS = new Exists();
    public static final Comparison<Comparable> IS = new Is();
    public static final Comparison<Comparable> IS_NOT = new IsNot();
    public static final Comparison<String> STARTS_WITH = new StartsWith();
    public static final Comparison<String> ENDS_WITH = new EndsWith();
    public static final Comparison<String> CONTAINS = new Contains();
    public static final Comparison<String> REGEX = new RegexComparison();
    public static final Comparison<Comparable> BETWEEN = new BetweenComparison();
    public static final Comparison<Comparable> EQUAL = new EqualComparison();
    public static final Comparison<Comparable> LESS_THAN = new LessThanComparison();
    public static final Comparison<Comparable> GREATER_THAN = new GreaterThanComparison();
    public static final Comparison<Comparable<Object>> LESS_THAN_OR_EQUAL = new LessThanOrEqualComparison();
    public static final Comparison<Comparable<Object>> GREATER_THAN_OR_EQUAL = new GreaterThanOrEqualComparison();
    public static final Comparison<String> NETMASK = new NetmaskComparison();

    private final String id;

    public Comparison(String id) {
        this.id = id;

    }

    public final String getId() {
        return id;
    }

    /**
     * @todo ggranum: This makes more sense than using the multi-arg versions, even though it's not as easy to call and isn't as safe.
     * Another option might be to add interfaces: 'TwoArgComparison', 'OneArgComparison' etc. Look more closely at Matchers for inspiration?
     * Using interfaces would help on the client side as well.
     */
    public boolean perform(Collection<T> arguments) {
        throw new NotImplementedException("Comparison '" + getId() + "' cannot be performed with a list of arguments.");
    }

    public boolean perform(T arg) {
        throw new NotImplementedException("Comparison '" + getId() + "' cannot be performed with one argument value.");
    }

    public boolean perform(T argA, T argB) {
        throw new NotImplementedException("Comparison '" + getId() + "' cannot be performed with two argument values.");
    }

    public boolean perform(T argA, T argB, T argC) {
        throw new NotImplementedException("Comparison '" + getId() + "' cannot be performed with three argument values.");
    }




    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("id", id)
            .toString();
    }
}
