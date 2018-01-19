<%@ include file="/html/portlet/ext/contentlet/init.jsp" %>
<portlet:defineObjects />
<%@page import="com.dotmarketing.portlets.contentlet.struts.ContentletForm"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotmarketing.portlets.contentlet.struts.ContentletForm"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<script type="text/javascript">
function viewFormsList()
{
	var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	href = href + "</portlet:actionURL>";
	document.location.href = href;
}
</script>
<%
    request.setAttribute("SHOW_HOST_SELECTOR", new Boolean(true));
	RenderRequestImpl rreq = (RenderRequestImpl) pageContext.getAttribute("renderRequest");
	String portletId1 = rreq.getPortletName();
	
	if (!UtilMethods.isSet(portletId1))
		portletId1 = layouts[0].getPortletIds().get(0);
	
	Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
	String strutsAction = ParamUtil.get(request, "struts_action", null);
	ContentletAPI conAPI = APILocator.getContentletAPI();
	
	String cmd = ParamUtil.get(request, "struts_action", null);
	List<CrumbTrailEntry> cTrail = new ArrayList<CrumbTrailEntry>();
	com.dotmarketing.portlets.contentlet.model.Contentlet hostContentlet = null;
	ContentletForm hostContentletForm = null;
	String inode=request.getParameter("inode");
	String _crumbHost = "";
	if(UtilMethods.isSet(inode)){
	    request.removeAttribute("SHOW_HOST_SELECTOR");
		hostContentlet = conAPI.find(inode,user,false);
		hostContentletForm = (ContentletForm) request.getAttribute("ContentletForm");
		try{
		    Structure structure = hostContentletForm.getStructure();
			if(structure.getVelocityVarName().equals("Host")) {	
				_crumbHost = hostContentlet.getIdentifier();
				request.setAttribute("_crumbHost",_crumbHost);
				
			}
		}
		catch(Exception e){
			// no host here in the request
			
		}
	}
	if(!UtilMethods.isSet(_crumbHost)){
	    _crumbHost = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
	}
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
	
	
	
	if (!UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {

		
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title." + portletId1), null));
		

	} else if (strutsAction.equals("/ext/contentlet/import_contentlets")) {

		Map params = new HashMap();
		params.put("struts_action", new String[] {"/ext/contentlet/view_contentlets"});
		String crumbTrailReferer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, WindowState.MAXIMIZED.toString(), params);
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title." + portletId1), crumbTrailReferer));
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "import-contentlet"), null));

	} else if (portlet1.getPortletId().equals("forms")) {
		
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title." + portletId1), "javascript: viewFormsList();"));

		if(strutsAction.equals("/ext/contentlet/view_contentlets")){
			
			if (UtilMethods.isSet(request.getAttribute("ContentletForm"))) {
				ContentletForm contentletForm = (ContentletForm) request.getAttribute("ContentletForm");
				cTrail.add(new CrumbTrailEntry(contentletForm.getStructure().getName(), null));
			} else {
				cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Form"), null));
			}
			
		}else if(strutsAction.equals("/ext/contentlet/edit_contentlet")){

			if (UtilMethods.isSet(request.getAttribute("ContentletForm"))) {
				ContentletForm contentletForm = (ContentletForm) request.getAttribute("ContentletForm");
				cTrail.add(new CrumbTrailEntry(contentletForm.getStructure().getName(), "javascript: cancelEdit();"));
			} else {
				cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Form"), "javascript: cancelEdit();"));
			}

			if (UtilMethods.isSet(request.getAttribute("ContentletForm"))) {
				ContentletForm contentletForm = (ContentletForm) request.getAttribute("ContentletForm");
				cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "add-edit") + " " + contentletForm.getStructure().getName(), null));
			} else {
				cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "edit-contentlet"), null));
			}

		}

	} else {
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title." + portletId1), "javascript: cancelEdit();"));
		
		if (UtilMethods.isSet(request.getAttribute("ContentletForm"))) {
			ContentletForm contentletForm = (ContentletForm) request.getAttribute("ContentletForm");
			cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "add-edit") + " " + UtilMethods.webifyString(contentletForm.getStructure().getName()), null));
		} else {
			cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "edit-contentlet"), null));
		}

	}
	request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, cTrail);
%>
<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>