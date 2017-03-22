package com.dotcms.util;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.cms.login.LoginService;
import com.dotcms.cms.login.LoginServiceFactory;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.portlet.PortletURL;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
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
 * Util class for {@link com.dotmarketing.portlets.ContentType.model.ContentType}
 */
public class ContentTypeUtil {

    private final LayoutAPI layoutAPI;
    private final LanguageAPI languageAPI;
    private final HttpServletRequestThreadLocal httpServletRequestThreadLocal;
    private final LoginService loginService;

    private static class SingletonHolder {
        private static final ContentTypeUtil INSTANCE = new ContentTypeUtil();
    }

    public static ContentTypeUtil getInstance() {
        return ContentTypeUtil.SingletonHolder.INSTANCE;
    }


    @VisibleForTesting
    public ContentTypeUtil(LayoutAPI layoutAPI,
                           LanguageAPI languageAPI,
                           HttpServletRequestThreadLocal httpServletRequestThreadLocal,
                           LoginService loginService){

        this.layoutAPI = layoutAPI;
        this.languageAPI = languageAPI;
        this.httpServletRequestThreadLocal = httpServletRequestThreadLocal;
        this.loginService = loginService;
    }

    private ContentTypeUtil(){
        layoutAPI = APILocator.getLayoutAPI();
        languageAPI = APILocator.getLanguageAPI();
        httpServletRequestThreadLocal = HttpServletRequestThreadLocal.INSTANCE;
        loginService = LoginServiceFactory.getInstance().getLoginService();
    }

    public String getActionUrl(final ContentType ContentType) {
        HttpServletRequest request = httpServletRequestThreadLocal.getRequest();
        User user = loginService.getLoggedInUser(request);
        
      //It is ok not to have a logged in user all the time as this can be called by a plugin or by a Unit test
        if ( user == null ) {
            Logger.debug(this, "No Logged in User found when calling ContentTypeUtil.getActionUrl");
            return null;
        }

        return getActionUrl(request, ContentType, user);
    }
    
    public String getActionUrl(final ContentType ContentType, final User user) {
      HttpServletRequest request = httpServletRequestThreadLocal.getRequest();
      return getActionUrl(request, ContentType, user);
  }
    
    
    /**
     * Get the action url for the ContentType
     * @param ContentType
     * @return String
     */
    public String getActionUrl( HttpServletRequest request, final ContentType ContentType, User user) {
        return getActionUrl(request, ContentType.inode(), user);
    }

    public String getActionUrl( HttpServletRequest request, final String ContentTypeInode, User user) {
        final List<Layout> layouts;
        String actionUrl = StringUtils.EMPTY;


        try {
            layouts = layoutAPI.loadLayoutsForUser(user);

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

        	        actionUrl = portletURL.toString() + "&selectedContentType=" + ContentTypeInode +
                        "&lang=" + this.getLanguageId(user.getLanguageId(), languageAPI);
                } else {		
               		
                    Logger.info(this, "Portlets are empty for the Layout: " + 		
                    		layout.getId());		
                }		
             } else {		
             		
             	Logger.info(this, "Layouts are empty for the user: " + user.getUserId());
            }
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
        }

        return actionUrl;
    }

    private Long getLanguageId (final String userLocaleString, final LanguageAPI languageAPI) {


        Long languageId = null;
        final Locale locale = LocaleUtil.fromLanguageId(userLocaleString);

        if (null != locale) {

            try {

                languageId =
                        languageAPI.getLanguage(locale.getLanguage(),
                                locale.getCountry()).getId();
            } catch (Exception e) {

                languageId = null;
            }
        }

        if (null == languageId) {

            languageId = languageAPI.getDefaultLanguage().getId();
        }

        return languageId;
    }
}
