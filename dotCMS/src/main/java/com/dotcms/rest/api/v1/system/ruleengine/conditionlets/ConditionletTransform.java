package com.dotcms.rest.api.v1.system.ruleengine.conditionlets;

import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import java.util.function.Function;

public class ConditionletTransform {

    private final Function<Conditionlet<?>, RestConditionlet> toRest = (app) -> {
        RestConditionlet rest = new RestConditionlet.Builder()
                                        .id(app.getId())
                                        .i18nKey(app.getI18nKey())
                                        .parameters(app.getParameterDefinitions() )
                                        .build();

        return rest;
    };

    public Conditionlet applyRestToApp(RestConditionlet rest, Conditionlet app) {
        throw new IllegalAccessError("Conditionlets are not editable");
    }

    public RestConditionlet appToRest(Conditionlet c) {
        return toRest.apply(c);
    }

    public Function<Conditionlet<?>, RestConditionlet> appToRestFn() {
        return toRest;
    }
}

