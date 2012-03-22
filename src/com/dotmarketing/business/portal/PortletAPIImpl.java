package com.dotmarketing.business.portal;

import java.util.List;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PropsUtil;

public class PortletAPIImpl implements PortletAPI {

	public boolean hasContainerManagerRights(User user) {
		
		boolean hasContainerManagerRights = false;
		List<Layout> layouts;
		try {
			layouts = APILocator.getLayoutAPI().loadLayoutsForUser(user);
		} catch (DotDataException e) {
			Logger.error(PortletAPIImpl.class,e.getMessage(),e);
			return false;
		}
		for (Layout layout : layouts) {
			if(layout.getPortletIds().contains("EXT_12")){
				hasContainerManagerRights = true;
				break;
			}
		}
		return hasContainerManagerRights;
	}

	public boolean hasTemplateManagerRights(User user) {
		boolean hasTemplateManagerRights = false;
		List<Layout> layouts;
		try {
			layouts = APILocator.getLayoutAPI().loadLayoutsForUser(user);
		} catch (DotDataException e) {
			Logger.error(PortletAPIImpl.class,e.getMessage(),e);
			return false;
		}
		for (Layout layout : layouts) {
			if(layout.getPortletIds().contains("EXT_13")){
				hasTemplateManagerRights = true;
				break;
			}
		}		
		return hasTemplateManagerRights;
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
