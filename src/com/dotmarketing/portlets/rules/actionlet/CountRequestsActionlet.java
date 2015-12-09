package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CountRequestsActionlet extends RuleActionlet {

    public CountRequestsActionlet() {
        super("EveryPageActionlet");
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, RuleComponentInstance instance) {
        //        String fireOn = params.get("fireOn").getValue();
        //
        //        Integer count = (Integer)request.getServletContext().getAttribute(fireOn);
        //
        //        if(count == null) {
        //            count = 0;
        //        }
        //
        //        request.getServletContext().setAttribute(fireOn, ++count);
        return true;
    }

    @Override
    public Instance instanceFrom(Map values) {
        return new Instance();
    }

    static class Instance implements RuleComponentInstance {

        public void checkValid() {

        }
    }
}
