<%@ include file="/html/portlet/ext/cmsmaintenance/init.jsp"%>
<%@ page import="javax.portlet.WindowState"%>

<form action="" id="viewMaintenanceForm" name="viewMaintenanceForm">
	<a href="<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'><portlet:param name="struts_action" value='/ext/cmsmaintenance/view_cms_maintenance' /></portlet:renderURL>"><%= LanguageUtil.get(pageContext,"View-Maintenance-Tasks") %></a>	
</form>
