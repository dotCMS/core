/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.liferay.portal.util;

import com.dotcms.repackage.javax.portlet.ActionRequest;
import com.dotcms.repackage.javax.portlet.ActionResponse;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.PortletContext;
import com.dotcms.repackage.javax.portlet.PortletPreferences;
import com.dotcms.repackage.javax.portlet.PortletRequest;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.javax.portlet.ValidatorException;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.util.Logger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyLocalManagerUtil;
import com.liferay.portal.ejb.PortletPreferencesPK;
import com.liferay.portal.ejb.UserLocalManagerUtil;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.portlet.ConcretePortletWrapper;
import com.liferay.portlet.PortletPreferencesImpl;
import com.liferay.portlet.PortletPreferencesWrapper;
import com.liferay.portlet.RenderRequestImpl;
import com.liferay.portlet.RenderResponseImpl;
import com.liferay.util.CollectionFactory;
import com.liferay.util.ParamUtil;
import com.liferay.util.SimpleCachePool;
import com.liferay.util.StringComparator;
import com.liferay.util.StringPool;
import com.liferay.util.servlet.DynamicServletRequest;
import com.liferay.util.servlet.UploadPortletRequest;
import com.liferay.util.servlet.UploadServletRequest;
import io.vavr.control.Try;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;

/**
 * <a href="PortalUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.95 $
 *
 */
public class PortalUtil {

  public static final String PUBLIC_PATH = "_public";
  private static final String SEARCH_STRING = "-";
  private static final String REPLACEMENT = "_";

  public static void clearRequestParameters(RenderRequest req) {

    // Clear the render parameters if they were set during processAction

    boolean action = ParamUtil.getBoolean(req, WebKeys.PORTLET_URL_ACTION);

    if (action) {
      ((RenderRequestImpl) req).getRenderParameters().clear();
    }
  }

  public static void copyRequestParameters(ActionRequest req, ActionResponse res) {

    // Set the request parameters as the render parameters

    try {
      ActionResponseImpl resImpl = (ActionResponseImpl) res;

      Map renderParameters = resImpl.getRenderParameters();

      res.setRenderParameter(WebKeys.PORTLET_URL_ACTION, "1");

      Enumeration enu = req.getParameterNames();

      while (enu.hasMoreElements()) {
        String param = (String) enu.nextElement();
        String[] values = req.getParameterValues(param);

        if (renderParameters.get(resImpl.getNamespace() + param) == null) {

          res.setRenderParameter(param, values);
        }
      }
    } catch (IllegalStateException ise) {

      // This should only happen if the developer called
      // sendRedirect of com.dotcms.repackage.javax.portlet.ActionResponse

    }
  }

  public static void destroyPortletConfig(Portlet portlet) {
    String scpId = PortalUtil.class.getName() + "." + PortletConfig.class.getName();

      scpId += "." + portlet.getCompanyId();
    

    Map map = (Map) SimpleCachePool.get(scpId);

    if (map == null) {
      return;
    }

    map.remove(portlet.getPortletId());
  }

  public static void destroyPortletContext(Portlet portlet) {
    String scpId = PortalUtil.class.getName() + "." + PortletContext.class.getName();

      scpId += "." + portlet.getCompanyId();
    

    Map map = (Map) SimpleCachePool.get(scpId);

    if (map == null) {
      return;
    }

    map.remove(portlet.getPortletId());
  }

  public static void destroyPortletInstance(Portlet portlet) {
    String scpId = PortalUtil.class.getName() + "." + com.dotcms.repackage.javax.portlet.Portlet.class.getName();

      scpId += "." + portlet.getCompanyId();
    

    Map map = (Map) SimpleCachePool.get(scpId);

    if (map == null) {
      return;
    }

    ConcretePortletWrapper portletInstance = (ConcretePortletWrapper) map.get(portlet.getPortletId());

    if (portletInstance != null) {
      portletInstance.destroy();

      map.remove(portlet.getPortletId());
    }


    destroyPortletConfig(portlet);
    destroyPortletContext(portlet);
  }

  public static String getAuthorizedPath(HttpServletRequest req) {
    String userId = getUserId(req);

    if ((userId == null) && (req.getRemoteUser() == null)) {
      return PUBLIC_PATH;
    } else {
      return StringPool.BLANK;
    }
  }

