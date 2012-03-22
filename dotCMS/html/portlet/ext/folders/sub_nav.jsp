<%@ include file="/html/portlet/ext/folders/init.jsp" %>
<%
	request.setAttribute("SHOW_HOST_SELECTOR", new Boolean(true));
	RenderRequestImpl rreq = (RenderRequestImpl) pageContext.getAttribute("renderRequest");
	String portletId1 = rreq.getPortletName();
	
	if (!UtilMethods.isSet(portletId1))
		portletId1 = layouts[0].getPortletIds().get(0);
	
	Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
	String strutsAction = ParamUtil.get(request, "struts_action", null);
	List<CrumbTrailEntry> cTrail = new ArrayList<CrumbTrailEntry>();
	String _crumbHost = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	String _crumbHostname = null;
	if(UtilMethods.isSet(_crumbHost) && !_crumbHost.equals("allHosts")) {
		Host currentHost = APILocator.getHostAPI().find(_crumbHost, APILocator.getUserAPI().getSystemUser(), false);
		_crumbHostname = currentHost.getHostname();
		if(UtilMethods.isSet(_crumbHostname)){
			cTrail.add(new CrumbTrailEntry(_crumbHostname, "javascript:showHostPreview();"));
		}
	}else if("allHosts".equals(_crumbHost)){
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "All-Hosts"), "javascript:showHostPreview();"));
	}
	
	if (!UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {

		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, cTrail);
	} else {

		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), "javascript: goBack();"));
		
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "edit-folder"), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, cTrail);
	}
%>

<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>