package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.repackage.com.google.common.base.Objects;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.org.apache.commons.lang.NotImplementedException;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.portlets.rules.exception.ComparisonDoesNotExistException;
import com.dotmarketing.portlets.rules.exception.ComparisonExistsException;
import java.util.Map;

/**
 * Comparisons are defacto singletons.
 *
 * Comparison and the registry of instances is not implemented in a thread safe fashion, as Comparisions should not be created in such a manner
 * that a race condition is likely.
 * @param <T>
 */
public class Comparison<T> {

    private static final Map<String, Comparison> COMPARISONS = Maps.newHashMap();
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

    protected Comparison(String id) {
        this.id = id;
        if(COMPARISONS.containsKey(id)){
            throw new ComparisonExistsException("The comparison with id '%s' has already been registered.", id);
        }
        COMPARISONS.put(id, this);
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

    public static <T> Comparison<T> register(Comparison<T> value) {
        synchronized (COMPARISONS) {
            if(COMPARISONS.containsKey(value.getId())) {
                throw new ComparisonExistsException("The comparison with id '%s' has already been registered.", value.toString());
            }
            COMPARISONS.put(value.getId(), value);
        }
        return value;
    }

    public static Comparison get(String id) {
        Comparison comparison = COMPARISONS.get(id);
        if(comparison == null) {
            throw new ComparisonDoesNotExistException("No comparison found for id '%s'.", id);
        }
        return comparison;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("id", id)
            .toString();
    }

    public static class Exists extends Comparison<Object> {

        private Exists() {
            super("exists");
        }

        @Override
        public boolean perform(Object arg) {
            return arg != null;
        }
    }

    private static class Is extends Comparison<Comparable<Object>> {

        protected Is() {
            super("is");
        }

        @Override
        public boolean perform(Comparable<Object> left, Comparable<Object> right) {
            return java.util.Objects.equals(left, right);
        }
    }

    private static class IsNot extends Comparison<Comparable<Object>> {

        protected IsNot() {
            super("isNot");
        }

        @Override
        public boolean perform(Comparable<Object> left, Comparable<Object> right) {
            return !java.util.Objects.equals(left, right);
        }
    }

    private static class StartsWith extends Comparison<String> {

        protected StartsWith() {
            super("startsWith");
        }

        @Override
        public boolean perform(String argA, String argB) {
            return super.perform(argA, argB);
        }
    }

    private static class EndsWith extends Comparison<String> {

        protected EndsWith() {
            super("endsWith");
        }

        @Override
        public boolean perform(String argA, String argB) {
            return super.perform(argA, argB);
        }
    }

    private static class Contains extends Comparison<String> {

        protected Contains() {
            super("contains");
        }

        @Override
        public boolean perform(String argA, String argB) {
            return super.perform(argA, argB);
        }
    }

    private static class RegexComparison extends Comparison<String> {

        protected RegexComparison() {
            super("regex");
        }

        @Override
        public boolean perform(String argA, String argB) {
            return super.perform(argA, argB);
        }
    }

    private static class BetweenComparison extends Comparison<Comparable<Object>> {

        protected BetweenComparison() {
            super("between");
        }

        @Override
        public boolean perform(Comparable<Object> argA, Comparable<Object> argB, Comparable<Object> argC) {
            int left = argA.compareTo(argB);
            int right = argB.compareTo(argC);
            int spread = argA.compareTo(argC);
            return left <= right;
        }
    }

    private static class EqualComparison extends Comparison<Comparable<Object>> {

        protected EqualComparison() {
            super("equal");
        }

        @Override
        public boolean perform(Comparable<Object> argA, Comparable<Object> argB) {
            return argA.compareTo(argB) == 0;
        }
    }

    private static class GreaterThanComparison extends Comparison<Comparable<Object>> {

        protected GreaterThanComparison() {
            super("greater_than");
        }

        @Override
        public boolean perform(Comparable<Object> argA, Comparable<Object> argB) {
            return argA.compareTo(argB) > 0;
        }
    }

    private static class LessThanComparison extends Comparison<Comparable<Object>> {

        protected LessThanComparison() {
            super("less_than");
        }

        @Override
        public boolean perform(Comparable<Object> argA, Comparable<Object> argB) {
            return argA.compareTo(argB) < 0;
        }
    }

    private static class GreaterThanOrEqualComparison extends Comparison<Comparable<Object>> {

        protected GreaterThanOrEqualComparison() {
            super("greater_than_or_equal");
        }

        @Override
        public boolean perform(Comparable<Object> argA, Comparable<Object> argB) {
            return argA.compareTo(argB) >= 0;
        }
    }

    private static class LessThanOrEqualComparison extends Comparison<Comparable<Object>> {

        protected LessThanOrEqualComparison() {
            super("less_than_or_equal");
        }

        @Override
        public boolean perform(Comparable<Object> argA, Comparable<Object> argB) {
            return argA.compareTo(argB) <= 0;
        }
    }

    private static class NetmaskComparison extends Comparison<String> {

        protected NetmaskComparison() {
            super("netmask");
        }

        @Override
        public boolean perform(String ipAddress, String netmask) {
            return HttpRequestDataUtil.isIpMatchingNetmask(ipAddress, netmask);
        }
    }
}
