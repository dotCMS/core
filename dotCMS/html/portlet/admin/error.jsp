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

<%@ include file="/html/portlet/admin/init.jsp" %>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"error\") %>" />

	<table border="0" cellpadding="0" cellspacing="0">
	<tr>
		<td>
			<font class="bg" size="2"><span class="bg-neg-alert">

			<c:if test="<%= SessionErrors.contains(renderRequest, NoSuchPortletException.class.getName()) %>">
				<%= LanguageUtil.get(pageContext, "the-portlet-could-not-be-found") %>
			</c:if>

			<c:if test="<%= SessionErrors.contains(renderRequest, NoSuchRoleException.class.getName()) %>">
				<%= LanguageUtil.get(pageContext, "the-role-could-not-be-found") %>
			</c:if>

			<c:if test="<%= SessionErrors.contains(renderRequest, NoSuchUserException.class.getName()) %>">
				<%= LanguageUtil.get(pageContext, "the-user-could-not-be-found") %>
			</c:if>

			<c:if test="<%= SessionErrors.contains(renderRequest, PrincipalException.class.getName()) %>">
				<%= LanguageUtil.get(pageContext, "you-do-not-have-the-required-permissions") %>
			</c:if>

		
			<c:if test="<%= SessionErrors.contains(renderRequest, RequiredRoleException.class.getName()) %>">
				<%= LanguageUtil.get(pageContext, "the-selected-role-cannot-be-deleted-because-it-is-a-required-system-role") %>
			</c:if>

			<c:if test="<%= SessionErrors.contains(renderRequest, RequiredUserException.class.getName()) %>">
				<%= LanguageUtil.get(pageContext, "you-cannot-delete-or-deactivate-yourself") %>
			</c:if>

			</span></font>
		</td>
	</tr>
	</table>
</liferay:box>