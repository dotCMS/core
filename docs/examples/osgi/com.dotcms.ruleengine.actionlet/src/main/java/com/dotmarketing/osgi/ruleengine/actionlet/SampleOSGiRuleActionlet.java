package com.dotmarketing.osgi.ruleengine.actionlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.portlets.rules.actionlet.ActionletParameterWrapper;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class SampleOSGiRuleActionlet extends RuleActionlet{

	private static final ActionletParameterWrapper[] PARAMS = new ActionletParameterWrapper[]{
			new ActionletParameterWrapper("URL")};

    public SampleOSGiRuleActionlet(){
        super("Sample OSGi Rule Action", PARAMS);
    }

    @Override
    public void executeAction(HttpServletRequest request, HttpServletResponse response, Map<String, RuleActionParameter> params) {

        String URLKeyParam = params.get("URL").getValue();

        if(UtilMethods.isSet(URLKeyParam)){
            try {
				response.sendRedirect(URLKeyParam);
			} catch (IOException e) {
				Logger.error(this.getClass(),e.toString());
			}
        } else {
            Logger.error(this.getClass(),
                    "Error trying to execute SampleOSGiRuleActionlet, URL is not set");
        }
    }
}
