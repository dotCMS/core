package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Actionlet to add Key/Value to the Session.
 * The exact names that had to be set in params are: sessionKey and sessionValue.
 *
 * @author Oscar Arrieta
 * @version 1.0
 * @since 09-22-2015
 */
public class SetSessionAttributeActionlet extends RuleActionlet<SetSessionAttributeActionlet.Instance> {

    private static final String I18N_BASE = "api.system.ruleengine.actionlet.SetSessionAttribute";

    private static final String SESSION_VALUE = "sessionValue";
    private static final String SESSION_KEY = "sessionKey";

    public SetSessionAttributeActionlet() {
        super(I18N_BASE, new ParameterDefinition<>(1, SESSION_KEY, new TextInput<>(new TextType())),
              new ParameterDefinition<>(2, SESSION_VALUE, new TextInput<>(new TextType()))
        );
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        request.getSession().removeAttribute(instance.key);
        request.getSession().setAttribute(instance.key, instance.value);
        return true;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(parameters);
    }

    static class Instance implements RuleComponentInstance {

        private final String key;
        private final String value;

        public Instance(Map<String, ParameterModel> parameters) {
            key = parameters.get(SESSION_KEY).getValue();
            value = parameters.get(SESSION_VALUE).getValue();
        }
    }
}
