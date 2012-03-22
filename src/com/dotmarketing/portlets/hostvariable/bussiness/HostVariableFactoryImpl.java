package com.dotmarketing.portlets.hostvariable.bussiness;

import java.util.ArrayList;
import java.util.List;

import net.sf.hibernate.ObjectNotFoundException;

import org.apache.commons.beanutils.BeanUtils;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;
import com.dotmarketing.util.InodeUtils;

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
		
	
		List<HostVariable> hvars= getAllVariables();
		List<HostVariable> hvarsforid =new ArrayList<HostVariable>();

			for(HostVariable  hvar :  hvars) {
				 if(hvar.getHostId().equals(hostId)) {
					
					 hvarsforid.add(hvar);
				}
			}
			return hvarsforid;
		}
	
	
	
}
