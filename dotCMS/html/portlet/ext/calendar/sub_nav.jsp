<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="com.dotmarketing.cache.FieldsCache"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@ include file="/html/portlet/ext/calendar/init.jsp" %>

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
	
	if (!UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), null));
	} else {
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), "javascript: cancelEdit();"));
		
		cTrail.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "edit-event"), null));
	}
	
	request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, cTrail);
	
	Structure eventStructure = APILocator.getEventAPI().getEventStructure();
	List<Field> fields = FieldsCache.getFieldsByStructureInode(eventStructure.getInode());
	boolean hasHostFolder = false;
	
	if (0 < fields.size()) {
		String hostFolderType = Field.FieldType.HOST_OR_FOLDER.toString();
		
		for (Field field: fields) {
			if (field.getFieldType().equals(hostFolderType)) {
				hasHostFolder = true;
				break;
			}
		}
	}
	
	request.setAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS, false);

%>

<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>