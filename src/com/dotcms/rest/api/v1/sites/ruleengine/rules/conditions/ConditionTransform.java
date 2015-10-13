package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import com.dotmarketing.portlets.rules.model.Condition;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConditionTransform {
    private final ConditionValueTransform conditionValueTransform = new ConditionValueTransform();

    public Condition restToApp(RestCondition rest) {
        Condition app = new Condition();
        return applyRestToApp(rest, app);
    }

    public Condition applyRestToApp(RestCondition rest, Condition app) {
        app.setName(rest.name);
        app.setConditionGroup(rest.owningGroup);
        app.setConditionletId(rest.conditionlet);
        app.setComparison(rest.comparison);
        app.setOperator(Condition.Operator.valueOf(rest.operator));
        app.setPriority(rest.priority);
        if(rest.values!=null)
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
                .owningGroup(app.getConditionGroup())
                .conditionlet(app.getConditionletId())
                .comparison(app.getComparison())
                .operator(app.getOperator().name())
                .priority(app.getPriority())
                .values(app.getValues().stream()
                           .map(conditionValueTransform.toRest)
                           .collect(Collectors.toMap(restCondition -> restCondition.key, Function.identity())))
                .build();

        return rest;
    };


}

