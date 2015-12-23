package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CountRequestsActionlet extends RuleActionlet<CountRequestsActionlet.Instance> {

    public CountRequestsActionlet() {
        super("Count Requests Actionlet");
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, CountRequestsActionlet.Instance instance) {
        Integer count = (Integer)request.getServletContext().getAttribute(instance.fireOn);
        if(count == null) {
            count = 0;
        }
        request.getServletContext().setAttribute(instance.fireOn, ++count);
        return true;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> values) {
        return new Instance(values);
    }

    static class Instance implements RuleComponentInstance {

        private final String fireOn;

        public Instance(Map<String, ParameterModel> values) {
            this.fireOn = values.get("fireOn").getValue();
        }
    }
}
