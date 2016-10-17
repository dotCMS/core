package com.dotcms.rest.api.v1.menu;

import com.dotmarketing.business.Layout;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by freddyrodriguez on 18/5/16.
 */
public class MenuContext {

    private final User user;
    private final MenuResource.App appFrom;
    private HttpServletRequest httpServletRequest;
    private Layout layout;
    private String portletId;
    private int layoutIndex;

    /**
     * Generate the Menu Context for the MenuResource
     * @param httpServletRequest httpservlet resquest
     * @param user User initializing this context
     * @param appFrom app initializinf this context
     */
    public MenuContext(HttpServletRequest httpServletRequest, User user, MenuResource.App appFrom) {
        this.httpServletRequest = httpServletRequest;
        this.user = user;
        this.appFrom = appFrom;
    }

    /**
     * Get the httpservlet request of this context
     * @return HttpServletRequest
     */
    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    /**
     * Set the index layout
     * @param layoutIndex
     */
    public void setLayoutIndex(int layoutIndex) {
        this.layoutIndex = layoutIndex;
    }

    /**
     * get the index layout
     * @return index layout
     */
    public int getLayoutIndex() {
        return layoutIndex;
    }
    
    /**
     * Set the layout in the context
     * @param layout
     */
    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    /**
     * Get the context layout
     * @return layout
     */
    public Layout getLayout() {
        return layout;
    }
    
    /**
     * Set the portlet id in the context
     * @param portletId
     */
    public void setPortletId(String portletId) {
        this.portletId = portletId;
    }

    /**
     * Get the portlet Id from the context
     * @return the portlet id
     */
    public String getPortletId() {
        return portletId;
    }
    
    /**
     * Get the user calling the app from context
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * Get the Me
     * @return
     */
    public MenuResource.App getAppFrom() {
        return appFrom;
    }
}