  public static Company getCompany(HttpServletRequest req) throws PortalException, SystemException {

    String companyId = PortalUtil.getCompanyId(req);

    if (companyId == null) {
      return null;
    }

    Company company = (Company) req.getAttribute(WebKeys.COMPANY);

    if (company == null) {
      company = CompanyLocalManagerUtil.getCompany(companyId);

      req.setAttribute(WebKeys.COMPANY, company);
    }

    return company;
  }

  public static Company getCompany(ActionRequest req) throws PortalException, SystemException {

    ActionRequestImpl reqImpl = (ActionRequestImpl) req;

    return getCompany(reqImpl.getHttpServletRequest());
  }

  public static Company getCompany(RenderRequest req) throws PortalException, SystemException {

    RenderRequestImpl reqImpl = (RenderRequestImpl) req;

    return getCompany(reqImpl.getHttpServletRequest());
  }

  public static String getCompanyId(HttpServletRequest req) {
    String companyId = (String) req.getSession().getAttribute(WebKeys.COMPANY_ID);

    if (companyId == null) {
      companyId = (String) req.getAttribute(WebKeys.COMPANY_ID);
      if (companyId != null) {
        req.getSession().setAttribute(WebKeys.COMPANY_ID, companyId);
      }
    }
    if (companyId == null) {
      companyId = PublicCompanyFactory.getDefaultCompanyId();
      if (companyId != null) {
        req.getSession().setAttribute(WebKeys.COMPANY_ID, companyId);
      }

    }

    return companyId;
  }

  public static String getCompanyId(ActionRequest req) {
    ActionRequestImpl reqImpl = (ActionRequestImpl) req;

    return getCompanyId(reqImpl.getHttpServletRequest());
  }

  public static String getCompanyId(PortletRequest req) {
    String companyId = null;

    if (req instanceof ActionRequest) {
      companyId = getCompanyId((ActionRequest) req);
    } else {
      companyId = getCompanyId((RenderRequest) req);
    }

    return companyId;
  }

  public static String getCompanyId(RenderRequest req) {
    RenderRequestImpl reqImpl = (RenderRequestImpl) req;

    return getCompanyId(reqImpl.getHttpServletRequest());
  }

  public static Locale getLocale(HttpServletRequest req) {
    return (Locale) req.getSession().getAttribute(Globals.LOCALE_KEY);
  }

  public static Locale getLocale(RenderRequest req) {
    RenderRequestImpl reqImpl = (RenderRequestImpl) req;

    return getLocale(reqImpl.getHttpServletRequest());
  }

  public static String getPortletNamespace(String portletName) {
    if (null != portletName) {
      portletName = StringUtils.replace(portletName, SEARCH_STRING, REPLACEMENT);
    }
    return REPLACEMENT + portletName + REPLACEMENT;
  }

  public static PortletPreferencesPK getPortletPreferencesPK(HttpServletRequest req, String portletKey) throws SystemException {

    // HttpSession ses = req.getSession();

    String companyId = getCompanyId(req);

    // Portlet portlet = PortletManagerUtil.getPortletById(
    // companyId, portletKey);

    // String layoutId = null;
    String userId;
    try {
      userId = WebAPILocator.getUserWebAPI().getLoggedInUser(req).getUserId();
    } catch (Exception e) {
      Logger.debug(PortalUtil.class, "Unable to get logged in User Setting to default user: " + e.getMessage(), e);
      userId = User.getDefaultUserId(companyId);
    }
    String layoutId = PortletKeys.SHARED_PREF_ID; // I think this is right JT

    // String prefsSharingType = portlet.getPreferencesSharingType();

    // if (prefsSharingType.equals(Portlet.PREFERENCES_SHARING_TYPE_NONE)) {
    // Layout layout = (Layout)req.getAttribute(WebKeys.LAYOUT);
    //
    // layoutId = layout.getLayoutId();
    // userId = getUserId(req);
    // }
    // else if (prefsSharingType.equals(
    // if (prefsSharingType.equals(
    // Portlet.PREFERENCES_SHARING_TYPE_USER)) {
    //
    // layoutId = PortletKeys.SHARED_PREF_ID;
    // userId = getUserId(req);
    // }
    // else if (prefsSharingType.equals(
    // Portlet.PREFERENCES_SHARING_TYPE_COMPANY)) {
    //
    // layoutId = PortletKeys.SHARED_PREF_ID;
    // userId = User.getDefaultUserId(companyId);
    // }

    if (userId == null) {
      userId = User.getDefaultUserId(companyId);
    }

    PortletPreferencesPK pk = new PortletPreferencesPK(portletKey, layoutId, userId);

    return pk;
  }

