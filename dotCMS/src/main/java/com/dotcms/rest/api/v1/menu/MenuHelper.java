package com.dotcms.rest.api.v1.menu;

import com.dotcms.rest.BaseRestPortlet;
import com.dotcms.spring.portlet.PortletController;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.JSPPortlet;
import com.liferay.portlet.PortletURLImpl;
import com.liferay.portlet.StrutsPortlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper for the {@link MenuResource}
 * @author jsanca
 */
public class MenuHelper implements Serializable {

    public static final MenuHelper INSTANCE =
            new MenuHelper();

    private MenuHelper() {}

    /**
     * Get the list of menu portlet items for the specified menucontext
     * @param menuContext
     * @return a list of menu item associated to this context
     * @throws LanguageException
     * @throws ClassNotFoundException
     */
    public List<MenuItem> getMenuItems(MenuContext menuContext)
            throws LanguageException, ClassNotFoundException {

        List<MenuItem> menuItems = new ArrayList<>();
        List<String> portletIds = menuContext.getLayout().getPortletIds();

        for (String portletId : portletIds) {
            menuContext.setPortletId( portletId );
            String linkHREF = getUrl(menuContext);
            String linkName = LanguageUtil.get(menuContext.getUser(), "com.dotcms.repackage.javax.portlet.title." + portletId);
            boolean isAngular = isAngular( portletId );
            boolean isAjax = isAjax( portletId );

            menuItems.add ( new MenuItem(portletId, linkHREF, linkName, isAngular, isAjax) );
        }
        return menuItems;
    }

    /**
     * Validate if the portlet is a PortletController
     * @param portletId Id of the portlet
     * @return true if the portlet is a PortletController portlet, false if not
     * @throws ClassNotFoundException
     */
    public boolean isAngular(String portletId) throws ClassNotFoundException {
        Portlet portlet = APILocator.getPortletAPI().findPortlet(portletId);
        String portletClass = portlet.getPortletClass();
        Class classs = Class.forName( portletClass );
        return PortletController.class.isAssignableFrom( classs );
    }

    /**
     * Validate if the portlet is a BaseRestPortlet
     * @param portletId Id of the portlet
     * @return true if the portlet is a BaseRestPortlet portlet, false if not
     * @throws ClassNotFoundException
     */
    public boolean isAjax(String portletId) throws ClassNotFoundException {
        Portlet portlet = APILocator.getPortletAPI().findPortlet(portletId);
        String portletClass = portlet.getPortletClass();
        Class classs = Class.forName( portletClass );
        return BaseRestPortlet.class.isAssignableFrom( classs );
    }

    /**
     * Get the url of the menucontext
     * @param menuContext
     * @return the portlet url
     * @throws ClassNotFoundException
     */
    public String getUrl(MenuContext menuContext) throws ClassNotFoundException {
        Portlet portlet = APILocator.getPortletAPI().findPortlet( menuContext.getPortletId() );

        String portletClass = portlet.getPortletClass();
        Class classs = Class.forName( portletClass );
        MenuResource.App appFrom = menuContext.getAppFrom();

        Logger.debug(MenuResource.class,"### getPortletId" + menuContext.getPortletId());
        Logger.debug(MenuResource.class,"### portletClass" + portletClass);
        if(StrutsPortlet.class.isAssignableFrom( classs ) || JSPPortlet.class.isAssignableFrom( classs )) {
            PortletURLImpl portletURLImpl = new PortletURLImpl(menuContext.getHttpServletRequest(),
                    menuContext.getPortletId(), menuContext.getLayout().getId(), false);
            return portletURLImpl.toString() + "&dm_rlout=1&r=" + System.currentTimeMillis();
        }else if(BaseRestPortlet.class.isAssignableFrom( classs )) {
        	PortletURLImpl portletURLImpl = new PortletURLImpl(menuContext.getHttpServletRequest(),
                    menuContext.getPortletId(), menuContext.getLayout().getId(), false);
            return portletURLImpl.toString() + "&dm_rlout=1&r=" + System.currentTimeMillis()
            +"&"+WebKeys.AJAX_PORTLET+"=true";
        }else if(PortletController.class.isAssignableFrom( classs )){
            if (MenuResource.App.CORE.equals( appFrom )) {
                return "/spring/portlet/" + menuContext.getPortletId();
            }else{
                return "/html/ng/p/" + menuContext.getPortletId();
            }
        }

        return null;
    }

} // E:O:F:MenuHelper.
