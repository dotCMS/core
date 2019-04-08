package com.dotmarketing.business.portal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dotcms.business.CloseDBIfOpened;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;

import io.vavr.control.Try;

public class PortletAPIImpl implements PortletAPI {
    
    protected boolean hasPortletRights(User user, String pId) {
        boolean hasRights=false;
        try {
            for (Layout layout : APILocator.getLayoutAPI().loadLayoutsForUser(user)) {
                if(layout.getPortletIds().contains(pId)){
                   return true;
                }
            }
        }
        catch(Exception ex) {
            Logger.warn(this, "can't determine if user "+user.getUserId()+" has rights to portlet "+pId,ex);
            hasRights=false;
        }
        return hasRights;
    }
    
    public boolean hasUserAdminRights(User user) {
        return hasPortletRights(user,"users");
    }

	public boolean hasContainerManagerRights(User user) {
		return hasPortletRights(user,"containers");
	}

	public boolean hasTemplateManagerRights(User user) {
	    return hasPortletRights(user,"templates");
	}

	@CloseDBIfOpened
	public Portlet findPortlet(String portletId) {
		String companyId = CompanyUtils.getDefaultCompany().getCompanyId();
		try {
			return PortletManagerUtil.getPortletById(companyId, portletId);
		} catch (SystemException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	@CloseDBIfOpened
	public List<Portlet> findAllPortlets() throws SystemException {
		String companyId = CompanyUtils.getDefaultCompany().getCompanyId();

		List<Portlet>allPortlets = new ArrayList<>();
		allPortlets.addAll(PortletManagerUtil.getPortlets(companyId));
		Try.of(()->allPortlets.addAll(findPortletsInDb())).onFailure(e-> Logger.warn(PortletAPIImpl.class, e.getMessage()));
		return allPortlets;
	}

	public boolean canAddPortletToLayout(Portlet portlet) {
		String[] portlets = PropsUtil.getArray(PropsUtil.PORTLETS_EXCLUDED_FOR_LAYOUT);
		for(String portletId : portlets) {
			if(portletId.trim().equals(portlet.getPortletId()))
				return false;
		}
		return true;
	}

	public boolean canAddPortletToLayout(String portletId) {
		String[] attachablePortlets = PropsUtil.getArray(PropsUtil.PORTLETS_EXCLUDED_FOR_LAYOUT);
		for(String attachablePortlet : attachablePortlets) {
			if(attachablePortlet.trim().equals(portletId))
				return false;
		}
		return true;
	}

	
	
	private List<Portlet> findPortletsInDb() throws DotDataException{
	    String companyId = CompanyUtils.getDefaultCompany().getCompanyId();
        List<Map<String, Object>> ports = new DotConnect()
                .setSQL("select portletid ,groupid ,companyid , defaultpreferences, narrow,roles ,active_  from portlet where companyid=?")
                .addParam(companyId).loadObjectResults();
        final List<Portlet> portlets =
                ports.stream()
                        .map(m -> new Portlet(
                                (String) m.get("portletid"), 
                                (String) m.get("groupid"), 
                                (String) m.get("companyid"),
                                (String) m.get("defaultpreferences"), 
                                false, 
                                null, 
                                Boolean.parseBoolean((String) m.get("active_"))))
                        .collect(Collectors.toList());
        
        
	    return portlets;
	    
	    
	}
	
	
	
	
}
