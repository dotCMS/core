package com.dotcms.rest.api.v1.sites.rules;

import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.model.ConditionValue;
import java.util.function.Function;

public class ConditionValueTransform {
    public final Function<ConditionValue, RestConditionValue> toRest = (app) -> {

        RestConditionValue rest = new RestConditionValue.Builder().id(app.getId()).value(app.getValue()).priority(app.getPriority()).build();

        return rest;
    };
    public final Function<RestConditionValue, ConditionValue> toApp = (rest) -> {
        ConditionValue app = new ConditionValue();
        app.setPriority(rest.priority);
        app.setValue(rest.value);
        return app;
    };
    private final RulesAPI rulesAPI;

    public ConditionValueTransform() { this(new ApiProvider()); }

    public ConditionValueTransform(ApiProvider apiProvider) { this.rulesAPI = apiProvider.rulesAPI(); }
}

