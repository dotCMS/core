package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.model.RuleActionParameter;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Actionlet to add Key/Value to the Session.
 *
 * @author Oscar Arrieta
 * @version 1.0
 * @since 09-22-2015
 *
 */
public class SetSessionAttributeActionlet extends RuleActionlet{

    public SetSessionAttributeActionlet(){
        super(SetSessionAttributeActionlet.class.getSimpleName());
    }

    @Override
    public void executeAction(HttpServletRequest request, Map<String, RuleActionParameter> params) {

        String sessionKeyParam = params.get("sessionKey").getValue();
        String sessionValueParam = params.get("sessionValue").getValue();

        request.getSession().setAttribute(sessionKeyParam, sessionValueParam);
    }
}
