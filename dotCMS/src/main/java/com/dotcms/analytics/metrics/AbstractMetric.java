package com.dotcms.analytics.metrics;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.util.List;
import org.immutables.value.Value;

/**
 * A Metric represents something that wants to be measured in the system.
 * <p>
 * It comprises a name, a {@link MetricType} and a List of {@link Condition}s.
 * <p>
 * The {@link Condition}s are filters for the Metric.
 */

@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = Metric.class)
@JsonDeserialize(as = Metric.class)
public interface AbstractMetric extends Serializable {
    String name();

    MetricType type();

    List<Condition> conditions();

}
