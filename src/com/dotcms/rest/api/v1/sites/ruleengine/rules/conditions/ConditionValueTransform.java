package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import com.dotmarketing.portlets.rules.model.ConditionValue;
import java.util.function.Function;

public class ConditionValueTransform {
    public final Function<ConditionValue, RestConditionValue> toRest = (app) -> {

        RestConditionValue rest = new RestConditionValue.Builder()
            .id(app.getId())
            .value(app.getValue())
            .key(app.getKey())
            .priority(app.getPriority())
            .build();

        return rest;
    };
    public final Function<RestConditionValue, ConditionValue> toApp = (rest) -> {
        ConditionValue app = new ConditionValue();
        app.setId(rest.id);
        app.setPriority(rest.priority);
        app.setValue(rest.value);
        app.setKey(rest.key);
        return app;
    };

    public ConditionValue applyRestToApp(RestConditionValue rest, ConditionValue app) {
        app.setId(rest.id);
        app.setPriority(rest.priority);
        app.setValue(rest.value);
        app.setKey(rest.key);
        return app;
    }
}

