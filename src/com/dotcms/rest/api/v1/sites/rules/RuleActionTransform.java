package com.dotcms.rest.api.v1.sites.rules;

import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RuleActionTransform {
    private final RulesAPI rulesAPI;
    private RuleActionParameterTransform parameterTransform;

    public RuleActionTransform() { this(new ApiProvider()); }

    public RuleActionTransform(ApiProvider apiProvider) {
        this.rulesAPI = apiProvider.rulesAPI();
        this.parameterTransform = new RuleActionParameterTransform(apiProvider);
    }

    public RuleAction restToApp(RestRuleAction rest) {
        RuleAction app = new RuleAction();
        return applyRestToApp(rest, app);
    }

    public RuleAction applyRestToApp(RestRuleAction rest, RuleAction app) {
        app.setId(rest.id);
        app.setName(rest.name);
        app.setActionlet(rest.actionlet);
        app.setPriority(rest.priority);
        return app;
    }

    public RestRuleAction appToRest(RuleAction app) {
        return toRest.apply(app);
    }

    public Function<RuleAction, RestRuleAction> appToRestFn() {
        return toRest;
    }

    public final Function<RuleAction, RestRuleAction> toRest = (app) -> {

        Map<String, RestRuleActionParameter> params = app.getParameters().stream()
                .map(parameterTransform.appToRestFn())
                .collect(Collectors.toMap(r -> r.id, Function.identity()));

        RestRuleAction rest = new RestRuleAction.Builder()
                .id(app.getId())
                .name(app.getName())
                .actionlet(app.getActionlet())
                .priority(app.getPriority())
                .parameters(params)
                .build();


        return rest;
    };


}

