package com.dotcms.rest.api.v1.sites.ruleengine;

import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;

import java.util.function.Function;

public class RuleActionParameterTransform {
    private final RulesAPI rulesAPI;

    public RuleActionParameterTransform() { this(new ApiProvider()); }

    public RuleActionParameterTransform(ApiProvider apiProvider) { this.rulesAPI = apiProvider.rulesAPI(); }

    public RuleActionParameter restToApp(RestRuleActionParameter rest) {
        RuleActionParameter app = new RuleActionParameter();
        return applyRestToApp(rest, app);
    }

    public RuleActionParameter applyRestToApp(RestRuleActionParameter rest, RuleActionParameter app) {
        app.setId(rest.id);
        app.setKey(rest.key);
        app.setValue(rest.value);
        return app;
    }

    public RestRuleActionParameter appToRest(RuleActionParameter app) {
        return toRest.apply(app);
    }

    public Function<RuleActionParameter, RestRuleActionParameter> appToRestFn() {
        return toRest;
    }

    public final Function<RuleActionParameter, RestRuleActionParameter> toRest = (app) -> {

        RestRuleActionParameter rest = new RestRuleActionParameter.Builder()
                .id(app.getId())
                .key(app.getKey())
                .value(app.getValue())
                .build();


        return rest;
    };


}

