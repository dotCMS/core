package com.dotcms.rest.api.v1.sites.rules;

import com.dotmarketing.portlets.rules.model.ConditionValue;

import java.util.Map;
import java.util.function.Function;

public class ConditionValueTransform {
    public final Function<ConditionValue, RestConditionValue> toRest = (app) -> {

        RestConditionValue rest = new RestConditionValue.Builder().id(app.getId()).value(app.getValue()).priority(app.getPriority()).build();

        return rest;
    };
    public final Function<RestConditionValue, ConditionValue> toApp = (rest) -> {
        ConditionValue app = new ConditionValue();
        app.setId(rest.id);
        app.setPriority(rest.priority);
        app.setValue(rest.value);
        return app;
    };

    public ConditionValue applyRestToApp(RestConditionValue rest, ConditionValue app) {
        app.setId(rest.id);
        app.setPriority(rest.priority);
        app.setValue(rest.value);
        return app;
    }

    public final Function<Map.Entry<String, RestConditionValue>, ConditionValue> mapEntrytoApp = (entry) -> {
        ConditionValue app = new ConditionValue();
        app.setId(entry.getKey());
        app.setPriority(entry.getValue().priority);
        app.setValue(entry.getValue().value);
        return app;
    };
}

