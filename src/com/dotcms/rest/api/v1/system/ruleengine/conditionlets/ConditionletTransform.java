package com.dotcms.rest.api.v1.system.ruleengine.conditionlets;

import com.dotcms.rest.api.v1.system.ruleengine.actionlets.ComparisonTransform;
import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConditionletTransform {

    private final Function<Conditionlet, RestConditionlet> toRest = (app) -> {
        ComparisonTransform comparisonTransform = new ComparisonTransform();
        RestConditionlet rest = new RestConditionlet.Builder()
                                        .id(app.getId())
                                        .i18nKey(app.getI18nKey())
                                        .comparisons(app.getComparisons()
                                                        .stream()
                                                        .map(comparisonTransform.appToRestFn())
                                                        .collect(Collectors.toList()))
                                        .build();

        return rest;
    };

    public Conditionlet applyRestToApp(RestConditionlet rest, Conditionlet app) {
        throw new IllegalAccessError("Conditionlets are not editable");
    }

    public RestConditionlet appToRest(Conditionlet c) {
        return toRest.apply(c);
    }

    public Function<Conditionlet, RestConditionlet> appToRestFn() {
        return toRest;
    }
}

