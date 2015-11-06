package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.model.RuleActionParameter;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CountRequestsActionlet extends RuleActionlet {

    public CountRequestsActionlet() {
        super("EveryPageActionlet");
    }

    @Override
    public void executeAction(HttpServletRequest request, HttpServletResponse response, Map<String, RuleActionParameter> params) {

        String fireOn = params.get("fireOn").getValue();

        Integer count = (Integer)request.getServletContext().getAttribute(fireOn);

        if(count == null) {
            count = 0;
        }

        request.getServletContext().setAttribute(fireOn, ++count);
    }
}
