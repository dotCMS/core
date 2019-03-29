package com.dotmarketing.business.portal;

import java.util.Collection;
import java.util.List;

import com.dotcms.business.CloseDBIfOpened;
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

    String companyId = CompanyUtils.getDefaultCompany().getCompanyId();

    protected boolean hasPortletRights(User user, String pId) {
        boolean hasRights = false;
        try {
            for (Layout layout : APILocator.getLayoutAPI().loadLayoutsForUser(user)) {
                if (layout.getPortletIds().contains(pId)) {
                    return true;
                }
            }
        } catch (Exception ex) {
            Logger.warn(this, "can't determine if user " + user.getUserId() + " has rights to portlet " + pId, ex);
            hasRights = false;
        }
        return hasRights;
    }

    public boolean hasUserAdminRights(User user) {
        return hasPortletRights(user, "users");
    }

    public boolean hasContainerManagerRights(User user) {
        return hasPortletRights(user, "containers");
    }

    public boolean hasTemplateManagerRights(User user) {
        return hasPortletRights(user, "templates");
    }

    @CloseDBIfOpened
    public Portlet findPortlet(String portletId) {

        try {
            return PortletManagerUtil.getPortletById(companyId, portletId);
        } catch (SystemException e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }

    @CloseDBIfOpened
    public Portlet deletePortlet(String portletId) {

        try {
            return PortletManagerUtil.getPortletById(companyId, portletId);
        } catch (SystemException e) {
            throw new DotRuntimeException(e.getMessage(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    @CloseDBIfOpened
    public void InitPortlets() throws SystemException {
        PortletManagerUtil.getPortlets(companyId);
    }
    
    
    @SuppressWarnings("unchecked")
    @CloseDBIfOpened
    public Collection<Portlet> findAllPortlets() throws SystemException {
        return PortletManagerUtil.getPortlets(companyId);
    }

    public boolean canAddPortletToLayout(Portlet portlet) {
        String[] portlets = PropsUtil.getArray(PropsUtil.PORTLETS_EXCLUDED_FOR_LAYOUT);
        for (String portletId : portlets) {
            if (portletId.trim().equals(portlet.getPortletId()))
                return false;
        }
        return true;
    }

    public boolean canAddPortletToLayout(String portletId) {
        String[] attachablePortlets = PropsUtil.getArray(PropsUtil.PORTLETS_EXCLUDED_FOR_LAYOUT);
        for (String attachablePortlet : attachablePortlets) {
            if (attachablePortlet.trim().equals(portletId))
                return false;
        }
        return true;
    }

}
