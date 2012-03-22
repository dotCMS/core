package com.dotmarketing.portlets.usermanager.action;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_READ;
import static com.dotmarketing.business.PermissionAPI.PERMISSION_WRITE;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.cms.factories.PublicAddressFactory;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.mailinglists.factories.MailingListFactory;
import com.dotmarketing.portlets.mailinglists.model.MailingList;
import com.dotmarketing.portlets.usermanager.factories.UserManagerListBuilderFactory;
import com.dotmarketing.portlets.usermanager.factories.UserManagerPropertiesFactory;
import com.dotmarketing.portlets.usermanager.struts.UserManagerListSearchForm;
import com.dotmarketing.tag.factories.TagFactory;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.tag.model.TagInode;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.Validator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.Address;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.ActionException;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ActionResponseImpl;
import com.liferay.portlet.RenderRequestImpl;
import com.liferay.util.servlet.SessionMessages;
import com.liferay.util.servlet.UploadPortletRequest;

/**
 *
 * @author Oswaldo Gallango
 * @version 1.0
 *
 */
public class ViewUserManagerListAction extends DotPortletAction {

    private PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    /**
     * @param permissionAPI
     *            the permissionAPI to set
     */
    public void setPermissionAPI(PermissionAPI permissionAPIRef) {
        permissionAPI = permissionAPIRef;
    }

    /**
     * render
     *
     * @param mapping
     *            ActionMapping.
     * @param form
     *            ActionForm.
     * @param config
     *            PortletConfig.
     * @param req
     *            RenderRequest.
     * @param res
     *            RenderResponse.
     * @return ActionForward.
     * @exception Exception
     *                return an exception if there is an error
     * @see com.liferay.portal.struts.PortletAction#render(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm, javax.portlet.PortletConfig,
     *      javax.portlet.RenderRequest, javax.portlet.RenderResponse)
     */
    public ActionForward render(ActionMapping mapping, ActionForm form, PortletConfig config, RenderRequest req, RenderResponse res) throws Exception {

        HttpServletRequest request = ((RenderRequestImpl) req).getHttpServletRequest();

        String cmd = req.getParameter("cmd");

        UserManagerListSearchForm searchForm = (UserManagerListSearchForm) form;

        String userId = request.getParameter("userId");
        if (userId != null && !userId.equalsIgnoreCase("")) {
            String[] arrayUserIds = new String[1];
            arrayUserIds[0] = userId;
            searchForm.setArrayUserIds(arrayUserIds);
        }

        try {

            User user = com.liferay.portal.util.PortalUtil.getUser(req);
            List list = null;
            List roles = APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
            Iterator rolesIt = roles.iterator();
            boolean isMarketingAdmin = false;
            while (rolesIt.hasNext()) {
                Role role = (Role) rolesIt.next();
                if (role.getName().equals(Config.getStringProperty("MAILINGLISTS_ADMIN_ROLE"))) {
                    isMarketingAdmin = true;
                    break;
                }
            }
            if (isMarketingAdmin) {
                list = MailingListFactory.getAllMailingLists();
            } else {
                list = MailingListFactory.getMailingListsByUser(user);
                list.add(MailingListFactory.getUnsubscribersMailingList());
            }

            req.setAttribute(WebKeys.MAILING_LIST_VIEW, list);

            if ("load".equals(cmd)) {

                try {
                    return mapping.findForward("portlet.ext.usermanager.load_users");
                } catch (Exception e) {
                    _handleException(e, req, mapping);
                }
            }

            UserManagerPropertiesFactory._getFieldDisplayConfiguration(req);
            if ("config".equals(cmd)) {
                try {
                    return mapping.findForward("portlet.ext.usermanager.edit_usermanagerfields");
                } catch (Exception e) {
                    _handleException(e, req, mapping);
                }
            }
            if ("saveFile".equals(cmd)) {
                try {
                    UserManagerPropertiesFactory._add(req, res, config, form);
                    UserManagerPropertiesFactory._save(req, res, config, form);
                } catch (Exception ae) {
                    _handleException(ae, req, mapping);
                }
                return mapping.findForward("portlet.ext.usermanager.edit_usermanagerfields");
            }

            // Delete usermanager
            if (com.liferay.portal.util.Constants.DELETE.equals(cmd)) {
                try {
                    _delete(form, req, res);
                    cmd = com.liferay.portal.util.Constants.SEARCH;
                } catch (Exception e) {
                    _handleException(e, req, mapping);
                }
            }

            HttpSession sess = ((RenderRequestImpl) req).getHttpServletRequest().getSession();

            if (!Validator.validate(request, searchForm, mapping)) {
                if (req.getWindowState().equals(WindowState.NORMAL)) {
                    return mapping.findForward("portlet.ext.usermanager.view");
                } else {
                    if (cmd != null && cmd.equals(com.liferay.portal.util.Constants.SEARCH)) {
                        _doSearch(searchForm, req, res);
                    }
                    req.setAttribute(WebKeys.USERMANAGERLISTFORM, searchForm);

                    return mapping.findForward("portlet.ext.usermanager.view_usermanagerlist");
                }
            }
            if (req.getWindowState().equals(WindowState.NORMAL)) {
                req.setAttribute(WebKeys.USERMANAGERLISTFORM, searchForm);
                return mapping.findForward("portlet.ext.usermanager.view");
            } else {
                if (searchForm.getPage() == 0) {
                    if (sess.getAttribute(WebKeys.USERMANAGERLISTFORM) != null) {
                        searchForm = (UserManagerListSearchForm) sess.getAttribute(WebKeys.USERMANAGERLISTFORM);
                        form = searchForm;
                        if (cmd == null) {
                            _doSearch(searchForm, req, res);
                        }
                    }
                }
                if (cmd != null && cmd.equals(com.liferay.portal.util.Constants.SEARCH)) {
                    _doSearch(searchForm, req, res);
                }
                if (sess.getAttribute(WebKeys.USERMANAGERLISTFORM) != null) {
                    req.setAttribute(WebKeys.USERMANAGERLISTFORM, sess.getAttribute(WebKeys.USERMANAGERLISTFORM));
                    req.setAttribute(WebKeys.USERMANAGERLIST, sess.getAttribute(WebKeys.USERMANAGERLIST));
                    req.setAttribute(WebKeys.USERMANAGERLISTCOUNT, sess.getAttribute(WebKeys.USERMANAGERLISTCOUNT));

                } else {
                    req.setAttribute(WebKeys.USERMANAGERLISTFORM, searchForm);
                }
                return mapping.findForward("portlet.ext.usermanager.view_usermanagerlist");
            }
        } catch (Exception e) {
            req.setAttribute(PageContext.EXCEPTION, e);
            return mapping.findForward(Constants.COMMON_ERROR);
        }
    }

