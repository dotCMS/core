package com.dotcms.rest.api.v1.system.ruleengine.actionlets;

import com.dotcms.rest.api.RestTransform;
import com.dotmarketing.portlets.rules.conditionlet.Comparison;
import java.util.function.Function;

public class ComparisonTransform implements RestTransform<Comparison, RestComparison> {

    private final Function<Comparison, RestComparison> toRest = (app) -> new RestComparison.Builder()
                   .id(app.getId())
                   .label(app.getLabel())
                   .build();

    @Override
    public Comparison applyRestToApp(RestComparison rest, Comparison app) {
        throw new IllegalAccessError("Comparisons are not editable");
    }

    @Override
    public RestComparison appToRest(Comparison app) {
        return toRest.apply(app);
    }

    @Override
    public Function<Comparison, RestComparison> appToRestFn() {
        return toRest;
    }
}