  public static PortletPreferences getPreferences(HttpServletRequest req) {
    RenderRequest renderRequest = (RenderRequest) req.getAttribute(WebKeys.JAVAX_PORTLET_REQUEST);

    PortletPreferences prefs = null;

    if (renderRequest != null) {
      PortletPreferencesWrapper prefsWrapper = (PortletPreferencesWrapper) renderRequest.getPreferences();

      prefs = prefsWrapper.getPreferencesImpl();
    }

    return prefs;
  }


  public static User getSelectedUser(HttpServletRequest req) {
    String emailAddress = ParamUtil.getString(req, "p_u_e_a");

    User user = null;

    try {
      user = UserLocalManagerUtil.getUserByEmailAddress(getCompanyId(req), emailAddress);
    } catch (Exception e) {
    }

    return user;
  }

  public static User getSelectedUser(ActionRequest req) {
    ActionRequestImpl reqImpl = (ActionRequestImpl) req;

    return getSelectedUser(reqImpl.getHttpServletRequest());
  }

  public static User getSelectedUser(RenderRequest req) {
    RenderRequestImpl reqImpl = (RenderRequestImpl) req;

    return getSelectedUser(reqImpl.getHttpServletRequest());
  }

  public static String[] getSystemGroups() {
    return _getInstance()._getSystemGroups();
  }

  public static String[] getSystemRoles() {
    return _getInstance()._getSystemRoles();
  }

  public static UploadPortletRequest getUploadPortletRequest(ActionRequest req) {

    ActionRequestImpl actionReq = (ActionRequestImpl) req;
    DynamicServletRequest dynamicReq = (DynamicServletRequest) actionReq.getHttpServletRequest();
    UploadServletRequest uploadReq = (UploadServletRequest) dynamicReq.getRequest();

    return new UploadPortletRequest(uploadReq, getPortletNamespace(actionReq.getPortletName()));
  }

  public static User getUser(HttpServletRequest req) {
    User user = (User) req.getAttribute(WebKeys.USER);
    if (user == null) {
      String userId = PortalUtil.getUserId(req);
      if (userId == null) {
        return null;
      }
      user = Try.of(() -> UserLocalManagerUtil.getUserById(userId)).getOrNull();
      req.setAttribute(WebKeys.USER, user);
    }
    return user;
  }

  public static User getUser(HttpSession session) {
    User user = null;
    if (session != null) {
      user = (User) session.getAttribute(WebKeys.USER);
      if (user == null) {
        String userId = PortalUtil.getUserId(session);
        if (userId == null) {
          return null;
        }
        user = Try.of(() -> UserLocalManagerUtil.getUserById(userId)).getOrNull();
        session.setAttribute(WebKeys.USER, user);
      }
    }
    return user;
  }

  public static User getUser(ActionRequest req) {

    ActionRequestImpl reqImpl = (ActionRequestImpl) req;

    return getUser(reqImpl.getHttpServletRequest());
  }

  public static User getUser(RenderRequest req) {

    RenderRequestImpl reqImpl = (RenderRequestImpl) req;

    return getUser(reqImpl.getHttpServletRequest());
  }

  public static String getUserId(HttpSession ses) {
    if (ses != null && ses.getAttribute(WebKeys.USER_ID) != null) {
      return (String) ses.getAttribute(WebKeys.USER_ID);
    } else {
      return null;
    }
  }

  public static String getUserId(HttpServletRequest req) {

    String userId = (String) req.getAttribute(WebKeys.USER_ID);
    userId = (userId != null) ? userId : getUserId(req.getSession(false));
    req.setAttribute(WebKeys.USER_ID, userId);
    return userId;
  }

  public static String getUserId(ActionRequest req) {
    ActionRequestImpl reqImpl = (ActionRequestImpl) req;

    return getUserId(reqImpl.getHttpServletRequest());
  }

  public static String getUserId(RenderRequest req) {
    RenderRequestImpl reqImpl = (RenderRequestImpl) req;

    return getUserId(reqImpl.getHttpServletRequest());
  }

  public static String getUserName(String userId, String defaultUserName) {
    String userName = defaultUserName;

    try {
      userName = UserLocalManagerUtil.getUserById(userId).getFullName();
    } catch (Exception e) {
    }

    return userName;
  }

