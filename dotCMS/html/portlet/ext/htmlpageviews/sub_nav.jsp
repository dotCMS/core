<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ include file="/html/portlet/ext/htmlpageviews/init.jsp" %>

<%--
	String me = "HtmlpageViews"; 
--%>

<script type="text/javascript">
<!--
function doSearchPageStatistics() {
	var pageURL = document.getElementById('pageURL').value;
	window.location = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpageviews/view_htmlpage_views" /></portlet:renderURL>&pageURL='+pageURL;
}
//-->
</script>
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
		Host currentHost = APILocator.getHostAPI().find(_crumbHost, user, false);
		if(currentHost != null){
			_crumbHostname = currentHost.getHostname();
			if(UtilMethods.isSet(_crumbHostname)){
				cTrail.add(new CrumbTrailEntry(_crumbHostname, "javascript:showHostPreview();"));
			}
		}
	}else if("allHosts".equals(_crumbHost)){
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "All-Hosts"), "javascript:showHostPreview();"));
	}
	if(cTrail.size() <1){
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "No-Host-Permission"), "#"));
	}
	
	
	
	
	
	if (!UtilMethods.isSet(strutsAction) || strutsAction.equals("/ext/htmlpageviews/view_htmlpage_views")) {

		Map params = new HashMap();
		params.put("struts_action", new String[] {"/ext/browser/view_browser"});
		String crumbTrailReferer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, WindowState.MAXIMIZED.toString(), params);
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), crumbTrailReferer));
		
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "modes.Page-Statistics"), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, cTrail);
	}
	
	request.setAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS, false);

%>

<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>