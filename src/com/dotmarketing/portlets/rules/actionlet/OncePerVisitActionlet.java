package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.model.RuleActionParameter;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class OncePerVisitActionlet extends RuleActionlet {
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
        Integer count = (Integer) request.getServletContext().getAttribute("oncePerVisitCount");

        if(count==null)
            count = 1;

        request.getServletContext().setAttribute("oncePerVisitCount", count++);
    }
}
