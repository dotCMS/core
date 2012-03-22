<%
/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
%>

<%@ include file="/html/portal/init.jsp" %>

<table border="0" cellpadding="0" cellspacing="0">

<form action="<%= CTX_PATH %>/portal/change_password" method="post" name="fm">
<input name="<%= Constants.CMD %>" type="hidden" value="password">
<input name="referer" type="hidden" value="<%= CTX_PATH %>/portal/last_path">
<input name="p_u_e_a" type="hidden" value="<%= user.getEmailAddress() %>">
<input name="password_reset" type="hidden" value="false">

<tr>
	<td>
		<table border="0" cellpadding="0" cellspacing="2">
		<tr>
			<td>
				<font class="gamma" size="2"><span class="gamma-neg-alert">
				<%= LanguageUtil.get(pageContext, "the-system-policy-requires-you-to-change-your-password-at-this-time") %>
				</span></font>
			</td>
		</tr>
		</table>

		<br>

		<table border="0" cellpadding="0" cellspacing="2">

		<c:if test="<%= SessionErrors.contains(request, UserPasswordException.class.getName()) %>">

			<%
			UserPasswordException upe = (UserPasswordException)SessionErrors.get(request, UserPasswordException.class.getName());
			%>

			<tr>
				<td colspan="3">
					<font class="bg" size="1"><span class="bg-neg-alert">

					<c:if test="<%= upe.getType() == UserPasswordException.PASSWORDS_DO_NOT_MATCH %>">
						<%= LanguageUtil.get(pageContext, "please-enter-matching-passwords") %>
					</c:if>

					<c:if test="<%= upe.getType() == UserPasswordException.PASSWORD_INVALID %>">
						<%= LanguageUtil.get(pageContext, "please-enter-a-valid-password") %>
					</c:if>

					<c:if test="<%= upe.getType() == UserPasswordException.PASSWORD_ALREADY_USED %>">
						<%= LanguageUtil.get(pageContext, "please-enter-a-password-that-has-not-already-been-used") %>
					</c:if>

					</span></font>
				</td>
			</tr>
		</c:if>

		<tr>
			<td>
				<font class="gamma" size="2"><b><%= LanguageUtil.get(pageContext, "password") %></b></font>
			</td>
			<td width="10">&nbsp;
				
			</td>
			<td>
				<input class="form-text" name="password_1" size="22" type="password" value="">
			</td>
		</tr>
		<tr>
			<td>
				<font class="gamma" size="2"><b><%= LanguageUtil.get(pageContext, "enter-again") %></b></font>
			</td>
			<td width="10">&nbsp;
				
			</td>
			<td>
				<input class="form-text" name="password_2" size="22" type="password" value="">
			</td>
		</tr>
		<tr>
			<td align="center" colspan="3">
				<br>
                 <button dojoType="dijit.form.Button" onClick="submitForm(document.fm)">
                    <%= LanguageUtil.get(pageContext, "update") %>
                </button>
			</td>
		</tr>
		</table>
	</td>
</tr>

</form>

</table>

<script language="JavaScript">
	document.fm.password_1.focus();
</script>