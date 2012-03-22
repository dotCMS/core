<%@ include file="/html/portlet/ext/containers/init.jsp" %>


<table border="0" cellpadding="4" cellspacing="0" width="100%">
<Tr>
	<td align="center">
	<font class="gamma" size="2">
	<a class="bg" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/view_containers" /></portlet:renderURL>">
	<%= LanguageUtil.get(pageContext, "View-Containers1") %></a>
	</font>
	</td>
</tr>
<Tr>
	<td align="center">
	<font class="gamma" size="2">
	<a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/containers/edit_container" /></portlet:actionURL>">
	<%= LanguageUtil.get(pageContext, "Add-New-Container") %></a>
	</font>
	</td>
</tr>
</table>
