package com.dotmarketing.portlets.hostvariable.bussiness;

import com.google.common.annotations.VisibleForTesting;
import com.dotcms.util.transform.TransformerLocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.dotmarketing.util.InodeUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.net.sf.hibernate.ObjectNotFoundException;
import org.apache.commons.beanutils.BeanUtils;

public class HostVariableFactoryImpl extends HostVariableFactory{

	private static final String VARIABLE_NOT_IN_SITE = "Site Variable not found in the specified Site";

	protected void delete(HostVariable object) throws DotDataException {
	    
	    object = (HostVariable) HibernateUtil.load(HostVariable.class, object.getId());
	 
		HibernateUtil.delete(object);
		CacheLocator.getHostVariablesCache().clearVariablesForSite(object.getHostId());
	}

	public void deleteAllVariablesForSite(final String siteId) throws DotDataException {
		new DotConnect().setSQL("DELETE FROM host_variable WHERE host_id=?")
				.addParam(siteId)
				.loadResult();

		CacheLocator.getHostVariablesCache().clearVariablesForSite(siteId);
	}

	protected HostVariable find (String id) throws DotDataException {
		 HostVariable hvar= new HostVariable();
			try {
				hvar = (HostVariable) HibernateUtil.load(HostVariable.class, id);
			} catch (DotHibernateException e) { 
				if(!(e.getCause() instanceof ObjectNotFoundException))
					throw e; 
			}

		return hvar;
	}


	/**
	 * Saves a Site Variable. Without an id, the variable is inserted. With an id, the stored variable is updated, and
	 * only when it belongs to the Site the given variable names: a save never moves a variable to another Site.
	 *
	 * @param object The variable to save.
	 *
	 * @return The given variable.
	 *
	 * @throws DotDataException The id is not one of the given Site's variables, or the database operation failed.
	 */
	@Override
	protected HostVariable save(HostVariable object) throws DotDataException {
		String id = object.getId();
		
		if( InodeUtils.isSet(id)) {
			final HostVariable hvar = find(id);
			if (!InodeUtils.isSet(hvar.getId()) || !Objects.equals(hvar.getHostId(), object.getHostId())) {
				throw new DotDataException(VARIABLE_NOT_IN_SITE);
			}
			try
			{
				BeanUtils.copyProperties(hvar,object);
				HibernateUtil.saveOrUpdate(hvar);	
			}catch(Exception ex){
				throw new DotDataException(ex.getMessage(),ex);
			}
		}else{
			HibernateUtil.save(object);
		}

		CacheLocator.getHostVariablesCache().clearVariablesForSite(object.getHostId());

		return object;
	}
  
	protected List <HostVariable> getAllVariables() throws DotDataException {
		List <HostVariable> hostVariables = CacheLocator.getHostVariablesCache().getAll();
		if(hostVariables == null){
			HibernateUtil hu = new HibernateUtil(HostVariable.class);
			hu.setQuery ("from " + HostVariable.class.getName());
			hostVariables = hu.list();
			CacheLocator.getHostVariablesCache().put(hostVariables);
		}
		return hostVariables;
	}

	/**
	 * Returns the variables of a Site, ordered by key. The returned objects are copies of the cached ones, so a caller
	 * can change them without changing what later reads return.
	 *
	 * @param siteId The Site identifier.
	 *
	 * @return Copies of the Site's variables.
	 *
	 * @throws DotDataException The variables could not be read from the database.
	 */
	@Override
	@VisibleForTesting
	public List<HostVariable> getVariablesForHost(final String siteId) throws DotDataException {

		List<HostVariable> siteVariables = CacheLocator.getHostVariablesCache()
				.getVariablesForSite(siteId);
		if (siteVariables == null) {
			siteVariables = TransformerLocator.createHostVariableTransformer(
					new DotConnect().setSQL("SELECT * FROM host_variable "
									+ "WHERE host_id=? "
									+ "ORDER BY variable_key")
							.addParam(siteId)
							.loadObjectResults()
			).asList();

			CacheLocator.getHostVariablesCache().putVariablesForSite(siteId, siteVariables);
		}

		return siteVariables.stream().map(HostVariable::new).collect(Collectors.toList());
	}

	/**
	 * Updates the user_id of the host_variable table.
	 * This is called when an user is deleted.
	 *
	 * @param userToDelete UserId of the user that is going to be deleted
	 * @param userToReplace UserId of the user that is going to be replace it
	 * @throws DotDataException
	 */
    @Override
    public void updateUserReferences(final String userToDelete, final String userToReplace) throws DotDataException {

        new DotConnect().setSQL("update host_variable set user_id=?, last_mod_date=? where user_id=?")
            .addParam(userToReplace)
            .addParam(new Date())
            .addParam(userToDelete)
            .loadResult();

        CacheLocator.getHostVariablesCache().clearCache();
    }

	
}
