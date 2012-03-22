<%@ include file="/html/portlet/ext/links/init.jsp" %>

<table border="0" cellpadding="4" cellspacing="0" width="100%">
<Tr>
	<td align="center">
	<font class="gamma" size="2">
	<a class="bg" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/view_links" /></portlet:renderURL>">
	<%= LanguageUtil.get(pageContext, "View-Links") %></a>
	</font>
	</td>
</tr>
<Tr>
	<td align="center">
	<font class="gamma" size="2">
	<a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/links/edit_link" /></portlet:actionURL>">
	<%= LanguageUtil.get(pageContext, "Add-New-Link") %></a>
	</font>
	</td>
</tr>
</table>