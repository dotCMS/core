<%@ include file="/html/portlet/ext/languagesmanager/init.jsp" %>
<%@page import="com.dotmarketing.util.Config"%>
<!--table border="0" cellpadding="4" cellspacing="0" width="100%">
<tr class="beta">
	<td width="">
		<font class="beta" size="2"><a class="beta" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/languages_manager/view_languages_manager" /></portlet:renderURL>"><%= LanguageUtil.get(pageContext, "View-All-Languages") %></a></font>
	|	<font class="beta" size="2"><a class="beta" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString()%>"> <portlet:param name="struts_action" value="/ext/languages_manager/edit_language" /><portlet:param name="id" value="" /></portlet:actionURL>"><%= LanguageUtil.get(pageContext, "Add-New-Language") %></a></font>
	|	<font class="beta" size="2"><a class="beta" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/languages_manager/edit_language_keys" /><portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /></portlet:actionURL>"><%= LanguageUtil.get(pageContext, "Edit-Default-Language-Variables") %></a></font>
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
		
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "Editing-Language-Variables"), null));
		
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
	}
	
	request.setAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS, false);

%>

<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>