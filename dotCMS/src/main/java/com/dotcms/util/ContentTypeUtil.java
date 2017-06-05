package com.dotcms.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.portlet.PortletURL;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.rest.api.v1.contenttype.ContentTypeResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portlet.PortletURLImpl;
import com.liferay.util.LocaleUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * Utility class for the {@link ContentTypeResource} end-point and other Content
 * Type related classes throughout the system.
 * 
 * @author Freddy Rodriguez
 * @version 3.7
 * @since Oct 3, 2016
 */
public class ContentTypeUtil {

    private final LayoutAPI layoutAPI;
    private final LanguageAPI languageAPI;
    private final HttpServletRequestThreadLocal httpServletRequestThreadLocal;
    private final LoginServiceAPI loginService;

    /**
     * Holder class for holding a unique instance.
     *
     */
    private static class SingletonHolder {
        private static final ContentTypeUtil INSTANCE = new ContentTypeUtil();
    }

    /**
     * Returns a singleton instance of this class.
     * 
     * @return An instance of ContentTypeUtil.
     */
    public static ContentTypeUtil getInstance() {
        return ContentTypeUtil.SingletonHolder.INSTANCE;
    }

    /**
     * Private class constructor
     */
	private ContentTypeUtil() {
		this(APILocator.getLayoutAPI(), APILocator.getLanguageAPI(), HttpServletRequestThreadLocal.INSTANCE,
				APILocator.getLoginServiceAPI());
	}

    @VisibleForTesting
    public ContentTypeUtil(final LayoutAPI layoutAPI,
                           final LanguageAPI languageAPI,
                           final HttpServletRequestThreadLocal httpServletRequestThreadLocal,
                           final LoginServiceAPI loginService){
        this.layoutAPI = layoutAPI;
        this.languageAPI = languageAPI;
        this.httpServletRequestThreadLocal = httpServletRequestThreadLocal;
        this.loginService = loginService;
    }

	/**
	 * Returns the action URL for the specified Content Type. Valid layouts must
	 * be returned by the {@link User} requesting this data; otherwise, the URL
	 * will not be returned.
	 * 
	 * @param contentType
	 *            - The Content Type whose action URL will be returned.
	 * @return The action URL associated to the specified Content Type.
	 */
    public String getActionUrl(final ContentType contentType) {
        final HttpServletRequest request = httpServletRequestThreadLocal.getRequest();
        final User user = loginService.getLoggedInUser(request);
        // It is ok not to have a logged in user all the time as this can be called by a plugin or by a Unit test
        if (null == user) {
            Logger.debug(this, "No Logged in User found when calling ContentTypeUtil.getActionUrl");
            return null;
        }
        return getActionUrl(request, contentType, user);
    }

	/**
	 * Returns the action URL for the specified Content Type. Valid layouts must
	 * be returned by the {@link User} requesting this data; otherwise, the URL
	 * will not be returned.
	 * 
	 * @param contentType
	 *            - The Content Type whose action URL will be returned.
	 * @param user
	 *            - The user performing this action.
	 * @return The action URL associated to the specified Content Type.
	 */
    public String getActionUrl(final ContentType contentType, final User user) {
      final HttpServletRequest request = httpServletRequestThreadLocal.getRequest();
      
      String actionUrl = request != null? getActionUrl(request, contentType, user):null;
      return actionUrl;
    }

	/**
	 * Returns the action URL for the specified Content Type. Valid layouts must
	 * be returned by the {@link User} requesting this data; otherwise, the URL
	 * will not be returned.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @param contentType
	 *            - The Content Type whose action URL will be returned.
	 * @param user
	 *            - The user performing this action.
	 * @return The action URL associated to the specified Content Type.
	 */
    public String getActionUrl( HttpServletRequest request, final ContentType contentType, final User user) {
        return getActionUrl(request, contentType.inode(), user);
    }

    /**
	 * Returns the action URL for the specified Content Type. Valid layouts must
	 * be returned by the {@link User} requesting this data; otherwise, the URL
	 * will not be returned. This means that a layout must contain at least one
	 * portlet.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @param contentTypeInode
	 *            - The Inode of the Content Type whose action URL will be
	 *            returned.
	 * @param user
	 *            - The user performing this action.
	 * @return The action URL associated to the specified Content Type.
	 */
    public String getActionUrl( HttpServletRequest request, final String contentTypeInode, final User user) {
        final List<Layout> layouts;
        String actionUrl = StringUtils.EMPTY;
        try {
            layouts = this.layoutAPI.loadLayoutsForUser(user);
            if (0 != layouts.size()) {
                final Layout layout = layouts.get(0);
                final List<String> portletIds = layout.getPortletIds();
                if (0 != portletIds.size()) {
                	final String portletName = portletIds.get(0);
                	final PortletURL portletURL = new PortletURLImpl(request, portletName, layout.getId(), true);
                	portletURL.setWindowState(WindowState.MAXIMIZED);
	                portletURL.setParameters(map(
                        "struts_action", new String[]{"/ext/contentlet/edit_contentlet"},
                        "cmd", new String[]{"new"},
                        "inode", new String[]{""}
    	            ));
        	        actionUrl = portletURL.toString() + "&selectedStructure=" + contentTypeInode +
                        "&lang=" + this.getLanguageId(user.getLanguageId());
                } else {		
                    Logger.info(this, "Portlets are empty for the Layout: " + 		
                    		layout.getId());		
                }		
             } else {		
             	Logger.info(this, "Layouts are empty for the user: " + user.getUserId());
            }
        } catch (Exception e) {
			Logger.error(this,
					String.format(
							"An error occurred when retrieving the action URL of Content Type [%s] for user [%s]: %s",
							contentTypeInode, user.getUserId(), e.getMessage()),
					e);
        }
        return actionUrl;
    }

	/**
	 * Returns the correct internal language ID based on the user-selected
	 * locale.
	 * 
	 * @param userLocaleString
	 *            - The String representation of the locale selected by the
	 *            user.
	 * @return The ID of the selected locale.
	 */
    private Long getLanguageId (final String userLocaleString) {
        Long languageId = null;
        final Locale locale = LocaleUtil.fromLanguageId(userLocaleString);
        if (null != locale) {
            try {
				languageId = this.languageAPI.getLanguage(locale.getLanguage(), locale.getCountry()).getId();
            } catch (Exception e) {
                languageId = null;
            }
        }
        if (null == languageId) {
            languageId = this.languageAPI.getDefaultLanguage().getId();
        }
        return languageId;
    }

}
