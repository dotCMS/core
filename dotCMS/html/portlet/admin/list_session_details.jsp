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
String sessionId = request.getParameter("session_id");

DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
df.setTimeZone(timeZone);
%>


<%@page import="com.dotmarketing.business.APILocator"%><liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"live-sessions\") %>" />

	<table border="0" cellpadding="0" cellspacing="0" width="95%">
	<tr>
		<td>

			<%
			Map currentUsers = (Map)WebAppPool.get(company.getCompanyId(), WebKeys.CURRENT_USERS);
			UserTracker userTracker = (UserTracker)currentUsers.get(sessionId);
			%>

			<c:if test="<%=  userTracker == null %>">
				<table border="0" cellpadding="0" cellspacing="0">
				<tr>
					<td>
						<font class="bg" size="2">
						<%= LanguageUtil.get(pageContext, "session-id-not-found") %>
						</font>
					</td>
				</tr>
				</table>
			</c:if>

			<c:if test="<%=  userTracker != null %>">

				<%
				User selUser = APILocator.getUserAPI().loadUserById(userTracker.getUserId(),APILocator.getUserAPI().getSystemUser(),false);
				%>

				<table border="0" cellpadding="0" cellspacing="0">
				<tr>
					<td nowrap>
						<font class="bg" size="2"><b>
						<%= LanguageUtil.get(pageContext, "session-id") %>
						</b></font>
					</td>
					<td width="10">&nbsp;
						
					</td>
					<td nowrap>
						<font class="bg" size="2"><b>
						<%= LanguageUtil.get(pageContext, "user-id") %>
						</b></font>
					</td>
					<td width="10">&nbsp;
						
					</td>
					<td nowrap>
						<font class="bg" size="2"><b>
						<%= LanguageUtil.get(pageContext, "name") %>
						</b></font>
					</td>
					<td width="10">&nbsp;
						
					</td>
					<td nowrap>
						<font class="bg" size="2"><b>
						<%= LanguageUtil.get(pageContext, "email-address") %>
						</b></font>
					</td>
					<td width="10">&nbsp;
						
					</td>
					<td nowrap>
						<font class="bg" size="2"><b>
						<%= LanguageUtil.get(pageContext, "last-request") %>
						</b></font>
					</td>
					<td width="10">&nbsp;
						
					</td>
					<td nowrap>
						<font class="bg" size="2"><b>
						<%= LanguageUtil.get(pageContext, "num-of-hits") %>
						</b></font>
					</td>
				</tr>
				<tr>
					<td nowrap>
						<font class="bg" size="2">
						<%= userTracker.getUserTrackerId() %>
						</font>
					</td>
					<td width="10">&nbsp;
						
					</td>
					<td nowrap>
						<font class="bg" size="2">
						<%= userTracker.getUserId() %>
						</font>
					</td>
					<td width="10">&nbsp;
						
					</td>
					<td nowrap>
						<font class="bg" size="2">

						<c:if test="<%= selUser != null %>">
							<a class="bg" href="<portlet:actionURL><portlet:param name="struts_action" value="/admin/edit_user_profile" /><portlet:param name="p_u_e_a" value="<%= selUser.getEmailAddress() %>" /></portlet:actionURL>">
							<%= selUser.getFullName() %>
							</a>
						</c:if>

						<c:if test="<%= selUser == null %>">
							<%= LanguageUtil.get(pageContext, "not-available") %>
						</c:if>

						</font>
					</td>
					<td width="10">&nbsp;
						
					</td>
					<td nowrap>
						<font class="bg" size="2">

						<c:if test="<%= selUser != null %>">
							<%= selUser.getEmailAddress() %>
						</c:if>

						<c:if test="<%= selUser == null %>">
							<%= LanguageUtil.get(pageContext, "not-available") %>
						</c:if>

						</font>
					</td>
					<td width="10">&nbsp;
						
					</td>
					<td nowrap>
						<font class="bg" size="2">
						<%= df.format(userTracker.getModifiedDate()) %>
						</font>
					</td>
					<td width="10">&nbsp;
						
					</td>
					<td nowrap>
						<font class="bg" size="2">
						<%= userTracker.getHits() %>
						</font>
					</td>
				</tr>
				</table>

				<br>

				<table border="0" cellpadding="0" cellspacing="0">
				<tr>
					<td nowrap>
						<font class="bg" size="2"><b>
						<%= LanguageUtil.get(pageContext, "browser-os-type") %>
						</b></font>
					</td>
				</tr>
				<tr>
					<td nowrap>
						<font class="bg" size="2">
						<%= userTracker.getUserAgent() %>
						</font>
					</td>
				</tr>
				</table>

				<br>

				<table border="0" cellpadding="0" cellspacing="0">
				<tr>
					<td nowrap>
						<font class="bg" size="2"><b>
						<%= LanguageUtil.get(pageContext, "remote-host-ip") %>
						</b></font>
					</td>
				</tr>
				<tr>
					<td nowrap>
						<font class="bg" size="2">
						<%= userTracker.getRemoteAddr() %> / <%= userTracker.getRemoteHost() %>
						</font>
					</td>
				</tr>
				</table>

				<table border="0" cellpadding="0" cellspacing="0" width="100%">
				<tr>
					<td>
						<br>
					</td>
				</tr>
				<tr>
					<td background="<%= SKIN_CSS_IMG %>beta_dotted_x.gif"><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
				</tr>
				<tr>
					<td>
						<br>
					</td>
				</tr>
				<tr>
					<td>
						<table border="0" cellpadding="4" cellspacing="0" width="100%">
						<tr>
							<td class="beta<%= BrowserSniffer.is_ie(request) ? "-gradient" : StringPool.BLANK %>">
								<font class="beta" size="2"><b>
								<%= LanguageUtil.get(pageContext, "accessed-urls") %>
								</b></font>
							</td>
							<td align="right" class="beta<%= BrowserSniffer.is_ie(request) ? "-gradient" : StringPool.BLANK %>">
								<font class="bg" size="1">
								[
								<a class="bg" href="javascript: document.getElementById('<portlet:namespace />accessed_urls').style.display = ''; void(''); self.focus();"><%= LanguageUtil.get(pageContext, "show") %></a>
								/
								<a class="bg" href="javascript: document.getElementById('<portlet:namespace />accessed_urls').style.display = 'none'; void(''); self.focus();"><%= LanguageUtil.get(pageContext, "hide") %></a>
								]
								</font>
							</td>
						</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td id="<portlet:namespace />accessed_urls" style="display: none;">
						<table border="0" cellpadding="4" cellspacing="0" width="100%">

						<%
						List paths = userTracker.getPaths();

						for (int i = 0; i < paths.size(); i++) {
							UserTrackerPath userTrackerPath = (UserTrackerPath)paths.get(i);

							String className = "gamma";
							if (MathUtil.isEven(i)) {
								className = "bg";
							}
						%>

							<tr class="<%= className %>">
								<td valign="top">
									<font class="<%= className %>" size="1">
									<%= userTrackerPath.getPath() %>
									</font>
								</td>
								<td nowrap valign="top">
									<font class="<%= className %>" size="1">
									<%= df.format(userTrackerPath.getPathDate()) %>
									</font>
								</td>
							</tr>

						<%
						}
						%>

						</table>
					</td>
				</tr>
				<tr>
					<td>
						<table border="0" cellpadding="4" cellspacing="0" width="100%">
						<tr>
							<td class="beta<%= BrowserSniffer.is_ie(request) ? "-gradient" : StringPool.BLANK %>">
								<font class="beta" size="2"><b>
								<%= LanguageUtil.get(pageContext, "session-attributes") %>
								</b></font>
							</td>
							<td align="right" class="beta<%= BrowserSniffer.is_ie(request) ? "-gradient" : StringPool.BLANK %>">
								<font class="bg" size="1">
								[
								<a class="bg" href="javascript: document.getElementById('<portlet:namespace />session_attributes').style.display = ''; void(''); self.focus();"><%= LanguageUtil.get(pageContext, "show") %></a>
								/
								<a class="bg" href="javascript: document.getElementById('<portlet:namespace />session_attributes').style.display = 'none'; void(''); self.focus();"><%= LanguageUtil.get(pageContext, "hide") %></a>
								]
								</font>
							</td>
						</tr>
						</table>
					</td>
				</tr>
				<tr>
					<td id="<portlet:namespace />session_attributes" style="display: none;">
						<table border="0" cellpadding="4" cellspacing="0" width="100%">

						<%
						boolean userSessionAlive = true;

						HttpSession userSession = PortalSessionContext.get(sessionId);

						if (userSession != null) {
							try {
								if (company.getCompanyId().equalsIgnoreCase((String)userSession.getAttribute(WebKeys.COMPANY_ID))) {
									int counter = 0;

									Set sortedAttrNames = new TreeSet();

									Enumeration enu = userSession.getAttributeNames();

									while (enu.hasMoreElements()) {
										String attrName = (String)enu.nextElement();

										sortedAttrNames.add(attrName);
									}

									Iterator itr = sortedAttrNames.iterator();

									while (itr.hasNext()) {
										String attrName = (String)itr.next();

										String className = "gamma";
										if (MathUtil.isEven(counter++)) {
											className = "bg";
										}
						%>

										<tr class="<%= className %>">
											<td valign="top">
												<font class="<%= className %>" size="1">
												<%= attrName %>
												</font>
											</td>
										</tr>

						<%
									}
								}
							}
							catch (Exception e) {
								userSessionAlive = false;

								e.printStackTrace();
							}
						}
						%>

						</table>
					</td>
				</tr>
				</table>

				<c:if test="<%= (userSessionAlive && !session.getId().equalsIgnoreCase(sessionId)) %>">
					<br>

					<%
					PortletURL redirectURL = renderResponse.createRenderURL();

					redirectURL.setParameter("struts_action", "/admin/list_sessions");
					%>

                    <button dojoType="dijit.form.Button" onClick="self.location = '<portlet:actionURL><portlet:param name="struts_action" value="/admin/kill_session" /><portlet:param name="redirect" value="<%= redirectURL.toString() %>" /><portlet:param name="session_id" value="<%= sessionId %>" /></portlet:actionURL>';">
                        <%= LanguageUtil.get(pageContext, "kill-session") %>
                    </button>

				</c:if>
			</c:if>
		</td>
	</tr>
	</table>
</liferay:box>