  public static String getUserPassword(HttpServletRequest req) {
    return (String) req.getSession().getAttribute(WebKeys.USER_PASSWORD);
  }

  public static String getUserPassword(ActionRequest req) {
    ActionRequestImpl reqImpl = (ActionRequestImpl) req;

    return getUserPassword(reqImpl.getHttpServletRequest());
  }

  public static String getUserPassword(RenderRequest req) {
    RenderRequestImpl reqImpl = (RenderRequestImpl) req;

    return getUserPassword(reqImpl.getHttpServletRequest());
  }

  public static boolean isReservedParameter(String name) {
    return _getInstance()._reservedParams.contains(name);
  }

  public static boolean isSystemRole(Role role) {
    return role.isSystem();
  }

  public static Map mergeCategories(Map oldCategories, Map newCategories) {
    Map mergedCategories = null;

    if (oldCategories == null) {
      mergedCategories = new LinkedHashMap(newCategories);
    } else {
      mergedCategories = new LinkedHashMap(oldCategories);

      Iterator itr = newCategories.entrySet().iterator();

      while (itr.hasNext()) {
        Map.Entry entry = (Map.Entry) itr.next();

        String categoryName = (String) entry.getKey();
        List newKvps = (List) entry.getValue();

        List oldKvps = (List) mergedCategories.get(categoryName);

        if (oldKvps == null) {
          mergedCategories.put(categoryName, newKvps);
        } else {
          oldKvps.addAll(newKvps);
        }
      }
    }

    return mergedCategories;
  }

  public static void storePreferences(PortletPreferences prefs) throws IOException, ValidatorException {

    PortletPreferencesWrapper prefsWrapper = (PortletPreferencesWrapper) prefs;

    PortletPreferencesImpl prefsImpl = (PortletPreferencesImpl) prefsWrapper.getPreferencesImpl();

    prefsImpl.store();
  }

  // public static PortletMode updatePortletMode(
  // String portletId, User user, Layout layout, PortletMode portletMode)
  // throws PortalException, SystemException {
  //
  // if (portletMode == null || Validator.isNull(portletMode.toString())) {
  // if (layout.hasModeEditPortletId(portletId)) {
  // return PortletMode.EDIT;
  // }
  // else if (layout.hasModeHelpPortletId(portletId)) {
  // return PortletMode.HELP;
  // }
  // else {
  // return PortletMode.VIEW;
  // }
  // }
  // else {
  // if (portletMode.equals(PortletMode.EDIT)) {
  // layout.addModeEditPortletId(portletId);
  // }
  // else if (portletMode.equals(PortletMode.HELP)) {
  // layout.addModeHelpPortletId(portletId);
  // }
  // else if (portletMode.equals(PortletMode.VIEW)) {
  // layout.removeModeEditPortletId(portletId);
  // layout.removeModeHelpPortletId(portletId);
  // }
  //
  // if ((user != null) && !layout.isGroup()) {
  // LayoutManagerUtil.updateLayout(
  // layout.getPrimaryKey(), layout.getName(),
  // layout.getColumnOrder(), layout.getNarrow1(),
  // layout.getNarrow2(), layout.getWide(), layout.getStateMax(),
  // layout.getStateMin(), layout.getModeEdit(),
  // layout.getModeHelp());
  // }
  //
  // return portletMode;
  // }
  // }
  //
  // public static WindowState updateWindowState(
  // String portletId, User user, Layout layout, WindowState windowState)
  // throws PortalException, SystemException {
  //
  // if ((windowState == null) ||
  // (Validator.isNull(windowState.toString())) ||
  // (windowState.equals(LiferayWindowState.EXCLUSIVE))) {
  //
  // if (layout.hasStateMaxPortletId(portletId)) {
  // return WindowState.MAXIMIZED;
  // }
  // else if (layout.hasStateMinPortletId(portletId)) {
  // return WindowState.MINIMIZED;
  // }
  // else {
  // return WindowState.NORMAL;
  // }
  // }
  // else {
  // if ((windowState.equals(WindowState.MAXIMIZED)) ||
  // (windowState.equals(LiferayWindowState.POP_UP))) {
  //
  // layout.addStateMaxPortletId(portletId);
  // }
  // else if (windowState.equals(WindowState.MINIMIZED)) {
  // layout.addStateMinPortletId(portletId);
  // }
  // else if (windowState.equals(WindowState.NORMAL)) {
  // layout.removeStateMaxPortletId(portletId);
  // layout.removeStateMinPortletId(portletId);
  // }
  //
  // if ((user != null) && !layout.isGroup()) {
  // LayoutManagerUtil.updateLayout(
  // layout.getPrimaryKey(), layout.getName(),
  // layout.getColumnOrder(), layout.getNarrow1(),
  // layout.getNarrow2(), layout.getWide(), layout.getStateMax(),
  // layout.getStateMin(), layout.getModeEdit(),
  // layout.getModeHelp());
  // }
  //
  // return windowState;
  // }
  // }

