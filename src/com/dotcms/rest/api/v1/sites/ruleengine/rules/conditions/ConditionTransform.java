package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import com.dotcms.repackage.org.apache.commons.lang.SerializationUtils;
import com.dotmarketing.portlets.rules.model.Condition;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConditionTransform {
    private final ParameterModelTransform parameterModelTransform = new ParameterModelTransform();

    public Condition restToApp(RestCondition rest) {
        Condition app = new Condition();
        return applyRestToApp(rest, app);
    }

    public Condition applyRestToApp(RestCondition rest, Condition cond) {
    	Condition app = (Condition) SerializationUtils.clone(cond);
        app.setName(rest.name);
        app.setConditionGroup(rest.owningGroup);
        app.setConditionletId(rest.conditionlet);
        app.setOperator(Condition.Operator.valueOf(rest.operator));
        app.setPriority(rest.priority);
        // reset the values with the ones coming from rest
        app.getValues().clear();
        if(rest.values!=null){
        	for(Map.Entry<String, RestConditionValue> entry :  rest.values.entrySet()){
        		app.addValue(entry.getValue().key, entry.getValue().value);
        	}
        }

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
                .operator(app.getOperator().name())
                .priority(app.getPriority())
                .values(app.getValues().stream()
                           .map(parameterModelTransform.toRest)
                           .collect(Collectors.toMap(restCondition -> restCondition.key, Function.identity())))
                .build();

        return rest;
    };


}

