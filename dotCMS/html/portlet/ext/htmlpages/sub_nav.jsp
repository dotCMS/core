<%@ include file="/html/portlet/ext/htmlpages/init.jsp" %>

<%
	request.setAttribute("SHOW_HOST_SELECTOR", new Boolean(true));
	RenderRequestImpl rreq = (RenderRequestImpl) pageContext.getAttribute("renderRequest");
	String portletId1 = rreq.getPortletName();
	
	if (!UtilMethods.isSet(portletId1))
		portletId1 = layouts[0].getPortletIds().get(0);
	
	Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
	String strutsAction = ParamUtil.get(request, "struts_action", null);
	String referer = request.getParameter("referer");
	if (UtilMethods.isSet(referer))
		referer = UtilMethods.decodeURL(UtilMethods.decodeURL(referer));
	
	
	
	List<CrumbTrailEntry> cTrail = new ArrayList<CrumbTrailEntry>();
	String _crumbHost = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	String _crumbHostname = null;
	if(UtilMethods.isSet(_crumbHost) && !_crumbHost.equals("allHosts")) {
		Host currentHost = APILocator.getHostAPI().find(_crumbHost, user, false);
		if(currentHost != null){
			_crumbHostname = currentHost.getHostname();
			if(UtilMethods.isSet(_crumbHostname)){
				cTrail.add(new CrumbTrailEntry(_crumbHostname, "javascript:showHostPreview();"));
			}
		}
	}
	else if("allHosts".equals(_crumbHost)){
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "All-Hosts"), "javascript:showHostPreview();"));
	}
	if(cTrail.size() <1){
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "No-Host-Permission"), "#"));
	}
	
	
	
	
	if (!UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {

		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, cTrail);
	} else if (portletId1.equals("EXT_15")) {
		HTMLPage htmlpage;
		if (request.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_EDIT)!=null) {
			htmlpage = (HTMLPage) request.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_EDIT);
		}
		else {
			htmlpage = (HTMLPage) InodeFactory.getInode(request.getParameter("inode"),HTMLPage.class);
		}
		java.util.Map params = new java.util.HashMap();
		params.put("struts_action", new String[] {"/ext/htmlpages/view_htmlpages"});
		String portlet1Referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED.toString(), params);
		portlet1Referer = UtilMethods.encodeURL(portlet1Referer);
		

		params = new HashMap();
		params.put("struts_action", new String[] {"/ext/htmlpages/edit_htmlpage"});
		params.put("cmd", new String[] {"unlock"});
		params.put("inode", new String[] {String.valueOf(htmlpage.getInode())});
		String crumbTrailReferer = com.dotmarketing.util.PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED.toString(), params);
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), crumbTrailReferer + "&referer=" + portlet1Referer));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, cTrail);
		
		if (referer.contains("/ext/htmlpages/preview_htmlpage")) {
			cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "modes.Preview"), "javascript: cancelEdit();"));
		}
		
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "edit-htmlpage"), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, cTrail);
	} else if (portletId1.equals("EXT_BROWSER")) {
		HTMLPage htmlpage;
		if (request.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_EDIT)!=null) {
			htmlpage = (HTMLPage) request.getAttribute(com.dotmarketing.util.WebKeys.HTMLPAGE_EDIT);
		}
		else {
			htmlpage = (HTMLPage) InodeFactory.getInode(request.getParameter("inode"),HTMLPage.class);
		}
		java.util.Map params = new java.util.HashMap();
		params.put("struts_action", new String[] {"/ext/browser/view_browser"});
		String portlet1Referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED.toString(), params);
		portlet1Referer = UtilMethods.encodeURL(portlet1Referer);
		

		params = new HashMap();
		params.put("struts_action", new String[] {"/ext/htmlpages/edit_htmlpage"});
		params.put("cmd", new String[] {"unlock"});
		params.put("inode", new String[] {String.valueOf(htmlpage.getInode())});
		String crumbTrailReferer = com.dotmarketing.util.PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED.toString(), params);
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), crumbTrailReferer + "&referer=" + portlet1Referer));
		
		if ((referer != null) && (referer.contains("/ext/htmlpages/preview_htmlpage"))) {
			cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "modes.Preview"), "javascript: cancelEdit();"));
		}
		
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "edit-htmlpage"), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, cTrail);
	}
%>

<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>