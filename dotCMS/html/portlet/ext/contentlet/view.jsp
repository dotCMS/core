<%@ page import="java.util.*" %>
<%@ page import="javax.portlet.WindowState" %>
<%@ page import="com.dotmarketing.portlets.structure.factories.StructureFactory" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.business.PermissionAPI" %>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ include file="/html/portlet/ext/contentlet/init.jsp" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%
	List structures = StructureFactory.getStructures();
%>
<table border="0" cellpadding="4" cellspacing="0" width="100%">
<%
	PermissionAPI perAPI = APILocator.getPermissionAPI();
	Iterator strutsIt = structures.iterator();
	while (strutsIt.hasNext ()) {
		Structure structure = (Structure)strutsIt.next();
		boolean permRead = perAPI.doesUserHavePermission(structure,PermissionAPI.PERMISSION_READ, user);
		boolean permWrite = perAPI.doesUserHavePermission(structure,PermissionAPI.PERMISSION_WRITE, user);
		if (permRead) {
%>
<tr>
	<td width="5%"></td>
	<td align="left">
		<a class="beta" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name='struts_action' value='/ext/contentlet/view_contentlets' /><portlet:param name='structure_id' value='<%=structure.getInode()%>' /></portlet:renderURL>"><%= structure.getName() %></a>
	</td>
	<td align="left">
		<% if (permWrite) { %>
		(<a class="beta" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name='struts_action' value='/ext/contentlet/edit_contentlet' /><portlet:param name='cmd' value='new' /><portlet:param name='selectedStructure' value='<%=structure.getInode()%>' /><portlet:param name='allowChange' value='false' /></portlet:actionURL>">  <%= LanguageUtil.get(pageContext, "Add-New") %></a>)
		<% } %> 
	</td>
</tr>
<%		
		}
	}
%>
</table>

