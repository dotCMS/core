package com.dotmarketing.portlets.rules;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.rest.exception.InvalidConditionParameterException;
import com.dotmarketing.portlets.rules.exception.RuleConstructionFailedException;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.exception.RuleEvaluationFailedException;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import java.io.Serializable;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Geoff M. Granum
 */
public abstract class RuleComponentDefinition<T extends RuleComponentInstance> implements Serializable {

    private static final long serialVersionUID = 1L;
    protected final String id;
    protected final String i18nKey;
    private final Map<String, ParameterDefinition> parameterDefinitions;

    protected RuleComponentDefinition(String i18nKey, ParameterDefinition... parameterDefinitions) {
        this.id = this.getClass().getSimpleName();
        this.i18nKey = i18nKey;
        Map<String, ParameterDefinition> defs = Maps.newLinkedHashMap();
        for (ParameterDefinition def : parameterDefinitions) {
            defs.put(def.getKey(), def);
        }
        this.parameterDefinitions = ImmutableMap.copyOf(defs);
    }

    public Map<String, ParameterDefinition> getParameterDefinitions() {
        return parameterDefinitions;
    }

    /**
     * The unique type id for this Definition implementation.
     */
    @NotNull
    public final String getId() {
        return this.id;
    }

    public String getI18nKey(){
        return this.i18nKey;
    }

    public abstract T instanceFrom(Map<String, ParameterModel> parameters);

    public final T doCheckValid(RuleComponentModel data) {
        Map<String, ParameterModel> params = data.getParameters();
        for (Map.Entry<String, ParameterDefinition> entry : this.getParameterDefinitions().entrySet()) {
        	entry.getValue().checkValid(params.get(entry.getKey()));
        }

        T instance;
        try {
            instance = instanceFrom(params);
        } catch (RuleEngineException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleConstructionFailedException(e, "Could not create Component Instance of type %s from provided model %s.",
                                                      this.getId(), data.toString());
        }
        return instance;
    }



    public final boolean doEvaluate(HttpServletRequest request, HttpServletResponse response, T instance) {
        try {
            return this.evaluate(request, response, instance);
        } catch (RuleEngineException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleEvaluationFailedException(e, "Could not evaluate Condition from model: " + instance);
        }
    }

    public abstract boolean evaluate(HttpServletRequest request, HttpServletResponse response, T instance);
}