    /**
     * processAction
     *
     * @param mapping
     *            ActionMapping.
     * @param form
     *            ActionForm.
     * @param config
     *            PortletConfig.
     * @param req
     *            ActionRequest.
     * @param res
     *            ActionResponse.
     * @exception Exception
     *                return an exception if there is an error
     */
    public void processAction(ActionMapping mapping, ActionForm form, PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {
        String cmd = req.getParameter(com.liferay.portal.util.Constants.CMD);
        UserManagerListSearchForm searchForm = (UserManagerListSearchForm) form;
        if (cmd != null && cmd.equals("exportExcel")) {
            _exportExcel(req, res, config, form);
        }
        if (cmd != null && cmd.equals("downloadCSVTemplate")) {
            _downloadCSVTemplate(req, res, config, form);
        }

        if (com.liferay.portal.util.Constants.ADD.equals(cmd)) {
            try {
                _loadUserCSVFiles(form, req, res);
                String actionList = req.getParameter("actionList");
                String usermanagerListInode = req.getParameter("usermanagerListInode");
                if (actionList != null
                        && (actionList.equals(com.liferay.portal.util.Constants.SAVE) || actionList.equals(com.liferay.portal.util.Constants.ADD))) {
                    _saveMailingCSVList(form, req, res, usermanagerListInode);
                    searchForm.setArrayUserIds(null);

                    String referer = req.getParameter("referer");
                    if (UtilMethods.isSet(referer)) {
                    	_sendToReferral(req, res, referer);
                    	return;
                    }
                }
            } catch (Exception e) {
                _handleException(e, req);
            }

            _doSearch(searchForm, req, res);
            setForward(req, "portlet.ext.usermanager.view_usermanagerlist");
            return;
        }

        if ("saveFile".equals(cmd)) {
            try {
                UserManagerPropertiesFactory._getFieldDisplayConfiguration(req);
                UserManagerPropertiesFactory._add(req, res, config, form);
                UserManagerPropertiesFactory._save(req, res, config, form);
            } catch (Exception ae) {
                _handleException(ae, req);
            }

            String redirect = req.getParameter("referrer");
            redirect = URLDecoder.decode(redirect, "UTF-8");
            _sendToReferral(req, res, redirect);
            return;
        }

        // Saving Marketing List
        if (com.liferay.portal.util.Constants.SAVE.equals(cmd)) {
            try {
                _saveMailingList(form, req, res, "0");
            } catch (Exception e) {
                _handleException(e, req);
            }
        }
        if (com.liferay.portal.util.Constants.UPDATE.equals(cmd)) {
            try {
                if (searchForm.getUsermanagerListInode().equals("")) {
                    SessionMessages.add(req, "message", "message.mailinglistbuilder.nolist");
                } else {
                    _saveMailingList(form, req, res, searchForm.getUsermanagerListInode());
                }
            } catch (Exception e) {
                _handleException(e, req);
            }
        }
        if (com.liferay.portal.util.Constants.EDIT.equals(cmd)) {

            try {
                if (searchForm.getUsermanagerListInode().equals("")) {
                    SessionMessages.add(req, "message", "message.mailinglistbuilder.nolist");
                } else {
                    _removeFromMailingList(form, req, res, searchForm.getUsermanagerListInode());
                }
            } catch (Exception e) {
                _handleException(e, req);
            }
        }

        if ("updateSelectedGroupsRoles".equals(cmd)) {
            try {
                if (_addEditSelectedUsersGroupsRoles(req)) {
                    SessionMessages.add(req, "message", "message.mailinglistbuilder.groups-roles.updated");
                } else {
                    SessionMessages.add(req, "message", "message.mailinglistbuilder.groups-roles.not-updated");
                }
            } catch (Exception e) {
                _handleException(e, req);
            }
        }

        if ("deleteUsers".equals(cmd)) {
            try {
                if (_deleteSelectedUsers(req)) {
                    SessionMessages.add(req, "message", "message.usermanager.users.deleted");
                } else {
                    SessionMessages.add(req, "message", "message.usermanager.users.not-deleted");
                }
            } catch (Exception e) {
                _handleException(e, req);
            }

            _doSearch(searchForm, req, res);
            setForward(req, "portlet.ext.usermanager.view_usermanagerlist");
            return;
        }


        HttpSession sess = ((ActionRequestImpl) req).getHttpServletRequest().getSession();
        String[][] matchesArray = (String[][]) sess.getAttribute(WebKeys.USERMANAGERLIST);
        int count = (matchesArray != null) ? matchesArray.length : 0;

        req.setAttribute(WebKeys.USERMANAGERLIST, matchesArray);
        sess.setAttribute(WebKeys.USERMANAGERLIST, matchesArray);
        req.setAttribute(WebKeys.USERMANAGERLISTCOUNT, count);
        sess.setAttribute(WebKeys.USERMANAGERLISTCOUNT, count);

        if (sess.getAttribute(WebKeys.USERMANAGERLISTFORM) != null) {
            sess.setAttribute(WebKeys.USERMANAGERLISTFORM, (UserManagerListSearchForm) sess.getAttribute(WebKeys.USERMANAGERLISTFORM));
            req.setAttribute(WebKeys.USERMANAGERLISTFORM, (UserManagerListSearchForm) sess.getAttribute(WebKeys.USERMANAGERLISTFORM));
        }

        setForward(req, "portlet.ext.usermanager.view_usermanagerlist");

    }

    /**
     * _doSearch
     *
     * @param searchForm
     *            UserManagerListSearchForm.
     * @param req
     *            RenderRequest.
     * @param res
     *            RenderResponse.
     * @exception Exception
     *                return an exception if there is an error
     */
    private void _doSearch(UserManagerListSearchForm searchForm, RenderRequest req, RenderResponse res) throws Exception {
        HttpSession session = ((RenderRequestImpl) req).getHttpServletRequest().getSession();

        User searcherUser = PortalUtil.getUser(req);
        boolean isUserManagerAdmin = UserManagerListBuilderFactory.isUserManagerAdmin(searcherUser);

        String page = req.getParameter("page");
        if (!UtilMethods.isSet(page))
            page = (String) session.getAttribute("page");
        else
            session.setAttribute("page", page);

        if (!UtilMethods.isSet(page))
            page = "1";

        req.setAttribute("page", page);

        Map<String, Object> hm = _doSearch(searchForm, searcherUser, isUserManagerAdmin, page, Config.getIntProperty("USERMANAGER_PER_PAGE"));

        String[][] matchesArray = (String[][]) hm.get("matchesArray");
        int count = ((Integer) hm.get("count")).intValue();

        req.setAttribute(WebKeys.USERMANAGERLIST, matchesArray);
        session.setAttribute(WebKeys.USERMANAGERLIST, matchesArray);

        req.setAttribute(WebKeys.USERMANAGERLISTCOUNT, count);
        session.setAttribute(WebKeys.USERMANAGERLISTCOUNT, count);

        session.setAttribute(WebKeys.USERMANAGERLISTPARAMETERS, searchForm);
        session.setAttribute(WebKeys.USERMANAGERLISTFORM, searchForm);
        req.setAttribute(WebKeys.USERMANAGERLISTFORM, searchForm);
    }

    /**
     * _doSearch
     *
     * @param searchForm
     *            UserManagerListSearchForm.
     * @param req
     *            RenderRequest.
     * @param res
     *            RenderResponse.
     * @exception Exception
     *                return an exception if there is an error
     */
    public static void _doSearch(UserManagerListSearchForm searchForm, ActionRequest req, ActionResponse res) throws Exception {
        HttpSession session = ((RenderRequestImpl) req).getHttpServletRequest().getSession();

        User searcherUser = PortalUtil.getUser(req);
        boolean isUserManagerAdmin = UserManagerListBuilderFactory.isUserManagerAdmin(searcherUser);

        String page = req.getParameter("page");
        if (!UtilMethods.isSet(page))
            page = (String) session.getAttribute("page");
        else
            session.setAttribute("page", page);

        if (!UtilMethods.isSet(page))
            page = "1";

        req.setAttribute("page", page);

        Map<String, Object> hm = _doSearch(searchForm, searcherUser, isUserManagerAdmin, page, Config.getIntProperty("USERMANAGER_PER_PAGE"));

        String[][] matchesArray = (String[][]) hm.get("matchesArray");
        int count = ((Integer) hm.get("count")).intValue();

        req.setAttribute(WebKeys.USERMANAGERLIST, matchesArray);
        session.setAttribute(WebKeys.USERMANAGERLIST, matchesArray);

        req.setAttribute(WebKeys.USERMANAGERLISTCOUNT, count);
        session.setAttribute(WebKeys.USERMANAGERLISTCOUNT, count);

        session.setAttribute(WebKeys.USERMANAGERLISTPARAMETERS, searchForm);
        session.setAttribute(WebKeys.USERMANAGERLISTFORM, searchForm);
    }

    /**
     * _doSearch
     *
     * @param searchForm
     *            UserManagerListSearchForm.
     * @param searcherUser
     *            User.
     * @param isUserManagerAdmin
     *            boolean.
     * @param page
     *            String.
     * @param perPage
     *            int.
     * @return Map<String, Object>
     * @exception Exception
     *                return an exception if there is an error
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> _doSearch(UserManagerListSearchForm searchForm, User searcherUser, boolean isUserManagerAdmin, String page,
            int perPage) throws Exception {

        int pageInt = 1;
        try {
            pageInt = Integer.parseInt(page);
        } catch (NumberFormatException e) {
        }
        int offset = (pageInt - 1) * perPage;
        int endRecord = perPage;

        searchForm.setStartRow(offset);
        searchForm.setMaxRow(endRecord);

        List<Map<String, Object>> matches = UserManagerListBuilderFactory.doSearch(searchForm);
        searchForm.setStartRow(0);
        searchForm.setMaxRow(0);
        boolean isCount = true;
        int count = 0;
        try {
            count = Integer
                    .parseInt((String) ((Map<String, Object>) UserManagerListBuilderFactory.doSearch(searchForm, isCount).get(0)).get("total"));
        } catch (Exception ex) {
        }

        String[][] matchesArray = new String[matches.size()][16];
        Iterator<Map<String, Object>> it = matches.iterator();
        for (int i = 0; it.hasNext(); i++) {

            User user = null;

            String userId = (String) ((Map<String, Object>) it.next()).get("userid");

            user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);

            UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);

            List<Address> addresses = user.getAddresses();
            Address address = null;
            if (addresses.size() > 0) {
                address = (Address) addresses.get(0);
            }
            matchesArray[i][0] = (userId == null) ? "" : userId;
            matchesArray[i][1] = (user.getFirstName() == null) ? "" : user.getFirstName();
            matchesArray[i][2] = (user.getMiddleName() == null) ? "" : user.getMiddleName();
            matchesArray[i][3] = (user.getLastName() == null) ? "" : user.getLastName();
            matchesArray[i][4] = (user.getEmailAddress() == null) ? "" : user.getEmailAddress();
            matchesArray[i][5] = (address == null) ? "" : address.getStreet1();
            matchesArray[i][6] = (address == null) ? "" : address.getStreet2();
            matchesArray[i][7] = (address == null) ? "" : address.getCity();
            matchesArray[i][8] = (address == null) ? "" : address.getState();
            matchesArray[i][9] = (address == null) ? "" : address.getZip();
            matchesArray[i][10] = (address == null) ? "" : address.getCountry();
            matchesArray[i][11] = (address == null) ? "" : address.getPhone();
            matchesArray[i][12] = UtilMethods.htmlDateToHTMLTime(user.getCreateDate());

            if (!isUserManagerAdmin) {
                // adding read permission
                try {
                    _checkUserPermissions(userProxy, searcherUser, PERMISSION_READ);
                    matchesArray[i][13] = "true";
                } catch (ActionException ae) {
                    matchesArray[i][13] = "false";
                }

                // adding write permission
                try {
                    _checkUserPermissions(userProxy, searcherUser, PERMISSION_WRITE);
                    matchesArray[i][14] = "true";
                } catch (ActionException ae) {
                    matchesArray[i][14] = "false";
                }
            } else {
                matchesArray[i][13] = "true";
                matchesArray[i][14] = "true";
            }

            matchesArray[i][15] = String.valueOf(userProxy.getInode());
        }

        Map<String, Object> hm = new java.util.HashMap<String, Object>();
        hm.put("matchesArray", matchesArray);
        hm.put("count", count);

        return hm;
    }

    // Deleting User manager
    /**
     * _delete
     *
     * @param form
     *            ActionForm.
     * @param req
     *            RenderRequest.
     * @param res
     *            RenderResponse.
     * @exception Exception
     *                return an exception if there is an error
     */
    private void _delete(ActionForm form, RenderRequest req, RenderResponse res) throws Exception {

        String userId = req.getParameter("userID");
        String companyId = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getCompanyId();

        User user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
        UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);

        //delete user tags
        List<TagInode> userTagsList = TagFactory.getTagInodeByInode(String.valueOf(userProxy.getInode()));
        for (TagInode tag : userTagsList) {
        	Tag retrievedTag = TagFactory.getTagByTagId(tag.getTagId());
            TagFactory.deleteTagInode(tag);
            TagFactory.deleteTag(retrievedTag.getTagId());
        }

        if(InodeUtils.isSet(userProxy.getInode())) {
        	PermissionAPI perAPI = APILocator.getPermissionAPI();
        	perAPI.removePermissions(userProxy);
        	// deletes user proxy
        	InodeFactory.deleteInode(userProxy);
        }
        // deletes liferay user
        APILocator.getUserAPI().delete(APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false),com.dotmarketing.business.APILocator.getUserAPI().getSystemUser(), false);

