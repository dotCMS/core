package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Actionlet to add Key/Value to the Response header.
 * The exact names that had to be set in params are: headerKey and headerValue.
 *
 * @author Geoff M. Granum
 * @version 1.0
 * @since 09-22-2015
 */
public class SetResponseHeaderActionlet extends RuleActionlet<SetResponseHeaderActionlet.Instance> {

    private static final String I18N_BASE = "api.system.ruleengine.actionlet.SetResponseHeader";

    public static final String HEADER_KEY = "headerKey";
    public static final String HEADER_VALUE = "headerValue";

    public SetResponseHeaderActionlet() {
        super(I18N_BASE,
              new ParameterDefinition<>(1, HEADER_KEY, new TextInput<>(new TextType().required())),
              new ParameterDefinition<>(2, HEADER_VALUE, new TextInput<>(new TextType())));
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> parameters) {
        return new Instance(parameters);
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
        response.setHeader(instance.key, instance.value);
        return true;
    }

    static class Instance implements RuleComponentInstance {

        private final String key;
        private final String value;

        public Instance(Map<String, ParameterModel> parameters) {
            key = parameters.get(HEADER_KEY).getValue();
            String v = parameters.get(HEADER_VALUE).getValue();
            value = v != null ? v : "";
            Preconditions.checkArgument(StringUtils.isNotBlank(key), "SetResponseHeaderActionlet requires valid key.");
        }
    }
}
