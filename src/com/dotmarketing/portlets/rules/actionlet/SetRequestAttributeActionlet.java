package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.portlets.rules.actionlet.ActionParameterDefinition.DataType;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Actionlet to add Key/Value to the Request.
 * The exact names that had to be set in params are: requestKey and requestValue.
 *
 */
public class SetRequestAttributeActionlet extends RuleActionlet{

    private static final String I18N_BASE = "api.system.ruleengine.actionlet.SetRequestAttribute";

    public static final String REQUEST_KEY = "requestKey";
    public static final String REQUEST_VALUE = "requestValue";

    private static final List<ActionParameterDefinition> PARAMS = ImmutableList.of(
			new ActionParameterDefinition(REQUEST_KEY, DataType.TEXT),
			new ActionParameterDefinition(REQUEST_VALUE)
    );

    public SetRequestAttributeActionlet(){
        super(I18N_BASE, PARAMS);
    }

    @Override
    public void validateActionInstance(RuleAction actionInstance) throws InvalidActionInstanceException {
        Map<String, RuleActionParameter> params = actionInstance.getParameterMap();
        RuleActionParameter keyParam = Preconditions.checkNotNull(params.get(REQUEST_KEY), "SetRequestAttributeActionlet requires key parameter.");
        RuleActionParameter valueParam = Preconditions.checkNotNull(params.get(REQUEST_VALUE), "SetRequestAttributeActionlet requires Value parameter.");
        Preconditions.checkArgument(StringUtils.isNotBlank(keyParam.getValue()), "SetRequestAttributeActionlet requires valid key.");
    }

    @Override
    public void executeAction(HttpServletRequest request, HttpServletResponse response, Map<String, RuleActionParameter> params) {
        String key = params.get(REQUEST_KEY).getValue();
        String value = params.get(REQUEST_VALUE).getValue();
        request.setAttribute(key, value);
    }


}
