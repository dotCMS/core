<%@ include file="/html/portlet/ext/userclicks/init.jsp" %>

<table border="0" cellpadding="4" cellspacing="0" width="100%">
<Tr>
	<td align="center">
	<font class="gamma" size="2">
	<a class="bg" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/view_htmlpages" /></portlet:renderURL>">
		<%= LanguageUtil.get(pageContext, "Choose-User-to-view-statistics-on") %></a>
	</font>
	</td>
</tr>
</table>
