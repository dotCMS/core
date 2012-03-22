<%@ include file="/html/portal/init.jsp" %>

<%
Boolean staleSession = (Boolean)session.getAttribute(WebKeys.STALE_SESSION);

String userLogin = user.getEmailAddress();
if (company.getAuthType().equals(Company.AUTH_TYPE_ID)) {
	userLogin = user.getUserId();
}
%>

<table border="0" cellpadding="0" cellspacing="0">
<tr>
	<td>
		<font class="gamma" size="2">

		<c:if test="<%= staleSession != null && staleSession.booleanValue() == true %>">
			<span class="gamma-neg-alert">
			<%= LanguageUtil.get(pageContext, "you-have-been-logged-off-because-you-signed-on-with-this-account-using-a-different-session") %>
			</span>

			<%
			session.invalidate();
			%>
		</c:if>

		<c:if test="<%= SessionErrors.contains(request, PortletActiveException.class.getName()) %>">
			<span class="gamma-neg-alert">
			<%= LanguageUtil.get(pageContext, "this-page-is-part-of-an-inactive-portlet") %>
			</span>
		</c:if>

		<c:if test="<%= SessionErrors.contains(request, PrincipalException.class.getName()) %>">
			<span class="gamma-neg-alert">
			<%= LanguageUtil.get(pageContext, "you-do-not-have-the-roles-required-to-access-this-page") %>
			</span>
		</c:if>

		<c:if test="<%= SessionErrors.contains(request, RequiredLayoutException.class.getName()) %>">
			<span class="gamma-neg-alert">
			<%= LanguageUtil.get(pageContext, "please-contact-the-administrator-because-you-do-not-have-any-pages-configured") %>
			</span>
		</c:if>

		<c:if test="<%= SessionErrors.contains(request, RequiredRoleException.class.getName()) %>">
			<span class="gamma-neg-alert">
			<%= LanguageUtil.get(pageContext, "please-contact-the-administrator-because-you-do-not-have-any-roles") %>
			</span>
		</c:if>

		<c:if test="<%= SessionErrors.contains(request, UserActiveException.class.getName()) %>">
			<span class="gamma-neg-alert">
			<%= LanguageUtil.format(pageContext, "your-account-with-login-x-is-not-active", new LanguageWrapper[] {new LanguageWrapper("", user.getFullName(), ""), new LanguageWrapper("<b><i>", userLogin, "</i></b>")}, false) %><br><br>
			</span>

			<%= LanguageUtil.format(pageContext, "if-you-are-not-x-logout-and-try-again", user.getFullName(), false) %><br><br>
		</c:if>

		</font>
	</td>
</tr>
</table>