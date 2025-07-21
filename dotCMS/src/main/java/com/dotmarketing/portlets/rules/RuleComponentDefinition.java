package com.dotmarketing.portlets.rules;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.com.google.common.collect.Maps;
import javax.validation.constraints.NotNull;
import com.dotcms.rest.exception.InvalidRuleParameterException;
import com.dotmarketing.portlets.rules.exception.RuleConstructionFailedException;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.exception.RuleEvaluationFailedException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.annotation.Nullable;

/**
 * @author Geoff M. Granum
 */
public abstract class RuleComponentDefinition<T extends RuleComponentInstance> implements Serializable {

    private static final long serialVersionUID = 1L;
    protected final String id;
    protected final String i18nKey;
    private final Map<String, ParameterDefinition> parameterDefinitions;

    protected RuleComponentDefinition(String i18nKey, ParameterDefinition... parameterDefinitions) {
        this(null, i18nKey, parameterDefinitions);
    }

    /**
     * @deprecated Do not use this method directly, it exists only for the <em>Jackson</em>-binding
     * infrastructure
     */
    protected RuleComponentDefinition(String id, String i18nKey,
            ParameterDefinition... parameterDefinitions) {

        if (null == id) {
            id = this.getClass().getSimpleName();
        }
        this.id = id;
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
        String key = null;
        try {
            for (Map.Entry<String, ParameterDefinition> entry : this.getParameterDefinitions().entrySet()) {
                key = entry.getKey();
                entry.getValue().checkValid(params.get(key));
            }
        } catch (Exception e) {
            throw new RuleConstructionFailedException(e, "Could not create Component Instance of type %s from provided model %s: "
                                                         + "validation failed for parameter '%s'",
                                                      this.getId(), data.toString(), key);
        }
        T instance;
        try {
            instance = instanceFrom(params);
        } catch (RuleEngineException | InvalidRuleParameterException e) {
            throw e;
        } catch (Exception e) {
            Logger.warn(RuleComponentDefinition.class, "Unexpected error creating component.", e);
            throw new RuleConstructionFailedException(e, "Could not create Component Instance of type %s from provided model %s.",
                                                      this.getId(), data.toString());
        }
        return instance;
    }



    public final boolean doEvaluate(HttpServletRequest request, HttpServletResponse response, T instance) {
        long mils = System.currentTimeMillis();
        try {
            if(Logger.isDebugEnabled(this.getClass())) {
                Logger.debug(this.getClass(), "Evaluating ComponentDefinition " + this.toLogString());
            }
            boolean result = this.evaluate(request, response, instance);
            logEvalSuccess(mils, result);

            return result;
        } catch (RuleEngineException e) {
            logEvalError(mils);
            throw e;
        } catch (Exception e) {
            logEvalError(mils);
            throw new RuleEvaluationFailedException(e, "Could not evaluate Condition from model: " + instance);
        }
    }

    private void logEvalSuccess(long mils, boolean result) {
        if(Logger.isDebugEnabled(this.getClass())) {
            Logger.debug(this.getClass(), "Evaluation successful: " + this.toLogString()
                                          + " -  Duration (ms): " + (System.currentTimeMillis() - mils)
                                          + " -  Result: " + result);
        }
    }

    private void logEvalError(long mils) {
        if(Logger.isDebugEnabled(this.getClass())) {
            Logger.debug(this.getClass(), "Evaluation failed: " + this.toLogString()
                                          + " -  Duration (ms): " + (System.currentTimeMillis() - mils));
        }
    }

    public String toLogString(){
        return this.getClass().getSimpleName();
    }

    public abstract boolean evaluate(HttpServletRequest request, HttpServletResponse response, T instance);

    /**
     * Utility type used to correctly read immutable object from JSON representation.
     *
     * @deprecated Do not use this type directly, it exists only for the <em>Jackson</em>-binding
     * infrastructure
     */
    @Deprecated
    @JsonDeserialize
    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
    static protected final class Json {

        @javax.annotation.Nullable
        public String id;
        @javax.annotation.Nullable
        public String i18nKey;
        @javax.annotation.Nullable
        public Map<String, ParameterDefinition> parameterDefinitions;

        @JsonProperty("id")
        public void setId(@Nullable final String id) {
            this.id = id;
        }

        @JsonProperty("i18nKey")
        public void setI18nKey(@Nullable final String i18nKey) {
            this.i18nKey = i18nKey;
        }

        @JsonProperty("parameterDefinitions")
        public void setParameterDefinitions(
                @Nullable Map<String, ParameterDefinition> parameterDefinitions) {
            this.parameterDefinitions = parameterDefinitions;
        }
    }

}