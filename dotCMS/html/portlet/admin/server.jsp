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

<form action="<portlet:actionURL><portlet:param name="struts_action" value="/admin/server" /></portlet:actionURL>" method="post" name="<portlet:namespace />fm">
<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="">

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"server\") %>" />

	<table border="0" cellpadding="0" cellspacing="0" width="95%">
	<tr>
		<td>
			<table border="0" cellpadding="0" cellspacing="0" width="100%">
			<tr>
				<td>
					<font class="bg" size="2">
					<b><%= ReleaseInfo.getReleaseInfo() %></b>
					</font>
				</td>
			</tr>
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
					<font class="bg" size="2">

					<%
					Date uptime = (Date)SimpleCachePool.get(StartupAction.class.getName() + ".uptime");
					Date now = new Date();

					long uptimeDiff = now.getTime() - uptime.getTime();
					long days = uptimeDiff / Time.DAY;
					long hours = (uptimeDiff / Time.HOUR) % 24;
					long minutes = (uptimeDiff / Time.MINUTE) % 60;
					long seconds = (uptimeDiff / Time.SECOND) % 60;

					NumberFormat nf = NumberFormat.getInstance();
					nf.setMaximumIntegerDigits(2);
					nf.setMinimumIntegerDigits(2);
					%>

					<b><%= LanguageUtil.get(pageContext, "uptime") %>:</b> <c:if test="<%= days > 0 %>"><%= days %> <%= LanguageUtil.get(pageContext, ((days > 1) ? "days" : "day")) %>,</c:if> <%= nf.format(hours) %>:<%= nf.format(minutes) %>:<%= nf.format(seconds) %><br><br>

					<%
					nf = NumberFormat.getInstance(locale);
					%>

					<b><%= LanguageUtil.get(pageContext, "free-memory") %>:</b> <%= nf.format(Runtime.getRuntime().freeMemory()) %> <%= LanguageUtil.get(pageContext, "bytes") %><br>
					<b><%= LanguageUtil.get(pageContext, "total-memory") %>:</b> <%= nf.format(Runtime.getRuntime().totalMemory()) %> <%= LanguageUtil.get(pageContext, "bytes") %><br>
					<b><%= LanguageUtil.get(pageContext, "maximum-memory") %>:</b> <%= nf.format(Runtime.getRuntime().maxMemory()) %> <%= LanguageUtil.get(pageContext, "bytes") %>

					</font>
				</td>
			</tr>
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
					<table border="0" cellpadding="0" cellspacing="0">
					<tr>
						<td>
							<font class="bg" size="2">
							<%= LanguageUtil.get(pageContext, "run-the-garbage-collector-to-free-up-memory") %>
							</font>
						</td>
						<td width="30">&nbsp;
							
						</td>
						<td>
                            
                           <button dojoType="dijit.form.Button" 
                           onClick="document.<portlet:namespace />fm.<portlet:namespace /><%= Constants.CMD %>.value = 'gc'; submitForm(document.<portlet:namespace />fm);">
                               <%= LanguageUtil.get(pageContext, "run-garbage-collector") %>
                            </button>
                            
						</td>
					</tr>
					</table>
				</td>
			</tr>
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
					<table border="0" cellpadding="0" cellspacing="0">
					<tr>
						<td colspan="3">
							<font class="bg" size="2">
							<%= LanguageUtil.get(pageContext, "shutdown-the-server-in-the-specified-number-of-minutes") %>
							</font>
						</td>
					</tr>
					<tr>
						<td colspan="3">
							<br>
						</td>
					</tr>
					<tr>
						<td>
							<font class="bg" size="2"><%= LanguageUtil.get(pageContext, "number-of-minutes") %></font>
						</td>
						<td width="30">&nbsp;
							
						</td>
						<td>
							<input class="form-text" name="<portlet:namespace />shutdown_minutes" size="3" type="text">
						</td>
					</tr>
					<tr>
						<td colspan="3">
							<br>
						</td>
					</tr>
					<tr>
						<td>
							<font class="bg" size="2"><%= LanguageUtil.get(pageContext, "custom-message") %></font>
						</td>
						<td></td>
						<td>
							<textarea class="form-text" cols="70" name="<portlet:namespace />shutdown_message"  rows="5"></textarea>
						</td>
					</tr>
					<tr>
						<td colspan="3">
							<br>
						</td>
					</tr>
					<tr>
						<td colspan="2"></td>
						<td>
                            <button dojoType="dijit.form.Button" onClick="document.<portlet:namespace />fm.<portlet:namespace /><%= Constants.CMD %>.value = 'shutdown'; submitForm(document.<portlet:namespace />fm);">
                               <%= LanguageUtil.get(pageContext, "shutdown") %>
                            </button>
						</td>
					</tr>
					</table>
				</td>
			</tr>
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
							<font class="bg" size="2"><b>
							<%= LanguageUtil.get(pageContext, "log-levels") %>:
							</b></font>
						</td>
						<td align="right" class="beta<%= BrowserSniffer.is_ie(request) ? "-gradient" : StringPool.BLANK %>">
							<font class="bg" size="1">
							[
							<a class="bg" href="javascript: document.getElementById('<portlet:namespace />log_levels').style.display = ''; void(''); self.focus();"><%= LanguageUtil.get(pageContext, "show") %></a>
							/
							<a class="bg" href="javascript: document.getElementById('<portlet:namespace />log_levels').style.display = 'none'; void(''); self.focus();"><%= LanguageUtil.get(pageContext, "hide") %></a>
							]
							</font>
						</td>
					</tr>
					</table>
				</td>
			</tr>
			<tr>
				<td id="<portlet:namespace />log_levels" style="display: none;">

					<%
					Map currentLoggerNames = new TreeMap();

					Enumeration enu = LogManager.getCurrentLoggers();

					while (enu.hasMoreElements()) {
						Logger logger = (Logger)enu.nextElement();

						currentLoggerNames.put(logger.getName(), logger);
					}
					%>

					<table border="0" cellpadding="4" cellspacing="0" width="100%">

					<%
					int counter = 0;

					Iterator itr = currentLoggerNames.entrySet().iterator();

					while (itr.hasNext()) {
						Map.Entry entry = (Map.Entry)itr.next();

						String name = (String)entry.getKey();
						Logger logger = (Logger)entry.getValue();
					%>

						<c:if test="<%= logger.getLevel() != null %>">

							<%
							String className = "gamma";
							if (MathUtil.isEven(counter++)) {
								className = "bg";
							}
							%>

							<tr class="<%= className %>">
								<td>
									<font class="<%= className %>" size="2">
									<%= name %>
									</font>
								</td>
								<td>
									<select class="form-button" name="<portlet:namespace />log_level_<%= name %>">

										<%
										for (int i = 0; i < Levels.ALL_LEVELS.length; i++) {
										%>

											<option <%= logger.getLevel().equals(Levels.ALL_LEVELS[i]) ? "selected" : "" %> value="<%= Levels.ALL_LEVELS[i] %>"><%= Levels.ALL_LEVELS[i] %></option>

										<%
										}
										%>

									</select>
								</td>
							</tr>
						</c:if>

					<%
					}
					%>

					</table>

					<br>

					<table border="0" cellpadding="0" cellspacing="0">
					<tr>
						<td>
                            <button dojoType="dijit.form.Button" onClick="document.<portlet:namespace />fm.<portlet:namespace /><%= Constants.CMD %>.value = 'log_levels'; submitForm(document.<portlet:namespace />fm);">
                               <%= LanguageUtil.get(pageContext, "update") %>
                            </button>
						</td>
					</tr>
					</table>

					<br>
				</td>
			</tr>
			<tr>
				<td>
					<table border="0" cellpadding="4" cellspacing="0" width="100%">
					<tr>
						<td class="beta<%= BrowserSniffer.is_ie(request) ? "-gradient" : StringPool.BLANK %>">
							<font class="bg" size="2"><b>
							<%= LanguageUtil.get(pageContext, "system-properties") %>:
							</b></font>
						</td>
						<td align="right" class="beta<%= BrowserSniffer.is_ie(request) ? "-gradient" : StringPool.BLANK %>">
							<font class="bg" size="1">
							[
							<a class="bg" href="javascript: document.getElementById('<portlet:namespace />system_properties').style.display = ''; void(''); self.focus();"><%= LanguageUtil.get(pageContext, "show") %></a>
							/
							<a class="bg" href="javascript: document.getElementById('<portlet:namespace />system_properties').style.display = 'none'; void(''); self.focus();"><%= LanguageUtil.get(pageContext, "hide") %></a>
							]
							</font>
						</td>
					</tr>
					</table>
				</td>
			</tr>
			<tr>
				<td id="<portlet:namespace />system_properties" style="display: none;">
					<table border="0" cellpadding="4" cellspacing="0" width="100%">

					<%
					counter = 0;

					Properties systemProps = new SortedProperties();

					PropertiesUtil.copyProperties(System.getProperties(), systemProps);

					enu = systemProps.propertyNames();

					while (enu.hasMoreElements()) {
						String name = (String)enu.nextElement();

						String className = "gamma";
						if (MathUtil.isEven(counter++)) {
							className = "bg";
						}
					%>

						<tr>
							<td class="<%= className %>">
								<font class="<%= className %>" size="1">
								<%= StringUtil.shorten(name, 65) %>
								</font>
							</td>
							<td class="<%= className %>">
								<font class="<%= className %>" size="1">
								<%= StringUtil.shorten(System.getProperty(name), 75) %>
								</font>
							</td>
						</tr>

					<%
					}
					%>

					</table>

					<br>
				</td>
			</tr>
			<tr>
				<td>
					<table border="0" cellpadding="4" cellspacing="0" width="100%">
					<tr>
						<td class="beta<%= BrowserSniffer.is_ie(request) ? "-gradient" : StringPool.BLANK %>">
							<font class="bg" size="2"><b>
							<%= LanguageUtil.get(pageContext, "portal-properties") %>:
							</b></font>
						</td>
						<td align="right" class="beta<%= BrowserSniffer.is_ie(request) ? "-gradient" : StringPool.BLANK %>">
							<font class="bg" size="1">
							[
							<a class="bg" href="javascript: document.getElementById('<portlet:namespace />portal_properties').style.display = ''; void(''); self.focus();"><%= LanguageUtil.get(pageContext, "show") %></a>
							/
							<a class="bg" href="javascript: document.getElementById('<portlet:namespace />portal_properties').style.display = 'none'; void(''); self.focus();"><%= LanguageUtil.get(pageContext, "hide") %></a>
							]
							</font>
						</td>
					</tr>
					</table>
				</td>
			</tr>
			<tr>
				<td id="<portlet:namespace />portal_properties" style="display: none;">
					<table border="0" cellpadding="4" cellspacing="0" width="100%">

					<%
					counter = 0;

					Properties portalProps = new SortedProperties();

					PropertiesUtil.copyProperties(PropsUtil.getProperties(), portalProps);

					enu = portalProps.propertyNames();

					while (enu.hasMoreElements()) {
						String name = (String)enu.nextElement();

						String className = "gamma";
						if (MathUtil.isEven(counter++)) {
							className = "bg";
						}
					%>

						<tr>
							<td class="<%= className %>">
								<font class="<%= className %>" size="1">
								<%= StringUtil.shorten(name, 65) %>
								</font>
							</td>
							<td class="<%= className %>">
								<font class="<%= className %>" size="1">
								<%= StringUtil.shorten(PropsUtil.get(name), 75) %>
								</font>
							</td>
						</tr>

					<%
					}
					%>

					</table>
				</td>
			</tr>
			</table>
		</td>
	</tr>
	</table>
</liferay:box>

</form>