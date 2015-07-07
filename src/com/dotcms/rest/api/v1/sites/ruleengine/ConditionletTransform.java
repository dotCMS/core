package com.dotcms.rest.api.v1.sites.ruleengine;

import com.dotmarketing.portlets.rules.conditionlet.Conditionlet;
import java.util.function.Function;

public class ConditionletTransform {

    private final Function<Conditionlet, RestConditionlet> toRest = (app) -> {

        RestConditionlet rest = new RestConditionlet.Builder()
                                        .id("stub")
                                        .localizedName(app.getLocalizedName())
                                        .languageId(app.getLanguageId())
                                        .build();

        return rest;
    };

    public Conditionlet applyRestToApp(RestConditionlet rest, Conditionlet app) {

        return app;
    }

    public RestConditionlet appToRest(Conditionlet c) {

        return toRest.apply(c);
    }

    public Function<Conditionlet, RestConditionlet> appToRestFn() {
        return toRest;
    }
}

