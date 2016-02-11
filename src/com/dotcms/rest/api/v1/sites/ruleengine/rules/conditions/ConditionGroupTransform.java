package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import com.dotcms.repackage.org.apache.commons.lang.SerializationUtils;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.business.ApiProvider;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.LogicalOperator;
import com.dotmarketing.util.Logger;

import java.util.HashMap;
import java.util.Map;

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

    public ConditionGroup applyRestToApp(RestConditionGroup rest, ConditionGroup cgroup) {
    	ConditionGroup app = (ConditionGroup) SerializationUtils.clone(cgroup);
        app.setOperator(LogicalOperator.valueOf(rest.operator));
        app.setPriority(rest.priority);
        return app;
    }

    public RestConditionGroup appToRest(ConditionGroup app) {
        Map<String, Boolean> restConditionMap = new HashMap<>();

        if(app.getConditions()!=null && !app.getConditions().isEmpty()) {
            for (Condition condition : app.getConditions()) {

                if (rulesAPI.findConditionlet(condition.getConditionletId()) != null){
                    restConditionMap.put(condition.getId(), true);
                } else {
                    //In case the conditionlet from DB no longer exists in the List of Server Conditionlets.
                    //This case mostly for custom conditionlets (OSGI).
                    Logger.error(this, "Conditionlet not found: " + condition.getConditionletId());
                    throw new NotFoundException("Conditionlet not found: '%s'", condition.getConditionletId());
                }
            }
        }

        RestConditionGroup rest = new RestConditionGroup.Builder()
                .operator(app.getOperator().name())
                .id(app.getId())
                .priority(app.getPriority())
                .conditions(restConditionMap)
                .build();

        return rest;
    }

}

