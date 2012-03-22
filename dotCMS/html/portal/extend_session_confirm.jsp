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

<liferay:include page="/html/common/top.jsp">
	<liferay:param name="html_title" value="" />
	<liferay:param name="show_top" value="false" />
</liferay:include>

<%
int sessionTimeout = GetterUtil.getInteger(PropsUtil.get(PropsUtil.SESSION_TIMEOUT));
int sessionTimeoutWarning = GetterUtil.getInteger(PropsUtil.get(PropsUtil.SESSION_TIMEOUT_WARNING));
int sessionTimeoutWarningMinute = sessionTimeoutWarning * (int)Time.MINUTE;

Calendar sessionTimeoutCal = new GregorianCalendar(timeZone);
sessionTimeoutCal.add(Calendar.MILLISECOND, sessionTimeoutWarningMinute);

session.setMaxInactiveInterval(new Integer(sessionTimeoutWarning) * 60);

%>

<script language="JavaScript" event="onLoad()" for="window">
	setTimeout("sessionHasExpired()", <%= sessionTimeoutWarningMinute %>);
</script>

<script language="JavaScript">
	function sessionHasExpired() {
		document.getElementById("session_warning_text").innerHTML = "<%= UnicodeLanguageUtil.get(pageContext, "warning-due-to-inactivity-your-session-has-expired") %>";
		document.getElementById("session_btns").innerHTML = "";
	}
</script>

<form name="fm">

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_width" value="300" /> 
	<table border="0" cellpadding="0" cellspacing="0">
	<tr>
		<td align=center>
			<!--  font class="bg" size="2"><span class="bg-neg-alert" id="session_warning_text">-->
			<font class="bg" size="2"><span style="color:black;" id="session_warning_text">
			<%= LanguageUtil.format(pageContext, "warning-due-to-inactivity-your-session-will-expire", new Object[] {new Integer(sessionTimeoutWarning), DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, locale).format(Time.getDate(sessionTimeoutCal)), timeZone.getDisplayName(false, TimeZone.SHORT, locale), new Integer(sessionTimeout)}, false) %>
			</span></font>
		</td>
	</tr>
	<tr>
		<td>
			<br>
		</td>
	</tr>
	<tr>
		<td align="center" id="session_btns">            
            <button dojoType="dijit.form.Button"  onClick="opener.extendSession(); self.close();" name="ok_btn">
               <%= LanguageUtil.get(pageContext, "OK") %>
            </button>
                	            
            <button dojoType="dijit.form.Button" onClick="self.close();" name="cancel_btn">
               <%= LanguageUtil.get(pageContext, "Cancel") %>
            </button>
		</td>
	</tr>
	</table>
</liferay:box>
</form>

</body>

</html>