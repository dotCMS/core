package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleActionParameter;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class CountRequestsActionlet extends RuleActionlet {
    @Override
    public String getName() {
        return "EveryPageActionlet";
    }

    @Override
    public String getHowTo() {
        return null;
    }

    @Override
    public void executeAction(HttpServletRequest request, Map<String, RuleActionParameter> params) {
        request.getServletContext().setAttribute(Rule.FireOn.EVERY_PAGE.name(), true);
    }
}
