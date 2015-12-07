package com.dotmarketing.portlets.rules.parameter.comparison;

import com.dotcms.repackage.com.google.common.base.Objects;
import com.dotcms.repackage.org.apache.commons.lang.NotImplementedException;

public class Comparison<T> {

    public static final Comparison<Object> EXISTS = new Exists();
    public static final Comparison<Comparable<Object>> IS = new Is();
    public static final Comparison<Comparable<Object>> IS_NOT = new IsNot();
    public static final Comparison<String> STARTS_WITH = new StartsWith();
    public static final Comparison<String> ENDS_WITH = new EndsWith();
    public static final Comparison<String> CONTAINS = new Contains();
    public static final Comparison<String> REGEX = new RegexComparison();
    public static final Comparison<Comparable<Object>> BETWEEN = new BetweenComparison();
    public static final Comparison<Comparable<Object>> EQUAL = new EqualComparison();
    public static final Comparison<Comparable<Object>> LESS_THAN = new LessThanComparison();
    public static final Comparison<Comparable<Object>> GREATER_THAN = new GreaterThanComparison();
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
