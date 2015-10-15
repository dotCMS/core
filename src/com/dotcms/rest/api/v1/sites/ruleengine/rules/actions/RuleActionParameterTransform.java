package com.dotcms.rest.api.v1.sites.ruleengine.rules.actions;

import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;

import java.util.function.Function;

public class RuleActionParameterTransform {
    private final RulesAPI rulesAPI;

    public RuleActionParameterTransform() { this(new ApiProvider()); }

    public RuleActionParameterTransform(ApiProvider apiProvider) { this.rulesAPI = apiProvider.rulesAPI(); }

    public final Function<RuleActionParameter, RestRuleActionParameter> toRest = (app) -> {

        RestRuleActionParameter rest = new RestRuleActionParameter.Builder()
                .id(app.getId())
                .key(app.getKey())
                .value(app.getValue())
                .build();


        return rest;
    };

    public final Function<RestRuleActionParameter, RuleActionParameter> toApp = (rest) -> {
        RuleActionParameter app = new RuleActionParameter();
        app.setId(rest.id);
        app.setKey(rest.key);
        app.setValue(rest.value);
        return app;
    };

    public RuleActionParameter applyRestToApp(RestRuleActionParameter rest, RuleActionParameter app) {
        app.setId(rest.id);
        app.setKey(rest.key);
        app.setValue(rest.value);
        return app;
    }

}

