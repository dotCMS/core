package com.dotmarketing.servlets.ajax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublishingEndPointAPI;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.javax.portlet.WindowState;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

/**
 * This class acts like an invoker for classes that extend AjaxAction. It is intended to allow
 * developers a quick, safe and easy way to write AJAX servlets in dotCMS without having to wire
 * web.xml
 * 
 * @author will
 * 
 */
public class Trashme extends HttpServlet {

  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) throws ServletException {

  }

  protected void testing(HttpServletRequest request, HttpServletResponse response) throws Exception {

    User user = null;
    HttpSession session = request.getSession();
    PageContext pageContext=null;
    
    Map<String, String> initParams = (Map<String, String>) request.getAttribute("initParams");
    ContentletAPI conAPI = APILocator.getContentletAPI();
    ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(user);
    List<ContentType> contentTypes = null;
    if (initParams.get("content:baseTypes") != null) {
      for (String x : initParams.get("content:baseTypes").split(",")) {
        contentTypes.addAll(contentTypeAPI.findByType(BaseContentType.getBaseContentType(x)));
      }
      request.setAttribute("DONT_SHOW_ALL", true);
    } else if (initParams.get("content:types") != null) {
      for (String x : initParams.get("content:types").split(",")) {
        contentTypes.add(contentTypeAPI.find(x));
      }
      request.setAttribute("DONT_SHOW_ALL", true);
    } else {
      contentTypes = contentTypeAPI.findAll();
    }

    if (request.getParameter("selected_lang") != null) {
      session.setAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED, request.getParameter("selected_lang"));
    }

    List<Language> languages =APILocator.getLanguageAPI().getLanguages();


    String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED.toString(), ImmutableMap.of("struts_action", new String[] {"/ext/contentlet/view_contentlets"}));

    Map lastSearch = (Map) session.getAttribute(com.dotmarketing.util.WebKeys.CONTENTLET_LAST_SEARCH);
    final ContentType defaultType = contentTypeAPI.findDefault();
    final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
    Map<String, String> fieldsSearch = new HashMap<String, String>();
    Language selectedLanguage = new Language();
    List<String> categories = new ArrayList<>();
    boolean showDeleted = false;
    boolean filterSystemHost = false;
    boolean filterLocked = false;
    boolean filterUnpublish = false;
    int currpage = 1;
    String orderBy = "modDate desc";

    String languageId = String.valueOf(defaultLang.getId());
    if (session.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED) != null) {
      languageId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED);
    }

    String tryTypeId = "";
    if (UtilMethods.isSet(request.getParameter("structure_id"))) {
      tryTypeId = request.getParameter("structure_id");
    } else if (UtilMethods.isSet(request.getParameter("baseType"))) {
      tryTypeId = contentTypes.get(0).inode();
    }

    String schemeSelected = "catchall";
    if (UtilMethods.isSet(session.getAttribute(ESMappingConstants.WORKFLOW_SCHEME))) {
      schemeSelected = (String) session.getAttribute(ESMappingConstants.WORKFLOW_SCHEME);
    }

    String stepsSelected = "catchall";
    if (UtilMethods.isSet(session.getAttribute(ESMappingConstants.WORKFLOW_STEP))) {
      stepsSelected = (String) session.getAttribute(ESMappingConstants.WORKFLOW_STEP);
    }
    
    if (lastSearch != null && !UtilMethods.isSet(tryTypeId)) {
      ContentType typeSelected=null;
      String ssstruc = (String) session.getAttribute("selectedStructure");
      typeSelected=(ssstruc!=null) ? contentTypeAPI.find(ssstruc) : null;
      if (typeSelected != null) {
        if (!typeSelected.variable().equalsIgnoreCase("host") && contentTypes.contains(typeSelected)) {
          tryTypeId = typeSelected.id();
        } else {
          session.removeAttribute("selectedStructure");
          tryTypeId = null;;
        }
      }
      if (lastSearch.get("fieldsSearch") != null) {
        fieldsSearch = (Map<String, String>) lastSearch.get("fieldsSearch");
      }
      if (lastSearch.get("categories") != null) {
        categories = (List<String>) lastSearch.get("categories");
      }
      if (UtilMethods.isSet(lastSearch.get("showDeleted"))) {
        showDeleted = (Boolean) lastSearch.get("showDeleted");
      }
      if (UtilMethods.isSet(lastSearch.get("filterSystemHost"))) {
        filterSystemHost = (Boolean) lastSearch.get("filterSystemHost");
      }
      if (UtilMethods.isSet(lastSearch.get("filterLocked"))) {
        filterLocked = (Boolean) lastSearch.get("filterLocked");
      }
      if (lastSearch.get("filterUnpublish") != null)
        filterUnpublish = (Boolean) lastSearch.get("filterUnpublish");
      if (UtilMethods.isSet(lastSearch.get("page"))) {
        currpage = (Integer) lastSearch.get("page");
      }
      if (UtilMethods.isSet(lastSearch.get("orderBy"))) {
        orderBy = (String) lastSearch.get("orderBy");
      }

    }

    if (!UtilMethods.isSet(tryTypeId)) {
      if (session.getAttribute("selectedStructure") != null) {
        String longSelectedStructure = (String) session.getAttribute("selectedStructure");
        if (UtilMethods.isSet(longSelectedStructure)) {
          tryTypeId = longSelectedStructure.toString();
        }
      }
    }

    if (!UtilMethods.isSet(tryTypeId)
        || !contentTypes.contains(contentTypeAPI.find(tryTypeId))) {

      tryTypeId = "catchall";

    }
    ContentType typeSelected=contentTypeAPI.find(tryTypeId);
    List<Field> fields = new ArrayList<Field>();
    try {
      
      fields = typeSelected.fields();
    } catch (Exception e) {
      Logger.debug(this.getClass(), e.getMessage());
    }
    boolean hasNoSearcheableHostFolderField = false;
    boolean hasHostFolderField = false;
    for (Field field : fields) {
      if (field instanceof HostFolderField) {
        if (APILocator.getPermissionAPI().doesUserHavePermission(APILocator.getHostAPI().findSystemHost(), PermissionAPI.PERMISSION_READ,
            user, true)) {
          hasHostFolderField = true;
        }
        if (!field.searchable()) {
          hasNoSearcheableHostFolderField = true;
        }
        break;
      }
    }

    if (fieldsSearch == null || !UtilMethods.isSet(fieldsSearch.get("conHost")) || hasNoSearcheableHostFolderField) {
      fieldsSearch.put("conHost", (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID));
    }

    String crumbtrailSelectedHostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
    if ((crumbtrailSelectedHostId == null) || crumbtrailSelectedHostId.equals("allHosts"))
      crumbtrailSelectedHostId = "";

    String structureInodesList = "";
    String structureVelocityVarNames = "";

    for (ContentType type: contentTypes) {

      if (structureInodesList != "") {
        structureInodesList += ";" + type.id();
      } else
        structureInodesList += type.id();

      if (structureVelocityVarNames != "") {
        structureVelocityVarNames += ";" + type.variable();
      } else
        structureVelocityVarNames += type.variable();

    }

    String _allValue =
        (UtilMethods.webifyString(fieldsSearch.get("catchall")).endsWith("*"))
            ? UtilMethods.webifyString(fieldsSearch.get("catchall")).substring(0,
                UtilMethods.webifyString(fieldsSearch.get("catchall")).length() - 1)
            : UtilMethods.webifyString(fieldsSearch.get("catchall"));

    String[] strTypeNames = new String[] {"", LanguageUtil.get(pageContext, "Content"), LanguageUtil.get(pageContext, "Widget"),
        LanguageUtil.get(pageContext, "Form"), LanguageUtil.get(pageContext, "File"), LanguageUtil.get(pageContext, "HTMLPage"),
        LanguageUtil.get(pageContext, "Persona"), LanguageUtil.get(pageContext, "VanityURL"), LanguageUtil.get(pageContext, "KeyValue"),};

    final boolean enterprise = (LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level);
    final PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
    final List<PublishingEndPoint> sendingEndpointsList = pepAPI.getReceivingEndPoints();
    final boolean sendingEndpoints = UtilMethods.isSet(sendingEndpointsList) && !sendingEndpointsList.isEmpty();
    final boolean canReindexContentlets =
        APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadRoleByKey(Role.CMS_POWER_USER))
            || com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,
                com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());
  }

}
