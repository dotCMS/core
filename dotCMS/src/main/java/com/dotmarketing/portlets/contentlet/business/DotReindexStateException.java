package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.business.DotStateException;

/**
 * Used for throwing reindex problems
 * @author Jason Tesser
 *
 */
public class DotReindexStateException extends DotStateException {

	private static final long serialVersionUID = 1L;

	/**
	 * Used for throwing reindex problems
	 * @param x
	 */
	public DotReindexStateException(String x) {
		super(x);
	}
	
	/**
	 * Used for throwing reindex problems
	 * @param x
	 * @param e
	 */
	public DotReindexStateException(String x, Exception e) {
		super(x, e);
	}
}
