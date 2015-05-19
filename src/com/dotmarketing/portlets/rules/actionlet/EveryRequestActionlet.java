package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import com.dotmarketing.util.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class EveryRequestActionlet extends RuleActionlet {
    @Override
    public String getName() {
        return "EveryRequestActionlet";
    }

    @Override
    public String getHowTo() {
        return null;
    }

    @Override
    public void executeAction(HttpServletRequest request, Map<String, RuleActionParameter> params) {
        Logger.info(this, "HTTP SERVLET REQUEST:" + request);
        Logger.info(this, "SERVLET CONTEXT : " + request.getServletContext());
          request.getServletContext().setAttribute("everyRequest", true);
    }
}
