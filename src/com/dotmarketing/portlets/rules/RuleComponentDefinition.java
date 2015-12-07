package com.dotmarketing.portlets.rules;

import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.com.google.common.collect.Maps;
import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import java.io.Serializable;
import java.util.Map;

/**
 * @author Geoff M. Granum
 */
public abstract class RuleComponentDefinition implements Serializable {

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
}
 