        SessionMessages.add(req, "message", "message.usermanager.deleted");

    }

    /**
     * _exportExcel
     *
     * @param req
     *            RenderRequest.
     * @param res
     *            RenderResponse.
     * @param config
     *            PortletConfig.
     * @param form
     *            ActionForm.
     * @exception Exception
     *                return an exception if there is an error
     */
    @SuppressWarnings("unchecked")
    private void _exportExcel(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {

        HttpSession session = ((ActionRequestImpl) req).getHttpServletRequest().getSession();

        ActionResponseImpl resImpl = (ActionResponseImpl) res;
        HttpServletResponse httpRes = resImpl.getHttpServletResponse();

        User searcherUser = PortalUtil.getUser(req);

        UserManagerListSearchForm searchForm = (UserManagerListSearchForm) session.getAttribute(WebKeys.USERMANAGERLISTPARAMETERS);
        searchForm.setStartRow(0);
        searchForm.setMaxRow(0);
        List matches = UserManagerListBuilderFactory.doSearch(searchForm);

        String usersStr = req.getParameter("users");
        boolean fullCommand = new Boolean(req.getParameter("fullCommand"));
        if (usersStr == null) {
            usersStr = "";
        }

        int numberGenericVariables = Config.getIntProperty("MAX_NUMBER_VARIABLES_TO_SHOW");
        Company comp = PublicCompanyFactory.getDefaultCompany();
        String[][] matchesArray = new String[matches.size()][16 + numberGenericVariables];
        Iterator it = matches.iterator();
        for (int i = 0; it.hasNext(); i++) {

            User user = null;

            String userId = (String) ((Map) it.next()).get("userid");
            //Fix a bug when a userid is a substring of another userid example dotcms.1574 and dotcms15743
            String userIdExt = userId + ",";
            if (usersStr.equalsIgnoreCase("") || usersStr.contains(userIdExt) || fullCommand) {
                user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);

                UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);

                // adding read permission
                try {
                    _checkUserPermissions(userProxy, searcherUser, PERMISSION_READ);
                } catch (ActionException ae) {
                    continue;
                }

                List addresses = user.getAddresses();
                Address address = null;
                if (addresses.size() > 0) {
                    address = (Address) addresses.get(0);
                }

                matchesArray[i][0] = userId;
                matchesArray[i][1] = (user.getFirstName() == null) ? "" : user.getFirstName();
                matchesArray[i][2] = (user.getMiddleName() == null) ? "" : user.getMiddleName();
                matchesArray[i][3] = (user.getLastName() == null) ? "" : user.getLastName();
                matchesArray[i][4] = (user.getEmailAddress() == null) ? "" : user.getEmailAddress();
                matchesArray[i][5] = (user.getPassword() == null) ? "" : user.getPassword();
                matchesArray[i][6] = UtilMethods.htmlDateToHTMLTime(user.getBirthday());
                matchesArray[i][7] = (address == null) ? "" : address.getStreet1();
                matchesArray[i][8] = (address == null) ? "" : address.getStreet2();
                matchesArray[i][9] = (address == null) ? "" : address.getCity();
                matchesArray[i][10] = (address == null) ? "" : address.getState();
                matchesArray[i][11] = (address == null) ? "" : address.getZip();
                matchesArray[i][12] = (address == null) ? "" : address.getCountry();
                matchesArray[i][13] = (address == null) ? "" : address.getPhone();
                matchesArray[i][14] = (address == null) ? "" : address.getFax();
                matchesArray[i][15] = (address == null) ? "" : address.getCell();

                for (int j = 1; j <= numberGenericVariables; j++) {
                    matchesArray[i][15 + j] = (userProxy.getVar(j) == null) ? "" : userProxy.getVar(j);
                }
            }
        }

        httpRes.setContentType("application/octet-stream");
        httpRes.setHeader("Content-Disposition", "attachment; filename=\"users_" + UtilMethods.dateToHTMLDate(new Date(), "M_d_yyyy") + ".csv\"");

        ServletOutputStream out = httpRes.getOutputStream();
        try {

            if (matchesArray.length > 0) {

                out
                        .print("First Name,Middle Name,Last Name,Email Address,User Password,Date Of Birth,Address Street1,Address Street2,Address City,Address State,Address Zip,Address Country,Phone Number,Fax Number,Cell Number,");
                for (int j = 1; j <= numberGenericVariables; j++) {
                    out.print(LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var" + j) + ",");
                }
                out.print("\r\n");
                for (int i = 0; i < matchesArray.length; i++) {
                    String[] match = matchesArray[i];
                    if (match[0] != null && !match[0].equalsIgnoreCase("")) {
                        out.print((match[1] == null ? "," : "\"" + match[1] + "\","));
                        out.print((match[2] == null ? "," : "\"" + match[2] + "\","));
                        out.print((match[3] == null ? "," : "\"" + match[3] + "\","));
                        out.print((match[4] == null ? "," : "\"" + match[4] + "\","));
                        out.print((match[5] == null ? "," : "\"" + match[5] + "\","));
                        out.print((match[6] == null ? "," : "\"" + match[6] + "\","));
                        out.print((match[7] == null ? "," : "\"" + match[7] + "\","));
                        out.print((match[8] == null ? "," : "\"" + match[8] + "\","));
                        out.print((match[9] == null ? "," : "\"" + match[9] + "\","));
                        out.print((match[10] == null ? "," : "\"" + match[10] + "\","));
                        out.print((match[11] == null ? "," : "\"" + match[11] + "\","));
                        out.print((match[12] == null ? "," : "\"" + match[12] + "\","));
                        out.print((match[13] == null ? "," : "\"" + match[13] + "\","));
                        out.print((match[14] == null ? "," : "\"" + match[14] + "\","));
                        out.print((match[15] == null ? "," : "\"" + match[15] + "\","));
                        for (int j = 1; j <= numberGenericVariables; j++) {
                            out.print((match[15 + j] == null ? "," : "\"" + match[15 + j] + "\","));
                        }
                        out.print("\r\n");
                    }
                }
            } else {
                out.print("There are no Users to show");
                out.print("\r\n");
            }
            out.flush();
            out.close();
            HibernateUtil.closeSession();
        } catch (Exception p) {
            out.print("There are no Users to show");
            out.print("\r\n");
            out.flush();
            out.close();
            HibernateUtil.closeSession();
        }
    }

    /**
     * _downloadCSVTemplate
     *
     * @param req
     *            RenderRequest.
     * @param res
     *            RenderResponse.
     * @param config
     *            PortletConfig.
     * @param form
     *            ActionForm.
     * @exception Exception
     *                return an exception if there is an error
     */
    private void _downloadCSVTemplate(ActionRequest req, ActionResponse res, PortletConfig config, ActionForm form) throws Exception {

        ActionResponseImpl resImpl = (ActionResponseImpl) res;
        HttpServletResponse httpRes = resImpl.getHttpServletResponse();

        httpRes.setContentType("application/octet-stream");
        httpRes.setHeader("Content-Disposition", "attachment; filename=\"CSV_Template.csv\"");

        ServletOutputStream out = httpRes.getOutputStream();
        int numberGenericVariables = Config.getIntProperty("MAX_NUMBER_VARIABLES_TO_SHOW");
        Company comp = PublicCompanyFactory.getDefaultCompany();

        if (comp.getAuthType().equalsIgnoreCase(Company.AUTH_TYPE_ID)) {
        	out.print("First Name,Middle Name,Last Name,Email Address,User Password,Date Of Birth,Address Street1,Address Street2,Address City,Address State,Address Zip,Address Country,Phone Number,Fax Number,Cell Number,User ID,");
        } else {
        	out.print("First Name,Middle Name,Last Name,Email Address,User Password,Date Of Birth,Address Street1,Address Street2,Address City,Address State,Address Zip,Address Country,Phone Number,Fax Number,Cell Number,");
        }

        for (int j = 1; j <= numberGenericVariables; j++) {
            out.print(LanguageUtil.get(comp.getCompanyId(), comp.getLocale(), "user.profile.var" + j) + ",");
        }
        out.print("\r\n");

        // adding record for example
        out.print("First Name,,Last Name,user@mydomain.com\r\n");

        out.flush();
        out.close();
        HibernateUtil.closeSession();
    }

    /**
     * _loadUserCSVFiles
     *
     * @param form
     *            ActionForm.
     * @param req
     *            RenderRequest.
     * @param res
     *            RenderResponse.
     * @exception Exception
     *                return an exception if there is an error
     */
    private void _loadUserCSVFiles(ActionForm form, ActionRequest req, ActionResponse res) throws Exception {

    	User systemUser = APILocator.getUserAPI().getSystemUser();

        UserManagerListSearchForm userForm = (UserManagerListSearchForm) form;

        UploadPortletRequest uploadReq = PortalUtil.getUploadPortletRequest(req);
        java.io.File uploadedFile = uploadReq.getFile("newUsersFile");
        StringBuffer returnMessage = new StringBuffer();

        User userLoader = PortalUtil.getUser(req);
        boolean someError = false;
        String companyId = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getCompanyId();
        // read permission
        String[] readPermissions = req.getParameterValues("readRole");
        // write permission
        String[] writePermissions = req.getParameterValues("writeRole");

        int userCreated = 0;
        int countUserDuplicated = 0;
        int userTaged = 0;
        int numberGenericVariables = Config.getIntProperty("MAX_NUMBER_VARIABLES_TO_SHOW");

        List<String> arrayUserIds = new ArrayList<String>();

        // creating tags
        if (UtilMethods.isSet(userForm.getTagName())) {
            StringTokenizer tagNameToken = new StringTokenizer(userForm.getTagName(), ",");
            if (tagNameToken.hasMoreTokens()) {
                for (; tagNameToken.hasMoreTokens();) {
                    String tagTokenized = tagNameToken.nextToken().trim();
                    TagFactory.getTag(tagTokenized, userLoader.getUserId());
                }
            }
        }

        if (uploadedFile.exists() && uploadedFile.length() > 0) {

            //get the groups
            String groupsStr = req.getParameter("groups");
            //get the roles
            String rolesStr = req.getParameter("roles");

            String token;
            StringTokenizer tokens = new StringTokenizer(groupsStr, ",");
            HashSet<String> groups = new HashSet<String>();
            for (; tokens.hasMoreTokens();) {
                if (!((token = tokens.nextToken().trim()).equals("")))
                    groups.add(token);
            }

            tokens = new StringTokenizer(rolesStr, ",");
            HashSet<String> roles = new HashSet<String>();
            for (; tokens.hasMoreTokens();) {
                if (!((token = tokens.nextToken().trim()).equals("")))
                    roles.add(token);
            }

            int lineNumber = 0;

            BufferedReader input = new BufferedReader(new FileReader(uploadedFile));
            try {
                if (userForm.isIgnoreHeaders()) {
                    input.readLine();
                    lineNumber++;
                }
                String line = null;
                Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
                HibernateUtil.startTransaction();
                while ((line = input.readLine()) != null) {
                    lineNumber++;
                    if (lineNumber % 100 == 0) {
                        HibernateUtil.commitTransaction();
                        HibernateUtil.startTransaction();

                    }
                    String[] lineTok = UtilMethods.specialSplit(line, ",", "\"");

                    String firstName = "";
                    String middleName = "";
                    String lastName = "";
                    String email = "";
                    String password = "";
                    String dateOfBirthday = "";
                    String street1 = "";
                    String street2 = "";
                    String city = "";
                    String state = "";
                    String zip = "";
                    String country = "";
                    String phone = "";
                    String fax = "";
                    String cell = "";
                    String userId = "";

                    try {

                        int i = 0;
                        try {
                            firstName = lineTok[i++];
                        } catch (Exception e) {
                        }
                        try {
                            middleName = lineTok[i++];
                        } catch (Exception e) {
                        }
                        try {
                            lastName = lineTok[i++];
                        } catch (Exception e) {
                        }
                        try {
                            email = lineTok[i++];
                        } catch (Exception e) {
                        }

                        if (UtilMethods.isValidEmail(email)) {

                            try {
                                password = lineTok[i++];
                            } catch (Exception e) {
                            }

                            Date birthday = null;
                            String validationBirthdayString;
                            try {
                                dateOfBirthday = lineTok[i++];

                                birthday = UtilMethods.htmlToDate(dateOfBirthday);
                                validationBirthdayString = UtilMethods.dateToHTMLDate(birthday);
                                if (!dateOfBirthday.startsWith(validationBirthdayString) || (9999 < birthday.getYear()))
                                    birthday = null;
                            } catch (Exception e) {
                            }

                            try {
                                street1 = lineTok[i++];
                            } catch (Exception e) {
                            }
                            try {
                                street2 = lineTok[i++];
                            } catch (Exception e) {
                            }
                            try {
                                city = lineTok[i++];
                            } catch (Exception e) {
                            }
                            try {
                                state = lineTok[i++];
                            } catch (Exception e) {
                            }
                            try {
                                zip = lineTok[i++];
                            } catch (Exception e) {
                            }
                            try {
                                country = lineTok[i++];
                            } catch (Exception e) {
                            }
                            try {
                                phone = lineTok[i++];
                            } catch (Exception e) {
                            }
                            try {
                                fax = lineTok[i++];
                            } catch (Exception e) {
                            }
                            try {
                                cell = lineTok[i++];
                            } catch (Exception e) {
                            }

                            if (comp.getAuthType().equals(Company.AUTH_TYPE_ID)) {
                                try {
                                    userId = lineTok[i++];
                                } catch (Exception e) {
                                }
                            }

                            if (firstName.equalsIgnoreCase("")) {
                                if (!someError) {
                                    returnMessage.append( LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Errors-loading-users-br-br") );
                                    someError = true;
                                }
                                returnMessage.append( LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Missing-First-Name-on-line") +" "+ lineNumber + "<br>");
                                continue;
                            }
                            if (lastName.equalsIgnoreCase("")) {
                                if (!someError) {
                                    returnMessage.append(LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Errors-loading-users-br-br"));
                                    someError = true;
                                }
                                returnMessage.append(LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Missing-Last-Name-on-line")+" " + lineNumber + "<br>");
                                continue;
                            }
                            if (email.equalsIgnoreCase("")) {
                                if (!someError) {
                                    returnMessage.append(LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Errors-loading-users-br-br"));
                                    someError = true;
                                }
                                returnMessage.append(LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Missing-Email-Address-on-line") +" "+ lineNumber + "<br>");
                                continue;
                            }

                            User userDuplicated = null;
                            try {
                            	if (comp.getAuthType().equals(Company.AUTH_TYPE_ID)) {
                            		userDuplicated = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
                            	} else {
                            		userDuplicated = APILocator.getUserAPI().loadByUserByEmail(email, APILocator.getUserAPI().getSystemUser(), false);
                            	}
                            } catch (Exception e) {
                            }

                            if (userDuplicated == null) {
                            	User user = null;

                                // Check company authorization type to set user id.
                                if (comp.getAuthType().equals(Company.AUTH_TYPE_ID)) {
                                	try {
                                		 user = APILocator.getUserAPI().createUser(userId, null);
                                	} catch (IndexOutOfBoundsException ie) {
                                		Logger.error(this.getClass(), ie.getMessage(), ie);
                                	}

                                	if(userId.equalsIgnoreCase("")) {
	                                	if (!someError) {
	                                        returnMessage.append(LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Errors-loading-users-br-br"));
	                                        someError = true;
	                                    }
	                                    returnMessage.append(LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Missing-User-ID-on-line")+" " + lineNumber + "<br>");
	                                    continue;
                                	}
                                }else {
                                	 user = APILocator.getUserAPI().createUser(null, null);
                                }
                                user.setCreateDate(new Date());
                                user.setGreeting("Welcome, " + firstName + " " + lastName + "!");
                                user.setFirstName(firstName);
                                user.setMiddleName(middleName);
                                user.setLastName(lastName);
                                user.setNickName("");
                                user.setEmailAddress(email.trim().toLowerCase());
                                user.setBirthday(birthday);

                                if (!UtilMethods.isSet(password)) {
                                    password = PublicEncryptionFactory.getRandomPassword();
                                    user.setActive(false);
                                } else {
                                    user.setActive(true);
                                }
                                user.setPassword(PublicEncryptionFactory.digestString(password));
                                user.setPasswordEncrypted(true);

                                APILocator.getUserAPI().save(user, APILocator.getUserAPI().getSystemUser(), false);

                                UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);
                                String userProxyInode = userProxy.getInode();
                                userProxy = (UserProxy) InodeFactory.getInode(userProxyInode, UserProxy.class);
                                userProxy.setInode(userProxyInode);
                                userProxy.setUserId(user.getUserId());
                                // getting and saving the user
                                // additional info
                                String userAdditionalInfo = "";
                                for (int j = 1; j <= numberGenericVariables; j++) {
                                    try {
                                        userAdditionalInfo = lineTok[i++];
                                    } catch (Exception e) {
                                        userAdditionalInfo = "";
                                    }
                                    if (!userAdditionalInfo.equalsIgnoreCase("")) {
                                        userProxy.setVar(j, userAdditionalInfo);
                                    }
                                }

                                com.dotmarketing.business.APILocator.getUserProxyAPI().saveUserProxy(userProxy,APILocator.getUserAPI().getSystemUser(), false);

                                userProxyInode = userProxy.getInode();
                                // adding roles to user
                                Permission permission = null;
                                if (readPermissions != null) {
                                    for (int n = 0; n < readPermissions.length; n++) {
                                        permission = new Permission(userProxyInode, readPermissions[n], PERMISSION_READ);
                                        permissionAPI.save(permission, userProxy, systemUser, false);
                                    }
                                }

                                if (writePermissions != null) {
                                    for (int n = 0; n < writePermissions.length; n++) {
                                        permission = new Permission(userProxyInode, writePermissions[n], PERMISSION_WRITE);
                                        permissionAPI.save(permission, userProxy, systemUser, false);
                                    }
                                }

                                Date today = new Date();

                                Address address = PublicAddressFactory.getInstance();
                                address.setUserName(user.getFullName());
                                address.setCompanyId(companyId);
                                address.setUserId(user.getUserId());
                                address.setCreateDate(today);
                                address.setModifiedDate(today);
                                address.setPriority(1);
                                address.setClassName(user.getClass().getName());
                                address.setClassPK(user.getUserId());
                                address.setDescription("Primary");
                                address.setStreet1(street1);
                                address.setStreet2(street2);
                                address.setCountry(country);
                                address.setCity(city);
                                address.setState(state);
                                address.setZip(zip);
                                address.setPhone(phone);
                                address.setFax(fax);
                                address.setCell(cell);

                                PublicAddressFactory.save(address);

                                // creating tag users
                                if (UtilMethods.isSet(userForm.getTagName())) {
                                    StringTokenizer tagNameToken = new StringTokenizer(userForm.getTagName(), ",");
                                    if (tagNameToken.hasMoreTokens()) {
                                        for (; tagNameToken.hasMoreTokens();) {
                                            String tagTokenized = tagNameToken.nextToken().trim();
                                            TagFactory.addTagInode(tagTokenized, userLoader.getUserId(), "");
                                        }
                                    }
                                }


                                for (String roleId : roles) {
                                    com.dotmarketing.business.APILocator.getRoleAPI().addRoleToUser(roleId, user);
                                }

                                userCreated++;
                                userTaged++;

                                arrayUserIds.add(user.getUserId());

                            } else {
                                // email duplicated

                                if (userForm.isUpdateDuplicatedUsers()) {
                                    if (UtilMethods.isSet(firstName))
                                        userDuplicated.setFirstName(firstName);

                                    if (UtilMethods.isSet(middleName))
                                        userDuplicated.setMiddleName(middleName);

                                    if (UtilMethods.isSet(lastName))
                                        userDuplicated.setLastName(lastName);

                                    userDuplicated
                                            .setGreeting("Welcome, " + userDuplicated.getFirstName() + " " + userDuplicated.getLastName() + "!");

                                    if (UtilMethods.isSet(email))
                                        userDuplicated.setEmailAddress(email.trim().toLowerCase());

                                    if (UtilMethods.isSet(birthday))
                                        userDuplicated.setBirthday(birthday);

                                    if (UtilMethods.isSet(password)) {
                                        userDuplicated.setPassword(PublicEncryptionFactory.digestString(password));
                                        userDuplicated.setPasswordEncrypted(true);
                                    }

                                    APILocator.getUserAPI().save(userDuplicated,APILocator.getUserAPI().getSystemUser(),false);

                                    UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userDuplicated,APILocator.getUserAPI().getSystemUser(), false);
                                    // getting and saving the user
                                    // additional info
                                    String userAdditionalInfo = "";
                                    for (int j = 1; j <= numberGenericVariables; j++) {
                                        try {
                                            userAdditionalInfo = lineTok[i++];
                                        } catch (Exception e) {
                                            userAdditionalInfo = "";
                                        }
                                        if (!userAdditionalInfo.equalsIgnoreCase("")) {
                                            userProxy.setVar(j, userAdditionalInfo);
                                        }
                                    }

                                    com.dotmarketing.business.APILocator.getUserProxyAPI().saveUserProxy(userProxy,APILocator.getUserAPI().getSystemUser(), false);

                                   String userProxyInode = userProxy.getInode();

                                   permissionAPI.removePermissions(userProxy);

                                    // adding roles to user
                                    if (readPermissions != null) {
                                        for (int n = 0; n < readPermissions.length; n++) {
                                            permissionAPI.save(new Permission(userProxyInode, readPermissions[n], PERMISSION_READ), userProxy, systemUser, false);
                                        }
                                    }

                                    if (writePermissions != null) {
                                        for (int n = 0; n < writePermissions.length; n++) {
                                            permissionAPI.save(new Permission(userProxyInode, writePermissions[n], PERMISSION_WRITE), userProxy, systemUser, false);
                                        }
                                    }

                                    Address address = null;
                                    List<Address> addresses = PublicAddressFactory.getAddressesByUserId(userDuplicated.getUserId());
                                    for (int pos = 0; pos < addresses.size(); ++pos) {
                                        if ((addresses.get(pos).getDescription() != null) && (addresses.get(pos).getDescription().equals("Primary"))) {
                                            address = addresses.get(pos);
                                            break;
                                        }
                                    }

                                    Date today = new Date();

                                    if (address == null) {
                                        address = PublicAddressFactory.getInstance();
                                        address.setUserName(userDuplicated.getFullName());
                                        address.setCompanyId(companyId);
                                        address.setUserId(userDuplicated.getUserId());
                                        address.setCreateDate(today);
                                        address.setModifiedDate(today);
                                        address.setPriority(1);
                                        address.setClassName(userDuplicated.getClass().getName());
                                        address.setClassPK(userDuplicated.getUserId());
                                        address.setDescription("Primary");
                                        address.setStreet1(street1);
                                        address.setStreet2(street2);
                                        address.setCountry(country);
                                        address.setCity(city);
                                        address.setState(state);
                                        address.setZip(zip);
                                        address.setPhone(phone);
                                        address.setFax(fax);
                                        address.setCell(cell);
                                    } else {
                                        address.setModifiedDate(today);

                                        if (UtilMethods.isSet(street1))
                                            address.setStreet1(street1);

                                        if (UtilMethods.isSet(street2))
                                            address.setStreet2(street2);

                                        if (UtilMethods.isSet(country))
                                            address.setCountry(country);

                                        if (UtilMethods.isSet(city))
                                            address.setCity(city);

                                        if (UtilMethods.isSet(state))
                                            address.setState(state);

                                        if (UtilMethods.isSet(zip))
                                            address.setZip(zip);

                                        if (UtilMethods.isSet(phone))
                                            address.setPhone(phone);

                                        if (UtilMethods.isSet(fax))
                                            address.setFax(fax);

                                        if (UtilMethods.isSet(cell))
                                            address.setCell(cell);
                                    }

                                    PublicAddressFactory.save(address);

                                    for (String roleId : roles) {
                                        com.dotmarketing.business.APILocator.getRoleAPI().addRoleToUser(roleId, userDuplicated);
                                    }

                                    countUserDuplicated++;
                                } else {
                                    if (!someError) {
                                        returnMessage.append(LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Errors-loading-users-br-br"));
                                        someError = true;
                                    }

                                    returnMessage.append(LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Duplicated-Email")+": " + email + " "+LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "user.duplicated.email.line")+" " + lineNumber + "<br>");
                                }

                                if (UtilMethods.isSet(userForm.getTagName())) {
                                    StringTokenizer tagNameToken = new StringTokenizer(userForm.getTagName(), ",");
                                    if (tagNameToken.hasMoreTokens()) {
                                        UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userDuplicated,APILocator.getUserAPI().getSystemUser(), false);
                                        for (; tagNameToken.hasMoreTokens();) {
                                            String tagTokenized = tagNameToken.nextToken().trim();
                                            TagFactory.addTagInode(tagTokenized, String.valueOf(userProxy.getInode()), "");
                                        }
                                    }
                                }

                                arrayUserIds.add(userDuplicated.getUserId());

                                userTaged++;
                            }

                        } else {
                            // invalid email
                            if (!someError) {
                                returnMessage.append(LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Errors-loading-users-br-br"));
                                someError = true;
                            }
                            returnMessage.append(LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Invalid-Email")+": " + email + " "+LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "user.duplicated.email.line")+" " + lineNumber + "<br>");
                        }

                    } catch (Exception e) {
                        if (!someError) {
                            returnMessage.append(LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Errors-loading-users-br-br"));
                            someError = true;
                        }
                        returnMessage.append(LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Error-creating-user-with-email")+": " + email + " "+LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "user.duplicated.email.line")+" " + lineNumber + "<br>");

                    }

                }
                CacheLocator.getCmsRoleCache().clearCache();

            } catch (Exception e) {
                Logger.error(this.getClass(), e.getMessage(), e);
            }

            if (someError) {
                returnMessage.append("<br><br>");
            } else {
                returnMessage.append(LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Users-uploaded-successfully-br-br"));
            }
            HibernateUtil.commitTransaction();
            returnMessage.append(userCreated + " "+LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "new-user-accounts-created<br>")+" " + countUserDuplicated +" "+LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "user-accounts-updated"));

            if (UtilMethods.isSet(userForm.getTagName())) {
                returnMessage.append("<br>" + userTaged +" "+ LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "users-tagged-as>")+" " + userForm.getTagName());
            }

            SessionMessages.add(req, "message", returnMessage.toString());
            userForm.setArrayUserIds((String[]) arrayUserIds.toArray(new String[0]));
            HibernateUtil.flush();

        }

    }

    /**
     * validateUniqueEmail
     *
     * @param emailAddress
     *            String.
     * @param companyId
     *            String.
     * @return boolean
     */
    private boolean validateUniqueEmail(String emailAddress, String companyId) {
        boolean returnValue = true;
        User member = null;
        if (UtilMethods.isSet(emailAddress)) {
            try {
                member = APILocator.getUserAPI().loadByUserByEmail(emailAddress, APILocator.getUserAPI().getSystemUser(), false);
            } catch (Exception ex) {
                Logger.debug(this, ex.toString());
            }
        }
        if (!(member == null)) {
            returnValue = false;
        }
        return returnValue;
    }

    /**
     * Used to save a new mailing list from the results obtained of a user
     * search
     *
     * @param form
     * @param req
     * @param res
     * @param mailingListInode
     * @throws Exception
     */
    private void _saveMailingList(ActionForm form, ActionRequest req, ActionResponse res, String mailingListInode) throws Exception {

        User user = _getUser(req);
        UserManagerListSearchForm mlForm = (UserManagerListSearchForm) form;

        // Saving mailing list
        MailingList ml = (MailingList) InodeFactory.getInode(mailingListInode, MailingList.class);
        String cmd = req.getParameter(com.liferay.portal.util.Constants.CMD);

        if (com.liferay.portal.util.Constants.SAVE.equals(cmd)) {
            ml.setTitle(mlForm.getUsermanagerListTitle());
            ml.setPublicList(mlForm.isAllowPublicToSubscribe());
            ml.setUserId(user.getUserId());
        }
        HibernateUtil.saveOrUpdate(ml);
        mlForm.setUsermanagerListTitle("");
        mlForm.setAllowPublicToSubscribe(false);

        // Getting subscribers
        List<UserProxy> userProxies = null;
        if (!UserManagerListBuilderFactory.isFullCommand(req)) {
            String userIds = req.getParameter("users");
            userProxies = UserManagerListBuilderFactory.getUserProxiesFromList(userIds);
        } else {
            ActionRequestImpl reqImpl = (ActionRequestImpl) req;
            HttpServletRequest httpReq = reqImpl.getHttpServletRequest();
            UserManagerListSearchForm userManagerListSearchForm = (UserManagerListSearchForm) httpReq.getSession().getAttribute(
                    WebKeys.USERMANAGERLISTFORM);
            if (userManagerListSearchForm == null)
                userProxies = new ArrayList<UserProxy>();
            else
                userProxies = _getUserProxySubscribers(req, userManagerListSearchForm);
        }
        User sys = APILocator.getUserAPI().getSystemUser();
        // Adding subscribers
        for (UserProxy s : userProxies) {
        	if(s.getUserId() ==null){
        		continue;
        	}
        	if(s.getUserId().equals(sys.getUserId())){
        		continue;
        	}


            try {
                _checkUserPermissions(s, user, PERMISSION_READ);
                MailingListFactory.addMailingSubscriber(ml, s, false);
            } catch (Exception e) {
            }
        }

        SessionMessages.add(req, "message", "message.mailinglistbuilder.save");
    }

    /**
     * _removeFromMailingList
     *
     * @param form
     *            ActionForm.
     * @param req
     *            ActionRequest.
     * @param res
     *            ActionResponse.
     * @param mailingListInode
     *            String.
     * @exception Exception
     *                return an exception if there is an error
     */
    private void _removeFromMailingList(ActionForm form, ActionRequest req, ActionResponse res, String mailingListInode) throws Exception {

        // Retrieving mailing list
        MailingList ml = (MailingList) InodeFactory.getInode(mailingListInode, MailingList.class);

        // Getting subscribers
        List<UserProxy> userProxies = null;
        if (!UserManagerListBuilderFactory.isFullCommand(req)) {
            String userIds = req.getParameter("users");
            userProxies = UserManagerListBuilderFactory.getUserProxiesFromList(userIds);
        } else {
            userProxies = _getUserProxySubscribers(req, form);
        }
        // Removing subscribers
        for (UserProxy s : userProxies) {
            MailingListFactory.deleteUserFromMailingList(ml, s);
        }

        SessionMessages.add(req, "message", "message.mailinglistbuilder.subscribers.removed");
    }

    /**
     * _addEditSelectedUsersGroupsRoles
     *
     * @param req
     *            ActionRequest.
     * @return boolean.
     */
    private boolean _addEditSelectedUsersGroupsRoles(ActionRequest req) {
        try {
            //Get the user
            String usersStr = req.getParameter("users");
            //if Full Command update all the users in the search criteria
            if (UserManagerListBuilderFactory.isFullCommand(req)) {
                usersStr = UserManagerListBuilderFactory.loadFullCommand(req);
            }

            //get the groups
            String groupsStr = req.getParameter("groups");
            //get the roles
            String rolesStr = req.getParameter("roles");

            if ((usersStr == null) || (usersStr.trim().equals("")))
                return true;

            if (((groupsStr == null) || (groupsStr.trim().equals(""))) && ((rolesStr == null) || (rolesStr.trim().equals(""))))
                return true;

            StringTokenizer tokens = new StringTokenizer(usersStr, ",");
            String token;

            HashSet<String> users = new HashSet<String>();
            for (; tokens.hasMoreTokens();) {
                if (!((token = tokens.nextToken().trim()).equals("")))
                    users.add(token);
            }

            tokens = new StringTokenizer(groupsStr, ",");
            HashSet<String> groups = new HashSet<String>();
            for (; tokens.hasMoreTokens();) {
                if (!((token = tokens.nextToken().trim()).equals("")))
                    groups.add(token);
            }

            tokens = new StringTokenizer(rolesStr, ",");
            HashSet<String> roles = new HashSet<String>();
            for (; tokens.hasMoreTokens();) {
                if (!((token = tokens.nextToken().trim()).equals("")))
                    roles.add(token);
            }

            User user;
            for (String userId : users) {
            	user = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);

                for (String roleId : roles) {
                    com.dotmarketing.business.APILocator.getRoleAPI().addRoleToUser(roleId, user);
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * _deleteSelectedUsers
     *
     * @param req
     *            ActionRequest.
     * @return boolean.
     */
    private boolean _deleteSelectedUsers(ActionRequest req) {
        try {
            String usersStr = req.getParameter("users");
            if (UserManagerListBuilderFactory.isFullCommand(req)) {
                usersStr = UserManagerListBuilderFactory.loadFullCommand(req);
            }

            if ((usersStr == null) || (usersStr.trim().equals("")))
                return true;

            StringTokenizer tokens = new StringTokenizer(usersStr, ",");
            String token;
            String companyId = null;

            for (; tokens.hasMoreTokens();) {
                if (!((token = tokens.nextToken().trim()).equals("")))
                    companyId = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany().getCompanyId();

                User user = APILocator.getUserAPI().loadUserById(token,APILocator.getUserAPI().getSystemUser(),false);
                UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(user,APILocator.getUserAPI().getSystemUser(), false);

                //delete user tags
                String userId = user.getUserId();
                List<TagInode> userTagsList = TagFactory.getTagInodeByInode(String.valueOf(userProxy.getInode()));
                for (TagInode tag : userTagsList) {
                	Tag retrievedTag = TagFactory.getTagByTagId(tag.getTagId());
                    TagFactory.deleteTagInode(tag);
                    TagFactory.deleteTag(retrievedTag.getTagId());
                }
                // deletes user proxy
                InodeFactory.deleteInode(userProxy);
                // deletes liferay user
                APILocator.getUserAPI().delete(APILocator.getUserAPI().loadUserById(token,APILocator.getUserAPI().getSystemUser(),false),com.dotmarketing.business.APILocator.getUserAPI().getSystemUser(), false);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * _saveMailingCSVList
     *
     * @param form
     *            ActionForm.
     * @param req
     *            ActionRequest.
     * @param res
     *            ActionResponse.
     * @param mailingListInode
     *            String.
     * @exception Exception
     *                return an exception if there is an error
     */
    private void _saveMailingCSVList(ActionForm form, ActionRequest req, ActionResponse res, String mailingListInode) throws Exception {

        User user = _getUser(req);
        UserManagerListSearchForm mlForm = (UserManagerListSearchForm) form;
        String[] matchesArray = mlForm.getArrayUserIds();
        StringBuffer returnMessage = new StringBuffer();

        // Saving mailing list
        MailingList ml = (MailingList) InodeFactory.getInode(mailingListInode, MailingList.class);
        String actionList = req.getParameter("actionList");
        if (com.liferay.portal.util.Constants.ADD.equals(actionList)) {
            ml.setTitle(mlForm.getUsermanagerListTitle());
            ml.setPublicList(mlForm.isAllowPublicToSubscribe());
            ml.setUserId(user.getUserId());
            HibernateUtil.saveOrUpdate(ml);
        }
        mlForm.setUsermanagerListTitle("");
        mlForm.setAllowPublicToSubscribe(false);

        // Adding subscribers
        int userAdded = 0;
        for (int i = 0; i < matchesArray.length; i++) {
            if (matchesArray[i] != null && !matchesArray[i].equalsIgnoreCase("")) {
                UserProxy s = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(matchesArray[i],APILocator.getUserAPI().getSystemUser(), false);
                MailingListFactory.addMailingSubscriber(ml, s, false);
                userAdded++;
            }
        }

        returnMessage.append("<br>" + userAdded +" " +LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Users-Added-to")+" " + ml.getTitle() + " "+LanguageUtil.get(com.liferay.portal.util.PortalUtil.getUser(req), "Mailing-List"));

        String returnSessionMessage = (String) SessionMessages.get(req, "message");
        StringBuffer returnBuffer = new StringBuffer(returnSessionMessage);
        returnBuffer.append(returnMessage);
        SessionMessages.add(req, "message", returnBuffer.toString());

    }



    /**
     * _getUserProxySubscribers
     *
     * @param req
     *            ActionRequest.
     * @param form
     *            ActionForm.
     * @return List<UserProxy>
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws NoSuchUserException
     */
    private List<UserProxy> _getUserProxySubscribers(ActionRequest req, ActionForm form) throws NoSuchUserException, DotDataException, DotSecurityException {
        String usersStr = req.getParameter("users");
        boolean isFullCommand = UserManagerListBuilderFactory.isFullCommand(req);
        User userSubscriber;
        List<UserProxy> userProxies = new ArrayList<UserProxy>();

        if (isFullCommand)
        {
            UserManagerListSearchForm searchForm = (UserManagerListSearchForm) form;
            searchForm.setStartRow(0);
            searchForm.setMaxRow(-1);
            List matches = UserManagerListBuilderFactory.doSearch(searchForm);

            for (int i = 0; i < matches.size(); i++) {
                userSubscriber = APILocator.getUserAPI().loadUserById(((String) ((HashMap<String, Object>) matches.get(i)).get("userid")),APILocator.getUserAPI().getSystemUser(),false);
                UserProxy s = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userSubscriber,APILocator.getUserAPI().getSystemUser(), false);

                if (!InodeUtils.isSet(s.getInode())) {
                    s.setUserId(userSubscriber.getUserId());
                    s.setLastResult(0);
                    HibernateUtil.saveOrUpdate(s);
                }

                userProxies.add(s);
            }
        } else {
            StringTokenizer tokens = new StringTokenizer(usersStr, ",");
            String token;

            for (; tokens.hasMoreTokens();) {
                if (!((token = tokens.nextToken().trim()).equals(""))) {
                    userSubscriber = APILocator.getUserAPI().loadUserById(token,APILocator.getUserAPI().getSystemUser(),false);
                    UserProxy s = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(userSubscriber,APILocator.getUserAPI().getSystemUser(), false);

                    if (!InodeUtils.isSet(s.getInode())) {
                        s.setUserId(userSubscriber.getUserId());
                        s.setLastResult(0);
                        HibernateUtil.saveOrUpdate(s);
                    }

                    userProxies.add(s);
                }
            }
        }

        return userProxies;
    }
}