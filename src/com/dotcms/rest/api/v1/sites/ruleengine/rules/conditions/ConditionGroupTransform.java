package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConditionGroupTransform {
    private final RulesAPI rulesAPI;

    public ConditionGroupTransform() { this(new ApiProvider()); }

    public ConditionGroupTransform(ApiProvider apiProvider) {
        this.rulesAPI = apiProvider.rulesAPI();
    }

    public ConditionGroup restToApp(RestConditionGroup rest) {
        ConditionGroup app = new ConditionGroup();
        return applyRestToApp(rest, app);
    }

    public ConditionGroup applyRestToApp(RestConditionGroup rest, ConditionGroup app) {
        app.setOperator(Condition.Operator.valueOf(rest.operator));
        app.setPriority(rest.priority);
        return app;
    }

    public RestConditionGroup appToRest(ConditionGroup app) {
        return toRest.apply(app);
    }

    public Function<ConditionGroup, RestConditionGroup> appToRestFn() {
        return toRest;
    }

    public final Function<ConditionGroup, RestConditionGroup> toRest = (app) -> {

        Map<String, Boolean> restConditionMap = new HashMap<>();

        if(app.getConditions()!=null && !app.getConditions().isEmpty()) {
            restConditionMap = app.getConditions().stream()
                .collect(Collectors.toMap(Condition::getId, c -> Boolean.TRUE));
        }

        RestConditionGroup rest = new RestConditionGroup.Builder()
                .operator(app.getOperator().name())
                .id(app.getId())
                .priority(app.getPriority())
                .conditions(restConditionMap)
                .build();

        return rest;
    };


}

