<%@ page import="com.dotmarketing.exception.DotDataException"%>

<%
  if (portlet == null) {
    //Added some debug and error handling code....
    throw new IllegalArgumentException("Passed portlet in html/portal/view_portlet_inc.jsp is null");
  }

  boolean access = false;
  try {
    String defaultAccessPortletId = request.getParameter(WebKeys.PORTLET_URL_CURRENT_ANGULAR_PORTLET) != null
    ? request.getParameter(WebKeys.PORTLET_URL_CURRENT_ANGULAR_PORTLET)
    : portlet.getPortletId();
    access = APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(defaultAccessPortletId, user);

    if (!access && !portlet.getPortletId().equals(defaultAccessPortletId)) {
  access = APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(portlet.getPortletId(), user);
    }
  } catch (DotDataException e) {
    Logger.error(this.getClass(), String.format("Exception on view_portlet_inc.jsp, portletId: %s angularPortletId: %s",
    portlet.getPortletId(), request.getParameter(WebKeys.PORTLET_URL_CURRENT_ANGULAR_PORTLET)), e);
  }

  String licenseManagerOverrideTicket = request.getParameter("licenseManagerOverrideTicket");
  String roleAdminOverrideTicket = request.getParameter("roleAdminOverrideTicket");
  if (roleAdminOverrideTicket != null) {
    if (request.getSession().getAttribute("roleAdminOverrideTicket") != null) {
  String overrideTicket = (String) request.getSession().getAttribute("roleAdminOverrideTicket");
  if (overrideTicket != null && roleAdminOverrideTicket.equalsIgnoreCase(overrideTicket)) {
    access = true;
  }
    }
  }

  if (licenseManagerOverrideTicket != null) {
    if (request.getSession().getAttribute("licenseManagerOverrideTicket") != null) {
  String overrideTicket = (String) request.getSession().getAttribute("licenseManagerOverrideTicket");
  if (overrideTicket != null && licenseManagerOverrideTicket.equalsIgnoreCase(overrideTicket)) {
    access = true;
  }
    }
  }

  ConcretePortletWrapper cachePortlet = (ConcretePortletWrapper) APILocator.getPortletAPI().getImplementingInstance(portlet);;


  PortletPreferences portletPrefs = null;

  PortletConfig portletConfig = APILocator.getPortletAPI().getPortletConfig(portlet);
  PortletContext portletCtx = portletConfig.getPortletContext();

  // Passing in a dynamic request with box_width makes it easier to render
  // portlets, but will break the TCK because the request parameters will have an
  // extra parameter

  RenderRequestImpl renderRequest =
  new RenderRequestImpl(request, portlet, cachePortlet, portletCtx, null, null, portletPrefs, layoutId);

  StringServletResponse stringServletRes = new StringServletResponse(response);

  RenderResponseImpl renderResponse =
  new RenderResponseImpl(renderRequest, stringServletRes, portlet.getPortletId(), company.getCompanyId(), layoutId);

  renderRequest.defineObjects(portletConfig, renderResponse);

  int portletTitleLength = 12;

  Map portletViewMap = CollectionFactory.getHashMap();

  portletViewMap.put("access", new Boolean(access));
  portletViewMap.put("active", new Boolean(portlet.isActive()));

  portletViewMap.put("portletId", portlet.getPortletId());
  portletViewMap.put("portletTitleLength", new Integer(portletTitleLength));

  renderRequest.setAttribute(WebKeys.PORTLET_VIEW_MAP, portletViewMap);

  if ((cachePortlet != null) && cachePortlet.isStrutsPortlet()) {

    // Make sure the Tiles context is reset for the next portlet

    request.removeAttribute(com.dotcms.repackage.org.apache.struts.taglib.tiles.ComponentConstants.COMPONENT_CONTEXT);
  }

  boolean portletException = false;

  cachePortlet.render(renderRequest, renderResponse);

  SessionMessages.clear(renderRequest);
  SessionErrors.clear(renderRequest);

  if ((cachePortlet != null) && cachePortlet.isStrutsPortlet()) {
    if (!access || portletException) {
  PortletRequestProcessor portletReqProcessor = (PortletRequestProcessor) portletCtx.getAttribute(WebKeys.PORTLET_STRUTS_PROCESSOR);

  ActionMapping actionMapping =
  portletReqProcessor.processMapping(request, response, (String) portlet.getInitParams().get("view-action"));

  ComponentDefinition definition = null;

  if (actionMapping != null) {

    // See action path /weather/view

    String definitionName = actionMapping.getForward();

    if (definitionName == null) {

  // See action path /journal/view_articles

  String[] definitionNames = actionMapping.findForwards();

  for (int definitionNamesPos = 0; definitionNamesPos < definitionNames.length; definitionNamesPos++) {
    if (definitionNames[definitionNamesPos].endsWith("view")) {
      definitionName = definitionNames[definitionNamesPos];

      break;
    }
  }

  if (definitionName == null) {
    definitionName = definitionNames[0];
  }
    }

    definition = TilesUtil.getDefinition(definitionName, request, application);
  }

  String templatePath = Constants.TEXT_HTML_DIR + "/portal/layout_portal.jsp";
  if (definition != null) {
    templatePath = Constants.TEXT_HTML_DIR + definition.getPath();
  }
%>

<%@page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.util.Logger"%>
<jsp:include page="/html/portal/portlet_error.jsp"></jsp:include>
<%
  } else {

      pageContext.getOut().print(stringServletRes.getString());

    }
  } else {
    renderRequest.setAttribute(WebKeys.PORTLET_CONTENT, stringServletRes.getString());

    String portletContent = StringPool.BLANK;
    if (portletException) {
      portletContent = "/portal/portlet_error.jsp";
    }

    if (!statePopUp || portletException) {
%>
<jsp:include page="/html/portal/portlet_error.jsp"></jsp:include>
<%
  } else {
%>
<%=renderRequest.getAttribute(WebKeys.PORTLET_CONTENT)%>
<%
  }
  }
%>

