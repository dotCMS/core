package com.dotcms.rest.api.v1.sites.ruleengine.rules.actions;

import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.org.apache.commons.lang.SerializationUtils;
import com.dotmarketing.business.ApiProvider;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.RuleAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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

    public RuleAction applyRestToApp(RestRuleAction rest, RuleAction rule) {
    	RuleAction app = (RuleAction) SerializationUtils.clone(rule);
    	if(rule.getParameters()!=null){
	    	for(Map.Entry <String, ParameterModel> entry: rule.getParameters().entrySet()){
	    		app.addParameter(entry.getValue());
	    	}
    	}
    	// rest the parameters, adding the rest params
    	app.setParameters(new HashMap<>());

        app.setId(rest.id);
        app.setRuleId(rest.owningRule);
        app.setActionlet(rest.actionlet);
        app.setPriority(rest.priority);
        if(rest.parameters!=null)
            app.setParameters(rest.parameters);

        return app;
    }

    public RestRuleAction appToRest(RuleAction app) {
        return toRest.apply(app);
    }

    public Function<RuleAction, RestRuleAction> appToRestFn() {
        return toRest;
    }

    public final Function<RuleAction, RestRuleAction> toRest = (app) -> {

        Map<String, ParameterModel> params = null;

        if(app.getParameters()!=null) {

            params = app.getParameters();
        }

        RestRuleAction rest = new RestRuleAction.Builder()
                .id(app.getId())
                .owningRule(app.getRuleId())
                .actionlet(app.getActionlet())
                .priority(app.getPriority())
                .parameters(params)
                .build();


        return rest;
    };


}

