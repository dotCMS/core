<%@ page import="javax.portlet.WindowState" %>
<%@ include file="/html/portlet/ext/structure/init.jsp" %>
<script language="javascript">
function addNewStructureSubNav()
{
	var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_structure' />";
	href = href + "<portlet:param name='cmd' value='null' />";
	href = href + "</portlet:actionURL>";			
	document.location.href = href;
}

function allStructureSubNav()
{
	var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	href = href + "</portlet:actionURL>";			
	document.location.href = href;
}

function addNewRelationshipSubNav()
{
	var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_relationship' />";
	href = href + "</portlet:actionURL>";			
	document.location.href = href;
}

function allRelationshipsSubNav()
{
	var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	href = href + "<portlet:param name='struts_action' value='/ext/structure/view_relationships' />";
	href = href + "</portlet:actionURL>";			
	document.location.href = href;
}
</script>
<!--table border="0" cellpadding="4" cellspacing="0" width="100%">
<tr class="beta">
	<td>
		<a class="gamma" href="#" onCLick="allStructureSubNav();"><%= LanguageUtil.get(pageContext, "View-all-Structures") %></a> | <a class="gamma" href="#" onCLick="addNewStructureSubNav();" ><%= LanguageUtil.get(pageContext, "Add-New-Structure") %></a> | <a class="gamma" href="#" onCLick="allRelationshipsSubNav();"><%= LanguageUtil.get(pageContext, "View-all-Relationships") %></a> | <a class="gamma" href="#" onCLick="addNewRelationshipSubNav();" ><%= LanguageUtil.get(pageContext, "Add-New-Relationship") %></a>
	</td>
</tr>
</table-->

<%
	RenderRequestImpl rreq = (RenderRequestImpl) pageContext.getAttribute("renderRequest");
	String portletId1 = rreq.getPortletName();
	List<CrumbTrailEntry> entries = new ArrayList<CrumbTrailEntry>();
	
	if (!UtilMethods.isSet(portletId1))
		portletId1 = layouts[0].getPortletIds().get(0);
	
	Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
	String strutsAction = ParamUtil.get(request, "struts_action", null);
	
	if (!UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), null));
	} else if (strutsAction.equals("/ext/structure/edit_structure") && portlet1.getPortletId().equals("EXT_STRUCTURE")) {
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), "javascript: cancel();"));
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Add-Edit-Structure"), null));
	} else if (strutsAction.equals("/ext/structure/edit_structure") && portlet1.getPortletId().equals("EXT_FORM_HANDLER")) {
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), "javascript: allStructureSubNav();"));
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Edit-Form"), null));
	} else if (strutsAction.equals("/ext/structure/edit_field") && portlet1.getPortletId().equals("EXT_STRUCTURE")) {
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), "javascript: allStructureSubNav();"));
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Add-Edit-Structure"), "javascript: cancel();"));
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Add-Edit-Field"), null));
	} else if (strutsAction.equals("/ext/structure/edit_field") && portlet1.getPortletId().equals("EXT_FORM_HANDLER")) {
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), "javascript: allStructureSubNav();"));
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Edit-Form"), "javascript: cancel();"));
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Add-Edit-Field"), null));
	} else if(strutsAction.equals("/ext/structure/view_relationships") && portlet1.getPortletId().equals("EXT_STRUCTURE")) {
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Relationships"), null));
	} else if(strutsAction.equals("/ext/structure/edit_relationship") && portlet1.getPortletId().equals("EXT_STRUCTURE")) {
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Relationships"), "javascript: allRelationshipsSubNav();"));
		entries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Add/Edit-Relationship"), null));
	}
	
	request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, entries);
	request.setAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS, false);

%>

<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>