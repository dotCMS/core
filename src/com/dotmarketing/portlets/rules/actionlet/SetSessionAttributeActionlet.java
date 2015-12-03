package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.portlets.rules.actionlet.ActionParameterDefinition.DataType;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * Actionlet to add Key/Value to the Session.
 * The exact names that had to be set in params are: sessionKey and sessionValue.
 *
 * @author Oscar Arrieta
 * @version 1.0
 * @since 09-22-2015
 *
 */
public class SetSessionAttributeActionlet extends RuleActionlet{

    private static final String I18N_BASE = "api.system.ruleengine.actionlet.SetSessionAttribute";

    private static final String SESSION_KEY = "sessionKey";
    private static final String SESSION_VALUE = "sessionValue";
    private static final List<ActionParameterDefinition> PARAMS = ImmutableList.of(
			new ActionParameterDefinition(SESSION_KEY, DataType.TEXT),
			new ActionParameterDefinition(SESSION_VALUE)
    );

    public SetSessionAttributeActionlet(){
        super(I18N_BASE, PARAMS);
    }

    @Override
    public void validateActionInstance(RuleAction actionInstance) throws InvalidActionInstanceException {
        Map<String, RuleActionParameter> params = actionInstance.getParameterMap();
        RuleActionParameter keyParam = Preconditions.checkNotNull(params.get(SESSION_KEY), "SetSessionAttributeActionlet requires sessionKey parameter.");
        RuleActionParameter valueParam = Preconditions.checkNotNull(params.get(SESSION_VALUE), "SetSessionAttributeActionlet requires sessionValue parameter.");
        Preconditions.checkArgument(StringUtils.isNotBlank(keyParam.getValue()), "SetSessionAttributeActionlet requires valid sessionKey.");
    }

    @Override
    public void executeAction(HttpServletRequest request, HttpServletResponse response, Map<String, RuleActionParameter> params) {
        String key = params.get(SESSION_KEY).getValue();
        String value = params.get(SESSION_VALUE).getValue();
        if(value == null){
            request.getSession().removeAttribute(key);
        } else{
            request.getSession().setAttribute(key, value);

        }
    }


}
