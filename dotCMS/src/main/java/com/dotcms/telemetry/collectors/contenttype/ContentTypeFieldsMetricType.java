package com.dotcms.telemetry.collectors.contenttype;

import com.dotcms.telemetry.MetricCategory;
import com.dotcms.telemetry.MetricFeature;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricValue;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotRuntimeException;

import javax.ws.rs.NotSupportedException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Collects the total number of contentlets
 */
public abstract class ContentTypeFieldsMetricType implements MetricType {

    @Override
    public MetricCategory getCategory() {
        return MetricCategory.DIFFERENTIATING_FEATURES;
    }

    @Override
    public MetricFeature getFeature() {
        return MetricFeature.CONTENT_TYPE_FIELDS;
    }

    @Override
    public Optional<MetricValue> getStat() {
        try {
            return LocalTransaction.wrapReturn(() -> {
                final String sqlQuery = "SELECT field_type, count(*)\n" +
                        "FROM field GROUP BY field_type";
                final DotConnect dotConnect = new DotConnect();
                final List<Map<String, Object>> loadObjectResults = dotConnect.setSQL(sqlQuery)
                        .loadObjectResults();

                return getValue(loadObjectResults)
                        .map(o -> new MetricValue(this.getMetric(), o))
                        .or(() -> Optional.of(new MetricValue(this.getMetric(), 0)));
            });
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public Optional<Object> getValue() {
        throw new NotSupportedException();
    }

    abstract boolean filterCondition(Map<String, Object> map);

    Optional<Object> getValue(List<Map<String, Object>> results) {
        return Optional.of(results.stream()
                .filter(this::filterCondition)
                .map(m -> m.get("count"))
                .findFirst()
                .orElse(0));
    }
}
