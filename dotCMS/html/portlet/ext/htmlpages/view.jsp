<%@ include file="/html/portlet/ext/htmlpages/init.jsp" %>

<table border="0" cellpadding="4" cellspacing="0" width="100%">
<Tr>
	<td align="center">
	<font class="gamma" size="2">
	<a class="bg" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/view_htmlpages" /></portlet:renderURL>">
	<%= LanguageUtil.get(pageContext, "View-HTML-Pages") %></a>
	</font>
	</td>
</tr>
<Tr>
	<td align="center">
	<font class="gamma" size="2">
	<a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/htmlpages/edit_htmlpage" /></portlet:actionURL>">
	<%= LanguageUtil.get(pageContext, "Add-New-HTML-Page") %></a>
	</font>
	</td>
</tr>
</table>

