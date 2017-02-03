<%@ include file="/html/portlet/ext/files/init.jsp" %>

<table border="0" cellpadding="4" cellspacing="0" width="100%">
<Tr>
	<td align="center">
	<font class="gamma" size="2">
	<a class="bg" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/view_files" /></portlet:renderURL>">
	<%= LanguageUtil.get(pageContext, "View-Files") %></a>
	</font>
	</td>
</tr>
<Tr>
	<td align="center">
	<font class="gamma" size="2">
	<a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/files/edit_file" /></portlet:actionURL>">
	<%= LanguageUtil.get(pageContext, "Add-New-File") %> </a>
	</font>
	</td>
</tr>
</table>