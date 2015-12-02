package com.dotcms.rest.api.v1.system.ruleengine.actionlets;

import com.dotcms.rest.api.RestTransform;
import com.dotmarketing.portlets.rules.actionlet.ActionParameterDefinition;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ActionletTransform implements RestTransform<RuleActionlet, RestActionlet> {

    private final Function<RuleActionlet, RestActionlet> toRest = (app) -> new RestActionlet.Builder()
        .id(app.getId())
        .parameters(remapParameters(app.getParameters()))
        .i18nKey(app.getI18nKey())
        .build();

    /**
     * @todo ggranum: This method needs to be replaced with a RestActionPararmeterDefinition type, and possibly a container (plural form) as well.
     */
    private static Map<String, Map<String, String>> remapParameters(List<ActionParameterDefinition> parameters) {
        Map<String, Map<String, String>> mappedValues = new HashMap<>();
        if(!parameters.isEmpty()) {
            for (ActionParameterDefinition parameter : parameters) {
                mappedValues.put(parameter.getKey(), parameter.toMappedValues());
            }
        }
        return mappedValues;
    }

    @Override
    public RuleActionlet applyRestToApp(RestActionlet rest, RuleActionlet app) {
        throw new IllegalStateException("RuleActionlet is not modifiable.");
    }

    @Override
    public RestActionlet appToRest(RuleActionlet app) {
        return toRest.apply(app);
    }

    public Function<RuleActionlet, RestActionlet> appToRestFn() {
        return toRest;
    }
}

