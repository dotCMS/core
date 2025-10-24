package com.dotcms.rest.api.v1.menu;

import com.dotcms.rest.BaseRestPortlet;
import com.dotcms.spring.portlet.PortletController;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.JSPPortlet;
import com.liferay.portlet.PortletURLImpl;
import com.liferay.portlet.StrutsPortlet;
import com.liferay.portlet.VelocityPortlet;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Helper for the {@link MenuResource}
 * @author jsanca
 */
public class MenuHelper implements Serializable {

    public static final MenuHelper INSTANCE =
            new MenuHelper();

    private static final String PORTLET_DOESNT_EXIST_ERROR_MSG = "Portlet ID '%s' does not exist";
    private static final String PORTLET_KEY_PREFIX="com.dotcms.repackage.javax.portlet.title.";
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
            String url = getUrl(menuContext);
            Locale locale = Try.of(()-> new Locale(menuContext.getHttpServletRequest().getSession().getAttribute("com.dotcms.repackage.org.apache.struts.action.LOCALE").toString())).getOrElse(Locale.US);
            String linkName = normalizeLinkName(LanguageUtil.get(locale, PORTLET_KEY_PREFIX + portletId));
            boolean isAngular = isAngular( portletId );
            boolean isAjax = isAjax( portletId );

            menuItems.add ( new MenuItem(portletId, url, linkName, isAngular, isAjax) );
        }
        return menuItems;
    }


    String normalizeLinkName(String linkName) {
        if(UtilMethods.isEmpty(linkName)){
            return "ukn";
        }
        else if (!linkName.startsWith(PORTLET_KEY_PREFIX)) {
            return linkName;
        }
        linkName = linkName.replace(PORTLET_KEY_PREFIX, "");

        if (linkName.startsWith("c_")) {
            linkName = linkName.substring(2);
        }

        linkName = linkName.replace("_", " ");

        return UtilMethods.capitalize(linkName);

    }


    /**
     * Determines if the portlet is an Angular-based portlet by checking if it implements PortletController
     *
     * @param portletId ID of the portlet to check
     * @return true if the portlet is an Angular portlet, false if not
     * @throws ClassNotFoundException if the portlet class cannot be found
     */
    public boolean isAngular(final String portletId) throws ClassNotFoundException {
        final Portlet portlet = APILocator.getPortletAPI().findPortlet(portletId);
        if (null != portlet) {
            final String portletClass = portlet.getPortletClass();
            final Class<?> classs = Class.forName(portletClass);
            return PortletController.class.isAssignableFrom(classs);
        } else {
            Logger.error(this, String.format(PORTLET_DOESNT_EXIST_ERROR_MSG, portletId));
        }
        return false;
    }

    /**
     * Validate if the portlet is a BaseRestPortlet
     * @param portletId Id of the portlet
     * @return true if the portlet is a BaseRestPortlet portlet, false if not
     * @throws ClassNotFoundException
     */
    public boolean isAjax(final String portletId) throws ClassNotFoundException {
        final Portlet portlet = APILocator.getPortletAPI().findPortlet(portletId);
        if (null != portlet) {
            final String portletClass = portlet.getPortletClass();
            final Class<?> classs = Class.forName(portletClass);
            return BaseRestPortlet.class.isAssignableFrom(classs);
        } else {
            Logger.error(this, String.format(PORTLET_DOESNT_EXIST_ERROR_MSG, portletId));
        }

        return false;
    }

    /**
     * Get the url of the menucontext
     * @param menuContext
     * @return the portlet url
     * @throws ClassNotFoundException
     */
    public String getUrl(final MenuContext menuContext) throws ClassNotFoundException {
        final Portlet portlet = APILocator.getPortletAPI().findPortlet( menuContext.getPortletId() );

        if (null != portlet) {
            final String portletClass = portlet.getPortletClass();
            final Class<?> classs = Class.forName(portletClass);

            Logger.debug(MenuResource.class, "### getPortletId" + menuContext.getPortletId());
            Logger.debug(MenuResource.class, "### portletClass" + portletClass);

            if(UtilMethods.isSet(portlet.getPortletUrl())){
                final String portletUrl = portlet.getPortletUrl();
                Logger.debug(MenuResource.class, "### portletUrl" + portletUrl);
                return portletUrl.startsWith(StringPool.FORWARD_SLASH) ? portletUrl : StringPool.FORWARD_SLASH + portletUrl;
            }

            final PortletURLImpl portletURLImpl = new PortletURLImpl(menuContext.getHttpServletRequest(),
                    menuContext.getPortletId(), menuContext.getLayout().getId(), false);
            if ( StrutsPortlet.class.isAssignableFrom(classs)
                    || JSPPortlet.class.isAssignableFrom(classs)
                    || VelocityPortlet.class.isAssignableFrom(classs) ) {
                return portletURLImpl + "&dm_rlout=1&r=" + System.currentTimeMillis();
            } else if (BaseRestPortlet.class.isAssignableFrom(classs)) {
                return portletURLImpl + "&dm_rlout=1&r=" + System.currentTimeMillis() + "&" + WebKeys.AJAX_PORTLET + "=true";
            } else if (PortletController.class.isAssignableFrom(classs)) {
                return StringPool.FORWARD_SLASH + menuContext.getPortletId();
            }
        } else {
            Logger.error(this, String.format(PORTLET_DOESNT_EXIST_ERROR_MSG,
                    menuContext.getPortletId()));
        }

        return null;
    }

} // E:O:F:MenuHelper.
