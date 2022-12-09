package com.dotcms.rest.api;

import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.rest.ErrorResponseHelper;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.UserProxyAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;

/**
 * This utility class is meant to hold common class references used by our REST Endpoints in order to decrease the
 * number of parameters being passed down to class constructors, as per SonarQube's feedback. This is particularly
 * useful when mocking API instances in Unit Tests.
 * <p>Please feel free to add more APIs and/or utility classes to this provider as necessary.</p>
 *
 * @author Jose Castro
 * @since Dec 9th, 2022
 */
public class DotRestInstanceProvider {

    private ErrorResponseHelper errorHelper;

    private HostAPI siteAPI;
    private HostWebAPI hostWebAPI;

    private LayoutAPI layoutAPI;
    private LoginServiceAPI loginService;

    private PermissionAPI permissionAPI;

    private RoleAPI roleAPI;

    private UserAPI userAPI;
    private UserProxyAPI userProxyAPI;
    private UserWebAPI userWebAPI;

    private WebResource webResource;

    public ErrorResponseHelper getErrorHelper() {
        return errorHelper;
    }

    public DotRestInstanceProvider setErrorHelper(final ErrorResponseHelper errorHelper) {
        this.errorHelper = errorHelper;
        return this;
    }

    public HostAPI getHostAPI() {
        return siteAPI;
    }

    public DotRestInstanceProvider setHostAPI(final HostAPI siteAPI) {
        this.siteAPI = siteAPI;
        return this;
    }

    public HostWebAPI getHostWebAPI() {
        return hostWebAPI;
    }

    public DotRestInstanceProvider setHostWebAPI(HostWebAPI hostWebAPI) {
        this.hostWebAPI = hostWebAPI;
        return this;
    }

    public LayoutAPI getLayoutAPI() {
        return layoutAPI;
    }

    public DotRestInstanceProvider setLayoutAPI(final LayoutAPI layoutAPI) {
        this.layoutAPI = layoutAPI;
        return this;
    }

    public LoginServiceAPI getLoginService() {
        return loginService;
    }

    public DotRestInstanceProvider setLoginService(final LoginServiceAPI loginService) {
        this.loginService = loginService;
        return this;
    }

    public PermissionAPI getPermissionAPI() {
        return permissionAPI;
    }

    public DotRestInstanceProvider setPermissionAPI(final PermissionAPI permissionAPI) {
        this.permissionAPI = permissionAPI;
        return this;
    }

    public RoleAPI getRoleAPI() {
        return roleAPI;
    }

    public DotRestInstanceProvider setRoleAPI(final RoleAPI roleAPI) {
        this.roleAPI = roleAPI;
        return this;
    }

    public UserAPI getUserAPI() {
        return userAPI;
    }

    public DotRestInstanceProvider setUserAPI(final UserAPI userAPI) {
        this.userAPI = userAPI;
        return this;
    }

    public UserProxyAPI getUserProxyAPI() {
        return userProxyAPI;
    }

    public DotRestInstanceProvider setUserProxyAPI(final UserProxyAPI userProxyAPI) {
        this.userProxyAPI = userProxyAPI;
        return this;
    }

    public UserWebAPI getUserWebAPI() {
        return userWebAPI;
    }

    public DotRestInstanceProvider setUserWebAPI(final UserWebAPI userWebAPI) {
        this.userWebAPI = userWebAPI;
        return this;
    }

    public WebResource getWebResource() {
        return webResource;
    }

    public DotRestInstanceProvider setWebResource(final WebResource webResource) {
        this.webResource = webResource;
        return this;
    }

}
