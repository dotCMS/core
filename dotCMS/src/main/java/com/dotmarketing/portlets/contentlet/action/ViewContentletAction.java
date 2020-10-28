package com.dotmarketing.portlets.contentlet.action;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.javax.portlet.PortletConfig;
import com.dotcms.repackage.javax.portlet.RenderRequest;
import com.dotcms.repackage.javax.portlet.RenderResponse;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.apache.struts.action.ActionForward;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portal.struts.DotPortletAction;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.RenderRequestImpl;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

/**
 * Struts action that retrieves the required information to display the "Content Search" portlet.
 *
 * @author root
 * @version 1.1
 * @since Mar 22, 2012
 *
 */
public class ViewContentletAction extends DotPortletAction {

  private final LanguageAPI langAPI = APILocator.getLanguageAPI();
  private final StructureAPI structureAPI = APILocator.getStructureAPI();

  /**
   * Retrieves a list of {@link Structure} objects based on specific filtering parameters coming in
   * the {@link RenderRequest} object. The resulting collection will be added as a request attribute
   * that will be processed by the JSP associated to this action.
   *
   * @param req - Portlet wrapper class for the HTTP request.
   * @param user - The {@link User} loading the portlet.
   * @throws Exception An error occurred when processing this request.
   */
  @SuppressWarnings("unchecked")
  protected void _viewContentlets(final RenderRequest req, final User user) throws Exception {
    // GIT-2816
    final RenderRequestImpl reqImpl = (RenderRequestImpl) req;
    final HttpServletRequest request = reqImpl.getHttpServletRequest();

    final ContentletAPI conAPI = APILocator.getContentletAPI();

    final List<String> tempBinaryImageInodes = (List<String>) request.getSession().getAttribute(Contentlet.TEMP_BINARY_IMAGE_INODES_LIST);
    if (UtilMethods.isSet(tempBinaryImageInodes) && tempBinaryImageInodes.size() > 0) {
      for (final String inode : tempBinaryImageInodes) {
        conAPI.delete(conAPI.find(inode, APILocator.getUserAPI().getSystemUser(), false), APILocator.getUserAPI().getSystemUser(), false,
            true);
      }
      tempBinaryImageInodes.clear();
    }

    List<ContentType> contentTypes = resolveContentTypes(request);
    if (contentTypes.size() == 1) {
      req.setAttribute("DONT_SHOW_ALL", true);
    }

    req.setAttribute("contentSearchContentTypes", contentTypes);
    req.setAttribute("selectedStructure", resolveStructureSelected(request, contentTypes));

    if (req.getParameter("selected_lang") != null) {
      ((RenderRequestImpl) req).getHttpServletRequest().getSession().setAttribute(WebKeys.LANGUAGE_SEARCHED,
          req.getParameter("selected_lang"));
    }
    final List<Language> languages = this.langAPI.getLanguages();
    req.setAttribute(WebKeys.LANGUAGES, languages);

  }

  /**
   *
   */
  @Override
  public ActionForward render(final ActionMapping mapping, final ActionForm form, final PortletConfig config, final RenderRequest req,
                              final RenderResponse res) throws Exception {

    Logger.debug(ViewContentletAction.class, "Running ViewContentletsAction!!!!");

    try {
      // gets the user
      final User user = _getUser(req);
      _viewContentlets(req, user);
      return mapping.findForward("portlet.ext.contentlet.view_contentlets");

    } catch (final Exception e) {
      Logger.debug(this,e.getMessage());
      req.setAttribute(PageContext.EXCEPTION, e);
      return mapping.findForward(Constants.COMMON_ERROR);
    }
  }

  private List<BaseContentType> resolveBaseTypes(HttpServletRequest request) {
    Map<String, String> initParams = (Map<String, String>) request.getAttribute("initParams");
    String baseTypesRaw = (initParams.get("baseTypes") != null) ? initParams.get("baseTypes") : request.getParameter("baseType");
    if (!UtilMethods.isSet(baseTypesRaw)) {
      return ImmutableList.of();
    }
    String[] baseTypes = baseTypesRaw.trim().split(",");
    List<BaseContentType> baseTypeList = new ArrayList<>();
    for (String type : baseTypes) {
      if (UtilMethods.isSet(type) && UtilMethods.isNumeric(type)) {
        baseTypeList.add(BaseContentType.getBaseContentType(Integer.parseInt(type)));
      } else {
        baseTypeList.add(BaseContentType.getBaseContentType(type.trim()));
      }
    }
    return baseTypeList;
  }

  List<ContentType> resolveContentTypes(HttpServletRequest request) throws DotDataException, DotSecurityException {
    Map<String, String> initParams = (Map<String, String>) request.getAttribute("initParams");
    String contentTypesRaw = initParams.getOrDefault("contentTypes", request.getParameter("structure_id"));
    ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(PortalUtil.getUser(request));
    List<BaseContentType> baseTypes = resolveBaseTypes(request);
   
    final List<ContentType> contentTypesList = new ArrayList<>();
    boolean addAll = false;
    if (baseTypes.size() > 0) {
      for (BaseContentType type : baseTypes) {
        contentTypesList.addAll(contentTypeApi.findByType(type));
      }
    }
    else if (UtilMethods.isSet(contentTypesRaw)) {
      String[] contentTypes = contentTypesRaw.trim().split(",");

      for (String type : contentTypes) {
          ContentType contentType = Try.of(() -> APILocator.getContentTypeAPI(PortalUtil.getUser(request)).find(type.trim())).getOrNull();
          if (contentType != null) {
              contentTypesList.add(contentType);
          }
      }

    }else {
        contentTypesList.addAll(contentTypeApi.findAll());
        addAll = true;
    }

    contentTypesList.removeIf(t->t.variable().equalsIgnoreCase("forms"));

    if (!addAll) {
        request.setAttribute("contentTypesJs", buildJsArray(contentTypesList));
    }
    return contentTypesList;


  }

  private String buildJsArray(List<ContentType> types) {
    return String.join(",", types.stream().map(t ->  t.id() ).collect(Collectors.toList()));

  }

  private String resolveStructureSelected(final HttpServletRequest request, final List<ContentType> types) {
    Portlet portlet = APILocator.getPortletAPI().findPortlet(request.getParameter("p_p_id"));
    String prefix = (portlet != null) ? portlet.getPortletId() : "";

    String selectedStructure = types.size() == 1 ? types.get(0).id()
        : UtilMethods.isSet(request.getParameter("structure_id")) ? request.getParameter("structure_id")
            : request.getSession().getAttribute(prefix + "selectedStructure") != null
                ? (String) request.getSession().getAttribute(prefix + "selectedStructure")
                : null;
    if (selectedStructure != null) {
      for (ContentType s : types) {
        if (s.id().equals(selectedStructure)) {
          return selectedStructure;
        }
      }
    }
    return "catchall";

  }

}
