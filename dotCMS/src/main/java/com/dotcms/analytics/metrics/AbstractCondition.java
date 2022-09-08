package com.dotcms.analytics.metrics;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

/**
 * Represents a condition for a {@link Metric}. A Metric can have zero to many Conditions.
 * <p>
 * A condition comprises three parts:
 * <p>
 * <li>The 'parameter' can be an attribute of a page element, or a page URL, referrer, etc.
 * <li>The 'operator' is whatever is chosen as a comparison. See {@link Operator} for the different values
 * <li>The 'value' is the actual value that is desired to compare against. Can be a URL, referrer, id of an element, etc.
 */
@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = Condition.class)
@JsonDeserialize(as = Condition.class)
public interface AbstractCondition {
    String parameter();
    Operator operator();
    String value();

    enum Operator {
        EQUALS,
        CONTAINS
    }

    @Value.Style(typeImmutable="*", typeAbstract="Abstract*")
    @Value.Immutable
    interface AbstractParameter {
        String name();
    }

}
