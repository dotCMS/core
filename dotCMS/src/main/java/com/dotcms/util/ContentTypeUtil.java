package com.dotcms.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.portlet.PortletURL;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.javax.portlet.WindowStateException;
import com.dotcms.rest.api.v1.contenttype.ContentTypeResource;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portlet.PortletURLImpl;
import com.liferay.util.LocaleUtil;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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
      
      return request != null ? getActionUrl(request, contentType, user):null;
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
        return getActionUrl(request,contentTypeInode,user,"/ext/contentlet/edit_contentlet");
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
     * @param strutsAction - struts action url to execute
     * @return The action URL associated to the specified Content Type.
     */
    public String getActionUrl( HttpServletRequest request, final String contentTypeInode, final User user, final String strutsAction) {
        final List<Layout> layouts;
        String actionUrl = StringUtils.EMPTY;
        String referrer = StringUtils.EMPTY;
        try {
            layouts = this.layoutAPI.loadLayoutsForUser(user);
            if (UtilMethods.isSet(layouts)) {
                final Layout contentLayout = getContentPortletLayout(layouts);
                referrer = generateReferrerUrl(request, contentLayout, contentTypeInode, user);
                final PortletURL portletURL =
                        new PortletURLImpl(request, PortletID.CONTENT.toString(), contentLayout.getId(), true);
                portletURL.setWindowState(WindowState.MAXIMIZED);
                portletURL.setParameters(new HashMap<>(Map.of(
                        "struts_action", new String[] {strutsAction},
                        "cmd", new String[] {"new"},
                        "referer", new String[] {referrer},
                        "inode", new String[] {""},
                        "selectedStructure", new String[] {contentTypeInode},
                        "lang", new String[] {this.getLanguageId(user.getLanguageId()).toString()})));
                actionUrl = portletURL.toString();
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
     * Generates the referrer URL that will indicate the system what location the user will be
     * redirected to after performing an operation in the back-end. For example, this can be used by
     * the "+" sign component that adds different types of content to the system, as it indicates
     * where to return after adding new content.
     * 
     * @param request - The {@link HttpServletRequest} object.
     * @param layout - The layout where the user will be redirected after performing his task.
     * @param contentTypeInode - The Inode of the content type used by the new content.
     * @param user - The user performing this action.
     * @return The referrer URL.
     * @throws WindowStateException If the portlet does not support the
     *         {@link WindowState.MAXIMIZED} state.
     */
    private String generateReferrerUrl(final HttpServletRequest request, final Layout layout, final String contentTypeInode,
                    final User user) throws WindowStateException {
        final PortletURL portletURL = new PortletURLImpl(request, PortletID.CONTENT.toString(), layout.getId(), true);
        portletURL.setWindowState(WindowState.MAXIMIZED);
        portletURL.setParameters(new HashMap<>(Map.of(
                "struts_action", new String[] {"/ext/contentlet/view_contentlets"},
                "cmd", new String[] {"new"},
                "inode", new String[] {""},
                "structure_id", new String[] {contentTypeInode},
                "lang", new String[] {this.getLanguageId(user.getLanguageId()).toString()})));
        return portletURL.toString();
    }

    /**
     * Traverses the list of {@link Layout} objects in order to find the one that contains the
     * "Content" portlet. If not present, then the first layout in the list is returned by default.
     * Layouts are basically the menu option that groups one or more links that lead to portlets in
     * dotCMS.
     * 
     * @param layouts - The list of layouts.
     * @return The specific layout that contains the "Content" portlet.
     */
    private Layout getContentPortletLayout(final List<Layout> layouts) {
        for (Layout layout : layouts) {
            final List<String> portletIds = layout.getPortletIds();
            if (UtilMethods.isSet(portletIds)) {
                final Optional<String> contentPortletId =
                                portletIds.stream().filter(id -> id.equals(PortletID.CONTENT.toString())).findFirst();
                if (contentPortletId.isPresent()) {
                    return layout;
                }
            } else {
                Logger.info(this, "Portlets are empty for the Layout: " + layout.getId());
            }
        }
        return layouts.get(0);
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
