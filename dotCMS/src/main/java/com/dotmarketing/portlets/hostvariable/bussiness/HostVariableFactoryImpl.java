package com.dotmarketing.portlets.hostvariable.bussiness;

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

import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.net.sf.hibernate.ObjectNotFoundException;
import org.apache.commons.beanutils.BeanUtils;

public class HostVariableFactoryImpl extends HostVariableFactory{

	protected void delete(HostVariable object) throws DotDataException {
	    
	    object = (HostVariable) HibernateUtil.load(HostVariable.class, object.getId());
	 
		HibernateUtil.delete(object);
		CacheLocator.getHostVariablesCache().clearCache();
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



	protected void save(HostVariable object) throws DotDataException {
		String id = object.getId();
		
		if( InodeUtils.isSet(id)) {
			try
			{
				HostVariable hvar = (HostVariable) HibernateUtil.load(HostVariable.class, id);
				BeanUtils.copyProperties(hvar,object);
				HibernateUtil.saveOrUpdate(hvar);	
			}catch(Exception ex){
				throw new DotDataException(ex.getMessage(),ex);
			}
		}else{
			HibernateUtil.save(object);
		}
		CacheLocator.getHostVariablesCache().clearCache();
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
	

	protected List<HostVariable> getVariablesForHost (String hostId ) throws DotDataException {
		return TransformerLocator.createHostVariableTransformer(
				new DotConnect().setSQL("SELECT * FROM host_variable WHERE host_id=?")
				.addParam(hostId)
				.loadObjectResults()
		).asList();
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
