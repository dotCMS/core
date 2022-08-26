package com.dotcms.experiments.model;

import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * DB:
 * experiment (traffic_type, traffic_proportion json)
 */

public class TrafficProportion implements Serializable {

    private Type type;
    private Map<String, Float> percentages;

    @VisibleForTesting
    public TrafficProportion() {
    }

    public TrafficProportion(final Type type, final Map<String, Float> percentages) {
        this.type = type;
        this.percentages = percentages;
    }

    public static TrafficProportion createSplitEvenlyTraffic() {
        return new TrafficProportion(Type.SPLIT_EVENLY,
                Collections.emptyMap());
    }

    public static TrafficProportion createCustomTraffic(final Map<String, Float> percentages) {
        return new TrafficProportion(Type.CUSTOM_PERCENTAGES,
                percentages);
    }

    public enum Type {
        SPLIT_EVENLY,
        CUSTOM_PERCENTAGES
    }

    public Type getType() {
        return type;
    }

    public Map<String, Float> getPercentages() {
        return percentages;
    }
}
