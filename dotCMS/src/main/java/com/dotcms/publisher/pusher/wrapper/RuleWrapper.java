package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotmarketing.portlets.rules.model.Rule;

/**
 * This wrapper class will contain all the information that a {@link Rule}
 * object requires for it to be pushed to another environment. This wrapper will
 * be added to the bundle file (usually in the form of an XML file) and will be
 * read by the handler class that will save this data in the destination server.
 * 
 * @author Jose Castro
 * @version 1.0
 * @since Mar 8, 2016
 *
 */
public class RuleWrapper {

	private Rule rule = null;
	private Operation operation = null;

	/**
	 * Builds a wrapper for the specified {@link Rule} object.
	 * 
	 * @param rule
	 *            - The rule that will be pushed.
	 */
	public RuleWrapper(Rule rule) {
		this.rule = rule;
	}

	/**
	 * Returns the {@link Rule} object containing all its required data:
	 * Condition Groups, Conditions, Actions, configuration parameters, etc.
	 * 
	 * @return The {@link Rule} object.
	 */
	public Rule getRule() {
		return this.rule;
	}

	/**
	 * Sets the {@link Rule} object containing all its required data: Condition
	 * Groups, Conditions, Actions, configuration parameters, etc.
	 * 
	 * @param rule
	 *            - The {@link Rule} object.
	 */
	public void setRule(Rule rule) {
		this.rule = rule;
	}

	/**
	 * Returns the push {@link Operation} set for this rule:
	 * {@link Operation#PUBLISH}, or {@link Operation#UNPUBLISH}.
	 * 
	 * @return The push operation.
	 */
	public Operation getOperation() {
		return operation;
	}

	/**
	 * Sets the push {@link Operation} set for this rule.
	 * 
	 * @param operation
	 *            - The push operation: {@link Operation#PUBLISH}, or
	 *            {@link Operation#UNPUBLISH}.
	 */
	public void setOperation(Operation operation) {
		this.operation = operation;
	}

	@Override
	public String toString() {
		return "RuleWrapper [rule=" + rule + ", operation=" + operation + "]";
	}

}
