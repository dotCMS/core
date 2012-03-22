<%@ include file="/html/portlet/ext/roleadmin/init.jsp" %>

<%
	RenderRequestImpl rreq = (RenderRequestImpl) pageContext.getAttribute("renderRequest");
	String portletId1 = rreq.getPortletName();
	
	if (!UtilMethods.isSet(portletId1))
		portletId1 = layouts[0].getPortletIds().get(0);
	
	Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
	String strutsAction = ParamUtil.get(request, "struts_action", null);
	
	if (!UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {
		List<CrumbTrailEntry> crumbTrailEntries = new ArrayList<CrumbTrailEntry>();
		
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
	}
	
	request.setAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS, false);

%>

<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>