<%@ page import="com.dotcms.repackage.javax.portlet.WindowState" %>
<%@ include file="/html/portlet/ext/structure/init.jsp" %>
<script language="javascript">
function addNewFormSubNav()
{
	var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	href = href + "<portlet:param name='struts_action' value='/ext/structure/edit_structure' />";
	href = href + "<portlet:param name='cmd' value='null' />";
	href = href + "</portlet:actionURL>";			
	document.location.href = href;
}

function allFormsSubNav()
{
	var href = "<portlet:actionURL windowState='<%=WindowState.MAXIMIZED.toString()%>'>";
	href = href + "</portlet:actionURL>";			
	document.location.href = href;
}

</script>
<!--table border="0" cellpadding="4" cellspacing="0" width="100%">
<tr class="beta">
	<td>
		<a class="gamma" href="#" onCLick="allFormsSubNav();"><%= LanguageUtil.get(pageContext, "View-all-forms") %></a> | <a class="gamma" href="#" onCLick="addNewFormSubNav();" ><%= LanguageUtil.get(pageContext, "Add-New-Form") %></a> 
	</td>
</tr>
</table-->

<%
	RenderRequestImpl rreq = (RenderRequestImpl) pageContext.getAttribute("renderRequest");
	String portletId1 = rreq.getPortletName();
	
	if (!UtilMethods.isSet(portletId1))
		portletId1 = layouts[0].getPortletIds().get(0);
	
	Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
	String strutsAction = ParamUtil.get(request, "struts_action", null);
	
	if (!UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {
		List<CrumbTrailEntry> crumbTrailEntries = new ArrayList<CrumbTrailEntry>();
		
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title." + portletId1), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
	}
	
	request.setAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS, false);

%>

<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>