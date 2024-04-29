package com.dotmarketing.portlets.hostvariable.bussiness;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import java.util.List;

//This interface should have default package access
public abstract class HostVariablesCache implements Cachable {

	abstract protected List<HostVariable> put(List<HostVariable> variables);

	abstract protected List<HostVariable> getAll();

	abstract public void clearCache();

	/**
	 * Puts the list of host variables for a specific site into the cache.
	 *
	 * @param siteId the ID of the site
	 * @param list   the list of host variables to be stored
	 */
	protected abstract void putVariablesForSite(final String siteId, final List<HostVariable> list);

	/**
	 * Retrieves the list of HostVariables for a specific site.
	 *
	 * @param siteId the ID of the site
	 * @return a list of HostVariables for the specified site
	 */
	protected abstract List<HostVariable> getVariablesForSite(final String siteId);

	/**
	 * Clears the variables for a specific site.
	 *
	 * @param siteId the ID of the site
	 */
	protected abstract void clearVariablesForSite(final String siteId);

}