package com.dotmarketing.util;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import com.dotmarketing.business.Role;

public class MenuContextData implements Cloneable {

    private String                  portletId;
    private Role                    administratorRole;
    private Role                    reportAdministratorRole;
    private Role                    reportEditorRole;
    private Role                    cmsAdministratorRole;
    private Role                    campaignAdminRole;
    private Role                    campaignEditorRole;

    private List<Role>              userRoles;
    private Map<String, List<Role>> portletRolesMap;
    private HttpServletRequest      request;
    private PageContext             pageContext;
    private String                  layoutId;
    private int                     menuCounter;
    private String                  parentMenuId;

    public MenuContextData() {
    }

    public MenuContextData(String portletId, Role administratorRole, Role reportAdministratorRole, Role reportEditorRole, Role cmsAdministratorRole,
            Role campaignAdminRole, Role campaignEditorRole, List<Role> userRoles, Map<String, List<Role>> portletRolesMap,
            HttpServletRequest request, PageContext pageContext, String layoutId, int menuCounter, String parentMenuId) {
        this.portletId = portletId;
        this.administratorRole = administratorRole;
        this.reportAdministratorRole = reportAdministratorRole;
        this.reportEditorRole = reportEditorRole;
        this.cmsAdministratorRole = cmsAdministratorRole;
        this.campaignAdminRole = campaignAdminRole;
        this.campaignEditorRole = campaignEditorRole;

        this.userRoles = userRoles;
        this.portletRolesMap = portletRolesMap;
        this.request = request;
        this.pageContext = pageContext;
        this.layoutId = layoutId;
        this.menuCounter = menuCounter;
        this.parentMenuId = parentMenuId;
    }

    @Override
    public Object clone() {
        return new MenuContextData(this.portletId, this.administratorRole, this.reportAdministratorRole, this.reportEditorRole,
                this.cmsAdministratorRole, this.campaignAdminRole, this.campaignEditorRole, this.userRoles, this.portletRolesMap, this.request,
                this.pageContext, this.layoutId, this.menuCounter, this.parentMenuId);

    }

	/**
	 * @return the portletId
	 */
	public String getPortletId() {
		return portletId;
	}

	/**
	 * @param portletId the portletId to set
	 */
	public void setPortletId(String portletId) {
		this.portletId = portletId;
	}

	/**
	 * @return the administratorRole
	 */
	public Role getAdministratorRole() {
		return administratorRole;
	}

	/**
	 * @return the reportAdministratorRole
	 */
	public Role getReportAdministratorRole() {
		return reportAdministratorRole;
	}

	/**
	 * @return the reportEditorRole
	 */
	public Role getReportEditorRole() {
		return reportEditorRole;
	}

	/**
	 * @return the cmsAdministratorRole
	 */
	public Role getCmsAdministratorRole() {
		return cmsAdministratorRole;
	}

	/**
	 * @return the campaignAdminRole
	 */
	public Role getCampaignAdminRole() {
		return campaignAdminRole;
	}

	/**
	 * @return the campaignEditorRole
	 */
	public Role getCampaignEditorRole() {
		return campaignEditorRole;
	}

	/**
	 * @return the userRoles
	 */
	public List<Role> getUserRoles() {
		return userRoles;
	}

	/**
	 * @return the portletRolesMap
	 */
	public Map<String, List<Role>> getPortletRolesMap() {
		return portletRolesMap;
	}

	/**
	 * @return the request
	 */
	public HttpServletRequest getRequest() {
		return request;
	}

	/**
	 * @return the pageContext
	 */
	public PageContext getPageContext() {
		return pageContext;
	}

	/**
	 * @return the layoutId
	 */
	public String getLayoutId() {
		return layoutId;
	}

	/**
	 * @return the menuCounter
	 */
	public int getMenuCounter() {
		return menuCounter;
	}

	/**
	 * @param menuCounter the menuCounter to set
	 */
	public void setMenuCounter(int menuCounter) {
		this.menuCounter = menuCounter;
	}

	/**
	 * @return the parentMenuId
	 */
	public String getParentMenuId() {
		return parentMenuId;
	}

	/**
	 * @param parentMenuId the parentMenuId to set
	 */
	public void setParentMenuId(String parentMenuId) {
		this.parentMenuId = parentMenuId;
	}
}
