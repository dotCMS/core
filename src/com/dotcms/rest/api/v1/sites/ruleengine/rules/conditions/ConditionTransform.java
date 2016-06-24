package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.org.apache.commons.lang.SerializationUtils;
import com.dotmarketing.portlets.rules.model.Condition;

import com.dotmarketing.portlets.rules.model.LogicalOperator;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.util.Logger;
import java.util.Map;
import java.util.function.Function;

public class ConditionTransform {
    private final ParameterModelTransform parameterModelTransform = new ParameterModelTransform();

    public Condition restToApp(RestCondition rest) {
        Condition app = new Condition();
        return applyRestToApp(rest, app);
    }

    public Condition applyRestToApp(RestCondition rest, Condition cond) {
    	Condition app = (Condition) SerializationUtils.clone(cond);
        app.setConditionGroup(rest.owningGroup);
        app.setConditionletId(rest.conditionlet);
        app.setOperator(LogicalOperator.valueOf(rest.operator));
        app.setPriority(rest.priority);
        // reset the values with the ones coming from rest
		if(app.getValues()!=null){
			app.getValues().clear();
		}
        if(rest.values!=null){
        	for(Map.Entry<String, RestConditionValue> entry :  rest.values.entrySet()){
        		app.addValue(entry.getValue().key, entry.getValue().value);
        	}
        }

        app.checkValid();
        return app;
    }

    public RestCondition appToRest(Condition c) {

        return toRest.apply(c);
    }

    private final Function<Condition, RestCondition> toRest = (app) -> {

        Map<String, RestConditionValue> values = Maps.newHashMap();
        for (ParameterModel parameterModel : app.getValues()) {
            RestConditionValue restCondition = parameterModelTransform.toRest(parameterModel);
            RestConditionValue existing = values.put(restCondition.key, restCondition);
            if(existing != null){
                Logger.warn(ConditionTransform.class, "Duplicate key found for condition parameter '" + existing.key +
                                                      "'. Was '" + existing.value + "', is now '" + restCondition.value + "'.");
            }
        }

        RestCondition rest = new RestCondition.Builder()
                .id(app.getId())
                .owningGroup(app.getConditionGroup())
                .conditionlet(app.getConditionletId())
                .operator(app.getOperator().name())
                .priority(app.getPriority())
                .values(values)
                .build();

        return rest;
    };


}

