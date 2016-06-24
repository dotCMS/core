package com.dotmarketing.portlets.rules.parameter.comparison;

import com.dotcms.repackage.com.google.common.base.Objects;
import com.dotcms.repackage.org.apache.commons.lang.NotImplementedException;
import com.dotmarketing.portlets.rules.conditionlet.Location;
import static com.dotmarketing.portlets.rules.parameter.display.DropdownInput.Option;

import java.util.Collection;

public class Comparison<T> {

    public static final Comparison<Object> EXISTS = new Exists();
    public static final Comparison<Object> IS = new Is();
    public static final Comparison<Object> IS_NOT = new IsNot();
    public static final Comparison<String> STARTS_WITH = new StartsWith();
    public static final Comparison<String> ENDS_WITH = new EndsWith();
    public static final Comparison<String> CONTAINS = new Contains();
    public static final Comparison<String> REGEX = new RegexComparison();
    public static final Comparison<Comparable> BETWEEN = new BetweenComparison();
    public static final Comparison<Comparable> EQUAL = new EqualComparison();
    public static final Comparison<Comparable> NOT_EQUAL = new NotEqualComparison();
    public static final Comparison<Comparable> LESS_THAN = new LessThanComparison();
    public static final Comparison<Comparable> GREATER_THAN = new GreaterThanComparison();
    public static final Comparison<Comparable<Object>> LESS_THAN_OR_EQUAL = new LessThanOrEqualComparison();
    public static final Comparison<Comparable<Object>> GREATER_THAN_OR_EQUAL = new GreaterThanOrEqualComparison();
    public static final Comparison<Location> WITHIN_DISTANCE = new WithinDistanceComparison();
    public static final Comparison<Location> NOT_WITHIN_DISTANCE = new NotWithinDistanceComparison();
    public static final Comparison<String> NETMASK = new NetmaskComparison();

    public static final Comparison[] NUMERIC_COMPARISONS = {EQUAL, NOT_EQUAL, LESS_THAN, GREATER_THAN, LESS_THAN_OR_EQUAL,
             GREATER_THAN_OR_EQUAL};

    private final String id;

    private final int rightHandArgCount;

    public Comparison(String id) {
        this(id, 1);
    }

    public Comparison(String id, int rightHandArgCount) {
        this.id = id;
        this.rightHandArgCount = rightHandArgCount;
    }

    public final String getId() {
        return id;
    }

    public int getRightHandArgCount() {
        return rightHandArgCount;
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

    public boolean perform(T expect, T argB) {
        throw new NotImplementedException("Comparison '" + getId() + "' cannot be performed with two argument values.");
    }

    public boolean perform(T argA, T argB, T argC) {
        throw new NotImplementedException("Comparison '" + getId() + "' cannot be performed with three argument values.");
    }

    /**
     * @todo ggranum: This modification of the simple, single type isn't ideal, but it's understandable. Find a better pattern
     * for future cases.
     */
    public boolean perform(T argA, T argB, double argC) {
        throw new NotImplementedException("Comparison '" + getId() + "' cannot be performed.");
    }

    public static class ComparisonOption extends Option {

        public final int rightHandArgCount;

        public ComparisonOption(String i18nKey, int rightHandArgCount) {
            super(i18nKey, i18nKey);
            this.rightHandArgCount = rightHandArgCount;
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("id", id)
            .toString();
    }
}