  public static User updateUser(HttpServletRequest req, HttpServletResponse res, String userId, String password1, String password2,
      boolean passwordReset) throws PortalException, SystemException {

    User user = UserManagerUtil.updateUser(userId, password1, password2, passwordReset);

    return user;
  }

  public static User updateUser(ActionRequest req, ActionResponse res, String userId, String password1, String password2,
      boolean passwordReset) throws PortalException, SystemException {

    ActionRequestImpl reqImpl = (ActionRequestImpl) req;
    ActionResponseImpl resImpl = (ActionResponseImpl) res;

    return updateUser(reqImpl.getHttpServletRequest(), resImpl.getHttpServletResponse(), userId, password1, password2, passwordReset);
  }

  public static User updateUser(RenderRequest req, RenderResponse res, String userId, String password1, String password2,
      boolean passwordReset) throws PortalException, SystemException {

    RenderRequestImpl reqImpl = (RenderRequestImpl) req;
    RenderResponseImpl resImpl = (RenderResponseImpl) res;

    return updateUser(reqImpl.getHttpServletRequest(), resImpl.getHttpServletResponse(), userId, password1, password2, passwordReset);
  }

  public static User updateUser(HttpServletRequest req, HttpServletResponse res, String userId, String firstName, String middleName,
      String lastName, String nickName, boolean male, Date birthday, String emailAddress, String smsId, String aimId, String icqId,
      String msnId, String ymId, String favoriteActivity, String favoriteBibleVerse, String favoriteFood, String favoriteMovie,
      String favoriteMusic, String languageId, String timeZoneId, String skinId, boolean dottedSkins, boolean roundedSkins, String greeting,
      String resolution, String refreshRate, String comments) throws PortalException, SystemException {

    String password = getUserPassword(req);
    if (!userId.equals(getUserId(req))) {
      password = StringPool.BLANK;
    }

    return UserManagerUtil.updateUser(userId, password, firstName, middleName, lastName, nickName, male, birthday, emailAddress, smsId,
        aimId, icqId, msnId, ymId, favoriteActivity, favoriteBibleVerse, favoriteFood, favoriteMovie, favoriteMusic, languageId, timeZoneId,
        skinId, dottedSkins, roundedSkins, greeting, resolution, refreshRate, comments);
  }

  public static User updateUser(ActionRequest req, ActionResponse res, String userId, String firstName, String middleName, String lastName,
      String nickName, boolean male, Date birthday, String emailAddress, String smsId, String aimId, String icqId, String msnId,
      String ymId, String favoriteActivity, String favoriteBibleVerse, String favoriteFood, String favoriteMovie, String favoriteMusic,
      String languageId, String timeZoneId, String skinId, boolean dottedSkins, boolean roundedSkins, String greeting, String resolution,
      String refreshRate, String comments) throws PortalException, SystemException {

    ActionRequestImpl reqImpl = (ActionRequestImpl) req;
    ActionResponseImpl resImpl = (ActionResponseImpl) res;

    return updateUser(reqImpl.getHttpServletRequest(), resImpl.getHttpServletResponse(), userId, firstName, middleName, lastName, nickName,
        male, birthday, emailAddress, smsId, aimId, icqId, msnId, ymId, favoriteActivity, favoriteBibleVerse, favoriteFood, favoriteMovie,
        favoriteMusic, languageId, timeZoneId, skinId, dottedSkins, roundedSkins, greeting, resolution, refreshRate, comments);
  }

