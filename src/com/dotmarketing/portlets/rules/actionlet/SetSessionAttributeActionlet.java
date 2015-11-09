package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

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

	private static final ActionletParameterWrapper[] PARAMS = new ActionletParameterWrapper[]{
			new ActionletParameterWrapper("sessionKey",ActionletParameterWrapper.DataType.TEXT),
			new ActionletParameterWrapper("sessionValue")};

    public SetSessionAttributeActionlet(){
        super(SetSessionAttributeActionlet.class.getSimpleName(), PARAMS);
    }

    @Override
    public void executeAction(HttpServletRequest request, HttpServletResponse response, Map<String, RuleActionParameter> params) {

        String sessionKeyParam = params.get("sessionKey").getValue();
        String sessionValueParam = params.get("sessionValue").getValue();

        if(UtilMethods.isSet(sessionKeyParam) && UtilMethods.isSet(sessionValueParam)){
            request.getSession().setAttribute(sessionKeyParam, sessionValueParam);
        } else {
            Logger.error(this.getClass(),
                    "Error trying to execute SetSessionAttributeActionlet, sessionKey or sessionValue are not set");
        }
    }
}
