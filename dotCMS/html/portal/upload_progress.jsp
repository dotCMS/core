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

<%
String uploadFileName = GetterUtil.getString((String)session.getAttribute(LiferayDiskFileUpload.FILE_NAME));

int uploadPercent = 0;
Float uploadPercentFloat = (Float)session.getAttribute(LiferayDiskFileUpload.PERCENT);
if (uploadPercentFloat != null) {
	uploadPercent = (int)(uploadPercentFloat.floatValue() * 100);
}

int factor = 3;
int complete = uploadPercent * factor - 10;
int incomplete = 100 * factor - complete - 10;
%>

<liferay:include page="/html/common/top.jsp">
	<liferay:param name="html_title" value="<%= LanguageUtil.get(pageContext, \"file-upload\") %>" />
	<c:if test="<%= uploadPercent < 100 %>">
		<liferay:param name="refresh_rate" value="1" />
	</c:if>
	<liferay:param name="cache_control" value="0" />
	<liferay:param name="show_top" value="false" />
</liferay:include>

<script language="JavaScript">
	self.focus();
</script>

<table border="0" cellpadding="4" cellspacing="0">
<tr>
	<td align="center">
		<font class="bg" size="2">
		<%= uploadPercent %>%
		</font>
	</td>
</tr>
<tr>
	<td align="center">
		<table border="0" cellpadding="0" cellspacing="0">
		<tr>
			<c:choose>
				<c:when test="<%= uploadPercent == 0 %>">
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_incomplete_left.gif" vspace="0" width="5"></td>
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_incomplete_middle.gif" vspace="0" width="<%= 100 * factor - 10 %>"></td>
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_incomplete_right.gif" vspace="0" width="5"></td>
				</c:when>
				<c:when test="<%= complete <= 0 %>">
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_complete_left.gif" vspace="0" width="5"></td>
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_incomplete_middle.gif" vspace="0" width="<%= 100 * factor - 10 %>"></td>
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_incomplete_right.gif" vspace="0" width="5"></td>
				</c:when>
				<c:when test="<%= complete < (100 * factor - 10) %>">
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_complete_left.gif" vspace="0" width="5"></td>
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_complete_middle.gif" vspace="0" width="<%= complete %>"></td>
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_incomplete_middle.gif" vspace="0" width="<%= incomplete %>"></td>
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_incomplete_right.gif" vspace="0" width="5"></td>
				</c:when>
				<c:when test="<%= (complete >= (100 * factor - 10)) && (uploadPercent != 100) %>">
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_complete_left.gif" vspace="0" width="5"></td>
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_complete_middle.gif" vspace="0" width="<%= 100 * factor - 10 %>"></td>
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_incomplete_right.gif" vspace="0" width="5"></td>
				</c:when>
				<c:when test="<%= uploadPercent == 100 %>">
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_complete_left.gif" vspace="0" width="5"></td>
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_complete_middle.gif" vspace="0" width="<%= complete %>"></td>
					<td><img height="23" hspace="0" src="<%= COMMON_IMG %>/portal/progress_bar_complete_right.gif" vspace="0" width="5"></td>
				</c:when>
			</c:choose>
		</tr>
		</table>
	</td>
</tr>
<tr>
	<td align="center">
		<font class="bg" size="2">

		<c:if test="<%= uploadPercent < 100 %>">
		<%= LanguageUtil.get(pageContext, "uploading") %>:
		</c:if>

		<c:if test="<%= uploadPercent == 100 %>">
		<%= LanguageUtil.get(pageContext, "upload-completed") %>:
		</c:if>

		</font>
	</td>
</tr>
<tr>
	<td align="center">
		<font class="bg" size="2"><i>
		<%= uploadFileName %>
		</i></font>
	</td>
</tr>
</table>

<c:if test="<%= uploadPercent == 100 %>">
	<script language="JavaScript">
		setTimeout("window.close()", 3000);
	</script>
</c:if>

</body>

</html>