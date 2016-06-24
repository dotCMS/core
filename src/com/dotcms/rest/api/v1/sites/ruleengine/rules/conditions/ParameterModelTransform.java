package com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions;

import com.dotcms.repackage.com.google.common.base.Function;
import com.dotcms.repackage.org.apache.commons.lang.SerializationUtils;
import com.dotmarketing.portlets.rules.model.ParameterModel;

public class ParameterModelTransform {

    public final ToRestFn toRestFn = new ToRestFn();
    public final ToAppFn toAppFn = new ToAppFn();

    public static class ToRestFn implements Function<ParameterModel, RestConditionValue> {
        @Override
        public RestConditionValue apply(ParameterModel app) {
            RestConditionValue rest = new RestConditionValue.Builder()
                .id(app.getId())
                .value(app.getValue())
                .key(app.getKey())
                .priority(app.getPriority())
                .build();

            return rest;
        }
    }

    public static class ToAppFn implements Function<RestConditionValue, ParameterModel> {

        @Override
        public ParameterModel apply(RestConditionValue rest) {
            ParameterModel app = new ParameterModel();
            app.setId(rest.id);
            app.setPriority(rest.priority);
            app.setValue(rest.value);
            app.setKey(rest.key);
            return app;
        }
    }

    public final RestConditionValue toRest(ParameterModel app){
        return this.toRestFn.apply(app);

    };
    public final ParameterModel toApp(RestConditionValue rest) {
        return this.toAppFn.apply(rest);
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