  public static User updateUser(RenderRequest req, RenderResponse res, String userId, String firstName, String middleName, String lastName,
      String nickName, boolean male, Date birthday, String emailAddress, String smsId, String aimId, String icqId, String msnId,
      String ymId, String favoriteActivity, String favoriteBibleVerse, String favoriteFood, String favoriteMovie, String favoriteMusic,
      String languageId, String timeZoneId, String skinId, boolean dottedSkins, boolean roundedSkins, String greeting, String resolution,
      String refreshRate, String comments) throws PortalException, SystemException {

    RenderRequestImpl reqImpl = (RenderRequestImpl) req;
    RenderResponseImpl resImpl = (RenderResponseImpl) res;

    return updateUser(reqImpl.getHttpServletRequest(), resImpl.getHttpServletResponse(), userId, firstName, middleName, lastName, nickName,
        male, birthday, emailAddress, smsId, aimId, icqId, msnId, ymId, favoriteActivity, favoriteBibleVerse, favoriteFood, favoriteMovie,
        favoriteMusic, languageId, timeZoneId, skinId, dottedSkins, roundedSkins, greeting, resolution, refreshRate, comments);
  }

  private static PortalUtil _getInstance() {
    if (_instance == null) {
      synchronized (PortalUtil.class) {
        if (_instance == null) {
          _instance = new PortalUtil();
        }
      }
    }

    return _instance;
  }

  private PortalUtil() {

    // Groups

    // String customSystemGroups[] =
    // PropsUtil.getArray(PropsUtil.SYSTEM_GROUPS);

    // if (customSystemGroups == null || customSystemGroups.length == 0) {
    // _allSystemGroups = Group.SYSTEM_GROUPS;
    // }
    // else {
    // _allSystemGroups = new String[
    // Group.SYSTEM_GROUPS.length + customSystemGroups.length];
    //
    // System.arraycopy(
    // Group.SYSTEM_GROUPS, 0, _allSystemGroups, 0,
    // Group.SYSTEM_GROUPS.length);
    //
    // System.arraycopy(
    // customSystemGroups, 0, _allSystemGroups,
    // Group.SYSTEM_GROUPS.length, customSystemGroups.length);
    // }

    // _sortedSystemGroups = new String[_allSystemGroups.length];
    //
    // System.arraycopy(
    // _allSystemGroups, 0, _sortedSystemGroups, 0,
    // _allSystemGroups.length);
    //
    // Arrays.sort(_sortedSystemGroups, new StringComparator());

    // Roles

    // String customSystemRoles[] = PropsUtil.getArray(PropsUtil.SYSTEM_ROLES);

    // if (customSystemRoles == null || customSystemRoles.length == 0) {
    // _allSystemRoles = Role.SYSTEM_ROLES;
    // }
    // else {
    // _allSystemRoles = new String[
    // Role.SYSTEM_ROLES.length + customSystemRoles.length];
    //
    // System.arraycopy(
    // Role.SYSTEM_ROLES, 0, _allSystemRoles, 0,
    // Role.SYSTEM_ROLES.length);
    //
    // System.arraycopy(
    // customSystemRoles, 0, _allSystemRoles, Role.SYSTEM_ROLES.length,
    // customSystemRoles.length);
    // }

    // _sortedSystemRoles = new String[_allSystemRoles.length];
    //
    // System.arraycopy(
    // _allSystemRoles, 0, _sortedSystemRoles, 0, _allSystemRoles.length);
    //
    // Arrays.sort(_sortedSystemRoles, new StringComparator());

    // Reserved parameter names

    _reservedParams = CollectionFactory.getHashSet();

    _reservedParams.add("p_l_id");
    _reservedParams.add("p_p_id");
    _reservedParams.add("p_p_action");
    _reservedParams.add("p_p_state");
    _reservedParams.add("p_p_mode");
  }

  private String[] _getSystemGroups() {
    return _allSystemGroups;
  }

  private String[] _getSystemRoles() {
    return _allSystemRoles;
  }

  private boolean _isSystemGroup(String groupName) {
    if (groupName == null) {
      return false;
    }

    groupName = groupName.trim();

    int pos = Arrays.binarySearch(_sortedSystemGroups, groupName, new StringComparator());

    if (pos >= 0) {
      return true;
    } else {
      return false;
    }
  }

  private boolean _isSystemRole(String roleName) {
    if (roleName == null) {
      return false;
    }

    roleName = roleName.trim();

    int pos = Arrays.binarySearch(_sortedSystemRoles, roleName, new StringComparator());

    if (pos >= 0) {
      return true;
    } else {
      return false;
    }
  }

  private static PortalUtil _instance;

  private String[] _allSystemGroups;
  private String[] _allSystemRoles;
  private String[] _sortedSystemGroups;
  private String[] _sortedSystemRoles;
  private Set _reservedParams;

}
