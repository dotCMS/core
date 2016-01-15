package com.dotmarketing.osgi.ruleengine.actionlet;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotmarketing.portlets.rules.RuleComponentInstance;
import com.dotmarketing.portlets.rules.actionlet.RuleActionlet;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import com.dotmarketing.portlets.rules.parameter.ParameterDefinition;
import com.dotmarketing.portlets.rules.parameter.display.TextInput;
import com.dotmarketing.portlets.rules.parameter.type.TextType;
import com.dotmarketing.util.Logger;

/**
 * This actionlet add sleeping time in milliseconds to show a page
 *
 * @author Oswaldo Gallango
 * @version 1.0
 * @since 01-14-2016
 */
public class SlowRuleActionlet extends RuleActionlet<SlowRuleActionlet.Instance> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String I18N_BASE = "com.dotmarketing.osgi.ruleengine.actionlet.slow_rule_execution";
	private static final String INPUT_WAIT_IN_MILLISECONDS = "Time";


	public SlowRuleActionlet() {
		super(I18N_BASE, new ParameterDefinition<>(1, INPUT_WAIT_IN_MILLISECONDS, new TextInput<>(new TextType())));
	}

	public boolean evaluate(HttpServletRequest request, HttpServletResponse response, Instance instance) {
		try {
			//sleep to throw the too slow warning message
			Thread.sleep(Integer.parseInt(instance.timeToWait));			
			return true;
		} catch (InterruptedException e) {
			Logger.error(SlowRuleActionlet.class, "Error executing Slow Rule Actionlet. Time in milliseconds is required", e);
		}
		return false;
	}

	public Instance instanceFrom(Map<String, ParameterModel> parameters) {
		return new Instance(parameters);
	}

	static class Instance implements RuleComponentInstance {

		private final String timeToWait;

		public Instance(Map<String, ParameterModel> parameters) {
			timeToWait = parameters.get(INPUT_WAIT_IN_MILLISECONDS).getValue();
		}
	}
}
