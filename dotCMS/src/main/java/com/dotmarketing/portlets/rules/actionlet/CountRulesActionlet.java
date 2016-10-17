package com.dotmarketing.portlets.rules.actionlet;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This actionlet is used for testing purposes. If no "attribute"
 * parameter is given to the actionlet a "count" property will be
 * created instead.  This attribute is stored on the servlet context
 * and is available through all the application.
 */
public class CountRulesActionlet extends RuleActionlet<CountRulesActionlet.Instance> {

	public static final String ATTRIBUTE="count";

	public static final String PARAMETER_NAME="attribute";

    public CountRulesActionlet() {
        super("Rule's Counter");
    }

    @Override
    public boolean evaluate(HttpServletRequest request, HttpServletResponse response, CountRulesActionlet.Instance instance) {
        Integer count = (Integer)request.getServletContext().getAttribute(instance.attribute);
        if(count == null) {
            count = 0;
        }
        request.getServletContext().setAttribute(instance.attribute, ++count);
        return true;
    }

    @Override
    public Instance instanceFrom(Map<String, ParameterModel> values) {
        return new Instance(values);
    }

    static class Instance implements RuleComponentInstance {

    	private final String attribute;

        public Instance(Map<String, ParameterModel> values) {
        	this.attribute = (values.get(PARAMETER_NAME)!=null)?values.get(PARAMETER_NAME).getValue():ATTRIBUTE;
        }
    }
}
