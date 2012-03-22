<%@ include file="/html/portlet/ext/dashboard/init.jsp" %>


<%
	RenderRequestImpl rreq = (RenderRequestImpl) pageContext.getAttribute("renderRequest");
	String portletId1 = rreq.getPortletName();
	
	if (!com.dotmarketing.util.UtilMethods.isSet(portletId1))
		portletId1 = layouts[0].getPortletIds().get(0);
	
	Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
	String strutsAction = ParamUtil.get(request, "struts_action", null);
	String cmd = ParamUtil.get(request, "cmd", null);
	
	
	
	if (!com.dotmarketing.util.UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {
		Map params = new HashMap();
		params.put("struts_action", new String[] {"/ext/dashboard/view_dashboard"});
		String crumbTrailReferer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, WindowState.MAXIMIZED.toString(), params);
		List<CrumbTrailEntry> crumbTrailEntries = new ArrayList<CrumbTrailEntry>();
		String hostId = request.getParameter("hostId");
		if(UtilMethods.isSet(cmd) && cmd.equals(Constants.VIEW_ACTIVITY_STREAM )){
			crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), crumbTrailReferer));
			if(UtilMethods.isSet(hostId)){
				  Host currentHost = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(),false);
				  params.put("struts_action", new String[] {"/ext/dashboard/view_dashboard"});
				  params.put("cmd", new String[] {Constants.VIEW_HOST_REPORT});
	              crumbTrailReferer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, WindowState.MAXIMIZED.toString(), params);
				  crumbTrailEntries.add(new CrumbTrailEntry(currentHost.getHostname(), crumbTrailReferer + "&hostId="+hostId));
			}
			 crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "work-stream"), null));
		}else if(UtilMethods.isSet(cmd) && cmd.equals(Constants.VIEW_BROWSER )){
			crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + "EXT_BROWSER"), crumbTrailReferer));
		}else if(UtilMethods.isSet(cmd) && cmd.equals(Constants.VIEW_FILES )){	
			crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + "EXT_3"), crumbTrailReferer));
		}else if(UtilMethods.isSet(cmd) && cmd.equals(Constants.VIEW_PAGES )){	
			crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + "EXT_15"), crumbTrailReferer));
		}else{
		if(UtilMethods.isSet(hostId)){
		  crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), crumbTrailReferer));
		  Host currentHost = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(),false);
		  crumbTrailEntries.add(new CrumbTrailEntry(currentHost.getHostname(), null));
		}else{
			crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), null));
		}
		}
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
	}
	
	request.setAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS, false);

%>

<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>