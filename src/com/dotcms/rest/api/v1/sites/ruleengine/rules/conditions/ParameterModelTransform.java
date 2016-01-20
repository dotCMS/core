package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import com.dotcms.repackage.org.apache.commons.lang.SerializationUtils;
import com.dotmarketing.portlets.rules.model.ParameterModel;

import java.util.function.Function;

public class ParameterModelTransform {
    public final Function<ParameterModel, RestConditionValue> toRest = (app) -> {

        RestConditionValue rest = new RestConditionValue.Builder()
            .id(app.getId())
            .value(app.getValue())
            .key(app.getKey())
            .priority(app.getPriority())
            .build();

        return rest;
    };
    public final Function<RestConditionValue, ParameterModel> toApp = (rest) -> {
        ParameterModel app = new ParameterModel();
        app.setId(rest.id);
        app.setPriority(rest.priority);
        app.setValue(rest.value);
        app.setKey(rest.key);
        return app;
    };

    public ParameterModel applyRestToApp(RestConditionValue rest, ParameterModel pModel) {
    	ParameterModel app = (ParameterModel) SerializationUtils.clone(pModel);

        app.setId(rest.id);
        app.setPriority(rest.priority);
        app.setValue(rest.value);
        app.setKey(rest.key);
        return app;
    }
}

