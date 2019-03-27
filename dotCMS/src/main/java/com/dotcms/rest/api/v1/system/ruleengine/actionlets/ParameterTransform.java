package com.dotcms.rest.api.v1.system.ruleengine.actionlets;

import com.dotcms.rest.api.RestTransform;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.comparison.Comparison;
import java.util.function.Function;

public class ParameterTransform implements RestTransform<ParameterDefinition, RestParameterDefinition> {

    private final Function<ParameterDefinition, RestParameterDefinition> toRest = (app) -> new RestParameterDefinition.Builder()
                   .id(app.getKey())
                   .build();

    @Override
    public ParameterDefinition applyRestToApp(RestParameterDefinition rest, ParameterDefinition app) {
        throw new IllegalAccessError("Parameter Definitions are not editable");
    }

    @Override
    public RestParameterDefinition appToRest(ParameterDefinition app) {
        return toRest.apply(app);
    }

    @Override
    public Function<ParameterDefinition, RestParameterDefinition> appToRestFn() {
        return toRest;
    }
}

