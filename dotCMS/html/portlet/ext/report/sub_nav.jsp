<%@ include file="/html/portlet/ext/report/init.jsp" %>
<script language="JavaScript">
	function openFormBuilder(){
		window.open("/html/dotCMS_form_builder/form_builder_2_7.html", "formwindow", "menubar=1, scrollbars=1, resizable=1, width=640, height=500");
	}
</script>

<%
	boolean editor = (Boolean)request.getAttribute(com.dotmarketing.portlets.report.action.ViewReportsAction.REPORT_EDITOR_OR_ADMIN);
%>

<%@page import="com.dotmarketing.portlets.report.action.ViewReportsAction"%>
<!--table border="0" cellpadding="4" cellspacing="0" width="100%">
<tr class="beta">
	<td nowrap="nowrap">
		<a class="beta" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/report/view_reports" /></portlet:renderURL>"><%= LanguageUtil.get(pageContext, "View-Reports") %></a>
		<% if(editor){ %>
		| <a class="beta" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/report/edit_report" /></portlet:actionURL>"><%= LanguageUtil.get(pageContext, "Add-New-Report") %></a>
		| <a class="beta" href="javascript: openFormBuilder()" ><%= LanguageUtil.get(pageContext, "Open-Form-Builder") %></a>
		<%} %>
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
		
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
	} else {
		List<CrumbTrailEntry> crumbTrailEntries = new ArrayList<CrumbTrailEntry>();
		
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), "javascript: cancelEdit();"));
		
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Edit-Report"), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
	}
	
	request.setAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS, false);

%>

<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>