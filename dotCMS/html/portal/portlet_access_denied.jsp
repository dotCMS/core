
portlet_access_denied.jsp

<%@ include file="/html/portal/init.jsp" %>

<table border="0" cellpadding="4" cellspacing="0" width="100%" height="300">
<tr>
	<td align="center">
		<table border="0" cellpadding="8" cellspacing="0">
		<tr>
			<td>
				<font class="gamma" size="2"><span class="gamma-neg-alert">
				<%= LanguageUtil.get(pageContext, "you-do-not-have-the-roles-required-to-access-this-portlet") %>
				</span></font>
			</td>
		</tr>
		</table>
	</td>
</tr>
</table>