package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.portlets.rules.exception.InvalidActionInstanceException;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Actionlet to add Key/Value to the Request.
 * The exact names that had to be set in params are: requestKey and requestValue.
 *
 * @author Oscar Arrieta
 * @version 1.0
 * @since 09-22-2015
 */
public class SetRequestAttributeActionlet extends RuleActionlet {

    private static final String I18N_BASE = "api.system.ruleengine.actionlet.SetRequestAttribute";

    public static final String REQUEST_KEY = "requestKey";
    public static final String REQUEST_VALUE = "requestValue";

    public SetRequestAttributeActionlet() {
        super(I18N_BASE,
              new ParameterDefinition<>(REQUEST_KEY, new TextInput<>(new TextType()), 1),
              new ParameterDefinition<>(REQUEST_VALUE, new TextInput<>(new TextType()), 2));
    }

    @Override
    public void validateActionInstance(RuleAction actionInstance) throws InvalidActionInstanceException {
        Map<String, ParameterModel> params = actionInstance.getParameterMap();
        ParameterModel keyParam = Preconditions.checkNotNull(params.get(REQUEST_KEY), "SetRequestAttributeActionlet requires key parameter.");
        ParameterModel valueParam = Preconditions.checkNotNull(params.get(REQUEST_VALUE), "SetRequestAttributeActionlet requires Value parameter.");
        Preconditions.checkArgument(StringUtils.isNotBlank(keyParam.getValue()), "SetRequestAttributeActionlet requires valid key.");
    }

    @Override
    public void executeAction(HttpServletRequest request, HttpServletResponse response, Map<String, ParameterModel> params) {
        String key = params.get(REQUEST_KEY).getValue();
        String value = params.get(REQUEST_VALUE).getValue();
        request.setAttribute(key, value);
    }
}
