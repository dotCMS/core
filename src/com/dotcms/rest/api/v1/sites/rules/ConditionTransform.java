package com.dotcms.rest.api.v1.sites.rules;

import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConditionTransform {
    private final RulesAPI rulesAPI;
    private ConditionValueTransform conditionValueTransform;

    public ConditionTransform() { this(new ApiProvider()); }

    public ConditionTransform(ApiProvider apiProvider) {
        this.rulesAPI = apiProvider.rulesAPI();
        conditionValueTransform = new ConditionValueTransform(apiProvider);
    }

    public Condition restToApp(RestCondition rest) {
        Condition app = new Condition();
        return applyRestToApp(rest, app);
    }

    public Condition applyRestToApp(RestCondition rest, Condition app) {
        app.setName(rest.name);
        app.setConditionletId(rest.conditionlet);
        app.setComparison(rest.comparison);
        app.setOperator(Condition.Operator.valueOf(rest.operator));
        app.setPriority(rest.priority);
        app.setValues(rest.values.values().stream()
                .map(conditionValueTransform.toApp)
                .collect(Collectors.toList()));
        return app;
    }

    public RestCondition appToRest(Condition c) {

        return toRest.apply(c);
    }

    public Function<Condition, RestCondition> appToRestFn() {
        return toRest;
    }

    private final Function<Condition, RestCondition> toRest = (app) -> {

        RestCondition rest = new RestCondition.Builder()
                .id(app.getId())
                .name(app.getName())
                .conditionlet(app.getConditionletId())
                .comparison(app.getComparison())
                .operator(app.getOperator().name())
                .priority(app.getPriority())
                .values(app.getValues().stream()
                        .map(conditionValueTransform.toRest)
                        .collect(Collectors.toMap(rcv->rcv.id, Function.identity())))
                .build();

        return rest;
    };


}

