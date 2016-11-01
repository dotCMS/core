package com.dotmarketing.business.query;

import java.util.List;

public interface ComplexCriteria extends Criteria {

	/**
	 * Will return the operator proceeding the passed in Criteria
	 * @param criteria
	 * @return returns Null if there is none
	 */
	public String getPreceedingOperator(Criteria criteria);
	
	/**
	 * returns the criteria within the complex grouping
	 */
	public List<Criteria> getCriteria(); 
	
	/**
	 * Is this complex grouping negated.  ie.. !(some criteria)
	 * @return
	 */
	public boolean isNegate();
}
