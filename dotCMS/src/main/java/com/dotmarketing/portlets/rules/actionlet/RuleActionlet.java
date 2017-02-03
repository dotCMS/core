package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.base.Objects;
import com.dotmarketing.portlets.rules.RuleComponentDefinition;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;

public abstract class RuleActionlet<T extends RuleComponentInstance> extends RuleComponentDefinition<T> {

    private static final long serialVersionUID = 1L;

    public RuleActionlet(String i18nKey, ParameterDefinition... parameterDefinitions) {
        super(i18nKey, parameterDefinitions);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) { return true; }
        if(!(o instanceof RuleActionlet)) { return false; }
        RuleActionlet that = (RuleActionlet)o;
        return Objects.equal(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
