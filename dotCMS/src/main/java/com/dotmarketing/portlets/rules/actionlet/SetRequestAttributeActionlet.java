package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.rest.exception.InvalidRuleParameterException;
import com.google.common.base.Preconditions;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;

/**
 * Actionlet to add Key/Value to the Request.
 * The exact names that had to be set in params are: requestKey and requestValue.
 *
 * @author Oscar Arrieta
 * @version 1.0
 * @since 09-22-2015
 */
public class SetRequestAttributeActionlet extends RuleActionlet<SetRequestAttributeActionlet.Instance> {

    private static final String I18N_BASE = "api.system.ruleengine.actionlet.SetRequestAttribute";

    public static final String REQUEST_KEY = "requestKey";
    public static final String REQUEST_VALUE = "requestValue";

    public SetRequestAttributeActionlet() {
        super(I18N_BASE,
              new ParameterDefinition<>(1, REQUEST_KEY, new TextInput<>(new TextType().required())),
              new ParameterDefinition<>(2, REQUEST_VALUE, new TextInput<>(new TextType())));
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(parameters);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        request.setAttribute(instance.key, instance.value);
        return true;
    }

    /**
     * Refuses request attribute names that only dotCMS sets.
     *
     * @param parameters The action parameters, keyed by parameter name.
     * @return The reserved attribute name, or empty when the name is an ordinary one.
     */
    @Override
    public Optional<String> getRefusedName(final Map<String, ParameterModel> parameters) {
        return reservedAttributeName(parameters, REQUEST_KEY);
    }

    static class Instance implements RuleComponentInstance {

        private final String key;
        private final String value;

        /**
         * Builds the action from its parameters, refusing a reserved request attribute name with a
         * client error.
         */
        public Instance(Map<String, ParameterModel> parameters) {
            if (reservedAttributeName(parameters, REQUEST_KEY).isPresent()) {
                throw new InvalidRuleParameterException("The attribute name is reserved for dotCMS");
            }
            key = parameters.get(REQUEST_KEY).getValue();
            String v = parameters.get(REQUEST_VALUE).getValue();
            value = v != null ? v : "";
            Preconditions.checkArgument(StringUtils.isNotBlank(key), "SetRequestAttributeActionlet requires valid key.");
        }
    }
}
