package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.business.DotStateException;

/**
 * Used for throwing contentlet problems
 * @author David Torres
 * @author Jason Tesser
 * @since 1.6
 */
public class DotContentletStateException extends DotStateException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Used for throwing contentlet problems
	 * @param x
	 */
	public DotContentletStateException(String x) {
		super(x);
	}
	
	/**
	 * Used for throwing contentlet problems
	 * @param x
	 * @param e
	 */
	public DotContentletStateException(String x, Exception e) {
		super(x, e);
	}
}
