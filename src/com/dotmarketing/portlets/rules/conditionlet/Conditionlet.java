package com.dotmarketing.portlets.rules.conditionlet;

import com.dotmarketing.portlets.rules.RuleComponentDefinition;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.exception.RuleConstructionFailedException;
import com.dotmarketing.portlets.rules.exception.RuleEvaluationFailedException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class Conditionlet<T extends RuleComponentInstance> extends RuleComponentDefinition {

    public static final String COMPARISON_KEY = "comparison";


    protected Conditionlet(String i18nKey, ParameterDefinition... parameterDefinitions) {
        super(i18nKey, parameterDefinitions);
    }

    public T checkValid(Condition condition) {
        Map<String, ParameterModel> params = condition.getParameters();
        for (Map.Entry<String, ParameterDefinition> entry : this.getParameterDefinitions().entrySet()) {
            entry.getValue().checkValid(params.get(entry.getKey()));
        }

        T instance;
        try {
            instance = instanceFrom(condition);
        } catch (RuleEngineException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleConstructionFailedException(e, "Could not create Conditionlet instance of type %s from provided model %s.",
                                                      this.getId(), condition.toString());
        }
        return instance;
    }

    public final boolean evaluate(HttpServletRequest request, HttpServletResponse response, Condition model) {
        T instance;
        try {
            instance = instanceFrom(model);
        } catch (RuleEngineException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleConstructionFailedException(e, "Could not create Conditionlet instance of type %s from provided model %s.",
                                                      this.getId(), model.toString());
        }
        try {
            return this.evaluate(request, response, instance);
        } catch (RuleEngineException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleEvaluationFailedException(e, "Could not evaluate Condition from model: " + model.toString());
        }
    }

    public abstract boolean evaluate(HttpServletRequest request, HttpServletResponse response, T instance);

    public T instanceFrom(Condition model) {
        return this.instanceFrom( model.getParameters() );
    }

    public abstract T instanceFrom(Map<String, ParameterModel> values);
}
