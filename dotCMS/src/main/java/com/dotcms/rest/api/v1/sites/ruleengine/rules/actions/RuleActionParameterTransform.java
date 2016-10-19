package com.dotcms.rest.api.v1.sites.ruleengine.rules.actions;

import com.dotcms.repackage.org.apache.commons.lang.SerializationUtils;
import com.dotmarketing.business.ApiProvider;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotmarketing.portlets.rules.model.ParameterModel;

import java.util.function.Function;

public class RuleActionParameterTransform {
    private final RulesAPI rulesAPI;

    public RuleActionParameterTransform() { this(new ApiProvider()); }

    public RuleActionParameterTransform(ApiProvider apiProvider) { this.rulesAPI = apiProvider.rulesAPI(); }

    public final Function<ParameterModel, RestRuleActionParameter> toRest = (app) -> {

        RestRuleActionParameter rest = new RestRuleActionParameter.Builder()
                .id(app.getId())
                .key(app.getKey())
                .value(app.getValue())
                .build();


        return rest;
    };

    public final Function<RestRuleActionParameter, ParameterModel> toApp = (rest) -> {
        ParameterModel app = new ParameterModel();
        app.setId(rest.id);
        app.setKey(rest.key);
        app.setValue(rest.value);
        return app;
    };

    public ParameterModel applyRestToApp(RestRuleActionParameter rest, ParameterModel pmodel) {
    	ParameterModel app = (ParameterModel) SerializationUtils.clone(pmodel);
        app.setId(rest.id);
        app.setKey(rest.key);
        app.setValue(rest.value);
        return app;
    }

}

