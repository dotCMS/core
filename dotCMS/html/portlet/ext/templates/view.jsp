<%@ include file="/html/portlet/ext/templates/init.jsp" %>

<table border="0" cellpadding="4" cellspacing="0" width="100%">
<Tr>
	<td align="center">
	<font class="gamma" size="2">
	<a class="bg" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/view_templates" /></portlet:renderURL>">
	<%= LanguageUtil.get(pageContext, "View-Templates") %></a>
	</font>
	</td>
</tr>
<Tr>
	<td align="center">
	<font class="gamma" size="2">
	<a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/templates/edit_template" /></portlet:actionURL>">
	<%= LanguageUtil.get(pageContext, "Add-New-Template") %></a>
	</font>
	</td>
</tr>
</table>

