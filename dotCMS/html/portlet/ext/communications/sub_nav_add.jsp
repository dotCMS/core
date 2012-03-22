<%@ include file="/html/portlet/ext/communications/init.jsp" %>

<table border="0" cellpadding="4" cellspacing="0" width="100%">
<tr class="beta">
	<td width="">

		<font class="beta" size="2"><a class="beta" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/communications/view_communications" /></portlet:renderURL>&query=&orderby=">
		<%=LanguageUtil.get(pageContext, "View-All-Communications")%></a></font>
		|   <font class="beta" size="2"><a class="beta" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/communications/edit_communication" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>">
		<%=LanguageUtil.get(pageContext, "Create-New-Communication")%></a></font>
		|   <font class="beta" size="2">
		<%=LanguageUtil.get(pageContext, "Delete-Selected")%></font>
	</td>
</tr>
</table>
