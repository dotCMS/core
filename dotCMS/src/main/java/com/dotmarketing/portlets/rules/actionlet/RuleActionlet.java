package com.dotmarketing.portlets.rules.actionlet;

import com.google.common.base.Objects;
import com.dotmarketing.portlets.rules.RuleComponentDefinition;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.util.ReservedAttributeNames;
import java.util.Map;
import java.util.Optional;

public abstract class RuleActionlet<T extends RuleComponentInstance> extends RuleComponentDefinition<T> {

    private static final long serialVersionUID = 1L;

    public RuleActionlet(String i18nKey, ParameterDefinition... parameterDefinitions) {
        super(i18nKey, parameterDefinitions);
    }

    /**
     * Tells whether this action refuses one of the given parameter values, such as an attribute or
     * header name that only dotCMS may set. Unlike validation, this never throws, so a rule can
     * ignore an already stored action it refuses and keep running its other actions.
     *
     * @param parameters The action parameters, keyed by parameter name.
     * @return The name of a parameter value this action refuses, as a canonical constant, never
     * the stored text; empty when the parameters are accepted. Actionlets that refuse nothing
     * keep this default and always return empty.
     */
    public Optional<String> getRefusedName(final Map<String, ParameterModel> parameters) {
        return Optional.empty();
    }

    /**
     * Tells whether the given parameter holds an attribute name that only dotCMS sets (see
     * {@link ReservedAttributeNames}), for actionlets that write session or request attributes.
     *
     * @param parameters The action parameters, keyed by parameter name; may be {@code null}.
     * @param key        The parameter that holds the attribute name.
     * @return The reserved name the attribute name matches, as {@link ReservedAttributeNames#reservedName}
     * returns it (for a site SAML key, the key without the site id); empty when the name is
     * missing or an ordinary one.
     */
    protected static Optional<String> reservedAttributeName(final Map<String, ParameterModel> parameters,
            final String key) {
        final ParameterModel parameter = parameters == null ? null : parameters.get(key);
        return ReservedAttributeNames.reservedName(parameter == null ? null : parameter.getValue());
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
