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

<%@ include file="/html/common/init.jsp" %>

<%
String namespace = ParamUtil.getString(request, "namespace");

String monthParam = request.getParameter("sel_month");
String dayParam = request.getParameter("sel_day");
String yearParam = request.getParameter("sel_year");

String showTopPattern = ParamUtil.getString(request, "show_top_pattern");
boolean showMonthSel = ParamUtil.get(request, "show_month_sel", true);

Calendar selCal = new GregorianCalendar(timeZone, locale);
selCal.set(Calendar.DATE, 1);

try {
	selCal.set(Calendar.YEAR, Integer.parseInt(yearParam));
}
catch (NumberFormatException nfe) {
}

try {
	selCal.set(Calendar.MONTH, Integer.parseInt(monthParam));
}
catch (NumberFormatException nfe) {
}

try {
	int maxDayOfMonth = selCal.getActualMaximum(Calendar.DATE);

	int dayParamInt = Integer.parseInt(dayParam);

	if (dayParamInt > maxDayOfMonth) {
		dayParamInt = maxDayOfMonth;
	}

	selCal.set(Calendar.DATE, dayParamInt);
}
catch (NumberFormatException nfe) {
}

int selMonth = selCal.get(Calendar.MONTH);
int selDay = selCal.get(Calendar.DATE);
int selYear = selCal.get(Calendar.YEAR);

int maxDayOfMonth = selCal.getActualMaximum(Calendar.DATE);

selCal.set(Calendar.DATE, 1);
int dayOfWeek = selCal.get(Calendar.DAY_OF_WEEK);
selCal.set(Calendar.DATE, selDay);

Calendar curCal = new GregorianCalendar(timeZone, locale);
int curMonth = curCal.get(Calendar.MONTH);
int curDay = curCal.get(Calendar.DATE);
int curYear = curCal.get(Calendar.YEAR);

int[] monthIds = CalendarUtil.getMonthIds();
String[] months = CalendarUtil.getMonths(locale);

Set calendarData = (Set)request.getAttribute(WebKeys.CALENDAR_DATA);
%>

<table border="0" cellpadding="0" cellspacing="0" width="190">
<tr>
	<td>
		<table border="0" cellpadding="0" cellspacing="0" width="100%">

		<c:if test="<%= Validator.isNotNull(showTopPattern) %>">

			<%
			SimpleDateFormat sdf = new SimpleDateFormat(showTopPattern, locale);
			sdf.setTimeZone(timeZone);
			%>

			<tr class="gamma">
				<td class="beta" colspan="15"><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			</tr>
			<tr class="gamma">
				<td class="beta"><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
				<td align="center" colspan="13" height="20">
					<font class="gamma" size="2">
					<b><%= sdf.format(selCal.getTime()) %></b>
					</font>
				</td>
				<td class="beta"><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			</tr>
		</c:if>

		<tr class="beta">
			<td><img border="0" height="22" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			<td align="center" width="26">
				<font class="gamma" size="2"><b><%= LanguageUtil.get(pageContext, "sunday-abbreviation") %></font>
			</td>
			<td><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			<td align="center" width="26">
				<font class="gamma" size="2"><b><%= LanguageUtil.get(pageContext, "monday-abbreviation") %></font>
			</td>
			<td><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			<td align="center" width="26">
				<font class="gamma" size="2"><b><%= LanguageUtil.get(pageContext, "tuesday-abbreviation") %></font>
			</td>
			<td><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			<td align="center" width="26">
				<font class="gamma" size="2"><b><%= LanguageUtil.get(pageContext, "wednesday-abbreviation") %></font>
			</td>
			<td><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			<td align="center" width="26">
				<font class="gamma" size="2"><b><%= LanguageUtil.get(pageContext, "thursday-abbreviation") %></font>
			</td>
			<td><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			<td align="center" width="26">
				<font class="gamma" size="2"><b><%= LanguageUtil.get(pageContext, "friday-abbreviation") %></font>
			</td>
			<td><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			<td align="center" width="26">
				<font class="gamma" size="2"><b><%= LanguageUtil.get(pageContext, "saturday-abbreviation") %></font>
			</td>
			<td><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
		</tr>
		<tr class="gamma">

		<%
		for (int i = 1; i < dayOfWeek; i++) {
		%>

			<td class="beta"><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			<td height="25" width="26"><font class="gamma" size="1">&nbsp;</font></td>

		<%
		}

		for (int i = 1; i <= maxDayOfMonth; i++) {
			if (dayOfWeek > 7) {
		%>

			<td class="beta"><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
		</tr>
		<tr class="gamma">
			<td class="beta" colspan="15"><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
		</tr>
		<tr class="gamma">

		<%
				dayOfWeek = 1;
			}

			dayOfWeek++;

			Calendar tempCal = (Calendar)selCal.clone();
			tempCal.set(Calendar.MONTH, selMonth);
			tempCal.set(Calendar.DATE, i);
			tempCal.set(Calendar.YEAR, selYear);

			boolean hasData = calendarData.contains(new Integer(i));

			String className = "gamma";
			if (selMonth == curMonth && i == curDay && selYear == curYear) {
				className = "beta";
			}
		%>

			<td class="beta"><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			<td align="center" class="<%= className %>" height="25" valign="top" width="26">
				<table border="0" cellpadding="0" cellspacing="0" width="24">
				<tr>
					<td align="center" height="20" valign="top">
						<font class="<%= className %>" size="1">
						<a class="<%= className %>" href="javascript: <%= namespace %>updateCalendar(<%= selMonth %>, <%= i %>, <%= selYear %>);"><%= i %></a>
						</font>
					</td>
				</tr>

				<c:if test="<%= hasData %>">
					<tr>
						<td class="alpha"><img border="0" height="3" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
					</tr>
				</c:if>

				</table>
			</td>

		<%
		}

		for (int i = 7; i >= dayOfWeek; i--) {
		%>

			<td class="beta"><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			<td height="25" width="26"><font class="gamma" size="1">&nbsp;</font></td>

		<%
		}
		%>

			<td class="beta"><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
		</tr>
		<tr class="gamma">
			<td class="beta" colspan="15"><img border="0" height="1" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
		</tr>
		</table>
	</td>
</tr>

<c:if test="<%= showMonthSel %>">
	<tr>
		<td>
			<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td colspan="2"><img border="0" height="3" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
			</tr>
			<tr>
				<td>
					<select name="<%= namespace %>_sel_month" onChange="<%= namespace %>updateCalendar();">

						<%
						for (int i = 0; i < months.length; i++) {
						%>

							<option <%= (monthIds[i] == selMonth) ? "selected" : "" %> value="<%= monthIds[i] %>"><%= months[i] %></option>

						<%
						}
						%>

					</select>

					<select name="<%= namespace %>_sel_year" onChange="<%= namespace %>updateCalendar();">

						<%
						for (int i = -10; i <= 10; i++) {
						%>

							<option <%= ((curYear - selYear + i) == 0) ? "selected" : "" %> value="<%= curYear + i %>"><%= curYear + i %></option>

						<%
						}
						%>

					</select>
				</td>
			</tr>
			</table>
		</td>
	</tr>
</c:if>

</table>