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

<%
DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
df.setTimeZone(timeZone);
%>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"live-sessions\") %>" />

	<table border="0" cellpadding="0" cellspacing="0" width="95%">
	<tr>
		<td>
			<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td nowrap>
					<font class="bg" size="2"><b>
					<%= LanguageUtil.get(pageContext, "session-id") %>
					</b></font>
				</td>
				<td width="10">
					&nbsp;
				</td>
				<td nowrap>
					<font class="bg" size="2"><b>
					<%= LanguageUtil.get(pageContext, "user-id") %>
					</b></font>
				</td>
				<td width="10">
					&nbsp;
				</td>
				<td nowrap>
					<font class="bg" size="2"><b>
					<%= LanguageUtil.get(pageContext, "name") %>
					</b></font>
				</td>
				<td width="10">
					&nbsp;
				</td>
				<td nowrap>
					<font class="bg" size="2"><b>
					<%= LanguageUtil.get(pageContext, "email-address") %>
					</b></font>
				</td>
				<td width="10">
					&nbsp;
				</td>
				<td nowrap>
					<font class="bg" size="2"><b>
					<%= LanguageUtil.get(pageContext, "last-request") %>
					</b></font>
				</td>
				<td width="10">
					&nbsp;
				</td>
				<td nowrap>
					<font class="bg" size="2"><b>
					<%= LanguageUtil.get(pageContext, "num-of-hits") %>
					</b></font>
				</td>
				<td width="10">
					&nbsp;
				</td>
				<td nowrap>
					<font class="bg" size="2"><b>
					<%= LanguageUtil.get(pageContext, "details") %>
					</b></font>
				</td>
			</tr>

			<%
			Map currentUsers = (Map)WebAppPool.get(company.getCompanyId(), WebKeys.CURRENT_USERS);

			Map.Entry[] currentUsersArray = (Map.Entry[])currentUsers.entrySet().toArray(new Map.Entry[0]);
			UserTracker[] userTrackers = new UserTracker[currentUsersArray.length];

			for (int i = 0; i < currentUsersArray.length; i++) {
				Map.Entry mapEntry = currentUsersArray[i];

				userTrackers[i] = (UserTracker)mapEntry.getValue();
			}

			Arrays.sort(userTrackers, new UserTrackerModifiedDateComparator());

			for (int i = 0; i < userTrackers.length; i++) {
				UserTracker userTracker = userTrackers[i];

				User selUser = null;
				try {
					selUser = com.dotmarketing.business.APILocator.getUserAPI().loadUserById(userTracker.getUserId(),com.dotmarketing.business.APILocator.getUserAPI().getSystemUser(),false);
				}
				catch (com.dotmarketing.business.NoSuchUserException nsue) {
				}
			%>

				<tr>
					<td nowrap>
						<font class="bg" size="2">
						<%= userTracker.getUserTrackerId() %>
						</font>
					</td>
					<td width="10">
						&nbsp;
					</td>
					<td nowrap>
						<font class="bg" size="2">
						<%= userTracker.getUserId() %>
						</font>
					</td>
					<td width="10">
						&nbsp;
					</td>
					<td nowrap>
						<font class="bg" size="2">

						<% if (selUser != null ) {%>
							<a class="bg" href="<portlet:actionURL><portlet:param name="struts_action" value="/admin/edit_user_profile" /><portlet:param name="p_u_e_a" value="<%= selUser.getEmailAddress() %>" /></portlet:actionURL>">
							<%= selUser.getFullName() %>
							</a>
						<%}%>

						<% if (selUser == null ) {%>
							<i><%= userTracker.getFullName() %></i>
						<%}%>

						</font>
					</td>
					<td width="10">
						&nbsp;
					</td>
					<td nowrap>
						<font class="bg" size="2">

						<% if (selUser != null ) {%>
							<%= selUser.getEmailAddress() %>
						<%}%>

						<% if (selUser == null ) {%>
							<i><%= userTracker.getEmailAddress() %></i>
						<%}%>

						</font>
					</td>
					<td width="10">
						&nbsp;
					</td>
					<td nowrap>
						<font class="bg" size="2">
						<%= df.format(userTracker.getModifiedDate()) %>
						</font>
					</td>
					<td width="10">
						&nbsp;
					</td>
					<td nowrap>
						<font class="bg" size="2">
						<%= userTracker.getHits() %>
						</font>
					</td>
					<td width="10">
						&nbsp;
					</td>
					<td nowrap>
						<font class="bg" size="2"><a class="bg" href="<portlet:renderURL><portlet:param name="struts_action" value="/admin/list_session_details" /><portlet:param name="session_id" value="<%= userTracker.getUserTrackerId() %>" /></portlet:renderURL>">
						<%= LanguageUtil.get(pageContext, "view") %>
						</a></font>
					</td>
				</tr>

			<%
			}
			%>

			</table>
		</td>
	</tr>
	</table>
</liferay:box>