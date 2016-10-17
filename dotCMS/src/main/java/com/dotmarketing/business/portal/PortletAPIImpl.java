package com.dotmarketing.business.portal;

import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;

public class PortletAPIImpl implements PortletAPI {
    
    protected boolean hasPortletRights(User user, String pId) {
        boolean hasRights=false;
        try {
            for (Layout layout : APILocator.getLayoutAPI().loadLayoutsForUser(user)) {
                if(layout.getPortletIds().contains(pId)){
                    hasRights = true;
                    break;
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
        return hasPortletRights(user,"EXT_USER_ADMIN");
    }

	public boolean hasContainerManagerRights(User user) {
		return hasPortletRights(user,"EXT_12");
	}

	public boolean hasTemplateManagerRights(User user) {
	    return hasPortletRights(user,"EXT_13");
	}

	public Portlet findPortlet(String portletId) {
		String companyId = CompanyUtils.getDefaultCompany().getCompanyId();
		try {
			return PortletManagerUtil.getPortletById(companyId, portletId);
		} catch (SystemException e) {
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public List<Portlet> findAllPortlets() throws SystemException {
		String companyId = CompanyUtils.getDefaultCompany().getCompanyId();
		return PortletManagerUtil.getPortlets(companyId);
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

}
