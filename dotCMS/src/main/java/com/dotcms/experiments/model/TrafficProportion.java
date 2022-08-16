package com.dotcms.experiments.model;

import java.util.Collections;
import java.util.Map;

public class TrafficProportion {

    private Type type;
    private Map<String, Integer> percentages;

    private TrafficProportion(final Type type, final Map<String, Integer> percentages) {
        this.type = type;
        this.percentages = percentages;
    }

    public static TrafficProportion createSplitEvenlyTraffic() {
        return new TrafficProportion(Type.SPLIT_EVENLY,
                Collections.emptyMap());
    }

    public static TrafficProportion createCustomTraffic(final Map<String, Integer> percentages) {
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

    public Map<String, Integer> getPercentages() {
        return percentages;
    }
}
