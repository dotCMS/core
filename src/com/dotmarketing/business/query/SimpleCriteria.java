package com.dotmarketing.business.query;

import com.dotmarketing.business.query.GenericQueryFactory.Operator;

public interface SimpleCriteria extends Criteria {

	/**
	 * @return the attribute (the left side)
	 */
	public String getAttribute();
	
	/**
	 * @return the operator
	 */
	public Operator getOperator();

	/**
	 * @return the value (the right side)
	 */
	public Object getValue();
	
}
