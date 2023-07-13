package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.publishing.PublisherConfig.Operation;
import com.dotcms.variant.model.Variant;

/**
 * This wrapper class will contain all the information that a {@link com.dotcms.variant.model.Variant}
 * object requires for it to be pushed to another environment. This wrapper will
 * be added to the bundle file (usually in the form of an XML file) and will be
 * read by the handler class that will save this data in the destination server.
 */
public class VariantWrapper {

	private Variant variant;
	private Operation operation = null;

	public VariantWrapper() {
	}

	/**
	 * Builds a wrapper for the specified {@link Variant} object.
	 *
	 * @param variant
	 *            - The variant that will be pushed.
	 */
	public VariantWrapper(Variant variant) {
		this.variant = variant;
	}

	/**
	 * Returns the {@link Variant} object
	 * 
	 * @return The {@link Variant} object.
	 */
	public Variant getVariant() {
		return this.variant;
	}

	/**
	 * Sets the {@link Variant} object.
	 * 
	 * @param variant
	 *            - The {@link Variant} object.
	 */
	public void setVariant(Variant variant) {
		this.variant = variant;
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
		return "VariantWrapper [variant=" + variant + ", operation=" + operation + "]";
	}

}
