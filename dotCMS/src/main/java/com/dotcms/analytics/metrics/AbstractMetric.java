package com.dotcms.analytics.metrics;

import com.dotcms.util.DotPreconditions;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.util.List;
import org.immutables.value.Value;


@Value.Style(typeImmutable="*", typeAbstract="Abstract*")
@Value.Immutable
@JsonSerialize(as = Metric.class)
@JsonDeserialize(as = Metric.class)
public interface AbstractMetric extends Serializable {
    String name();

    MetricType type();

    List<Condition> conditions();
}
