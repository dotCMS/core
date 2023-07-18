package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotmarketing.portlets.rules.model.Rule;

/**
 * This wrapper class will contain all the information that a {@link com.dotcms.experiments.model.Experiment}
 * object requires for it to be pushed to another environment. This wrapper will
 * be added to the bundle file (usually in the form of an XML file) and will be
 * read by the handler class that will save this data in the destination server.
 */
public class ExperimentWrapper {

	private Experiment experiment;
	private Operation operation = null;

	public ExperimentWrapper() {
	}

	/**
	 * Builds a wrapper for the specified {@link Experiment} object.
	 *
	 * @param experiment
	 *            - The experiment that will be pushed.
	 */
	public ExperimentWrapper(Experiment experiment) {
		this.experiment = experiment;
	}

	/**
	 * Returns the {@link Rule} object containing all its required data:
	 * Condition Groups, Conditions, Actions, configuration parameters, etc.
	 * 
	 * @return The {@link Rule} object.
	 */
	public Experiment getExperiment() {
		return this.experiment;
	}

	/**
	 * Sets the {@link Experiment} object
	 * @param experiment
	 *            - The {@link Experiment} object.
	 */
	public void setExperiment(Experiment experiment) {
		this.experiment = experiment;
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
	 * Sets the push {@link Operation} set for this experiment.
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
		return "ExperimentWrapper [experiment=" + experiment + ", operation=" + operation + "]";
	}

}
