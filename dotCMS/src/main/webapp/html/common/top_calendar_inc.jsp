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

<link rel="stylesheet" type="text/css" href="<%= contextPath %>/html/js/calendar/calendar-blue.css">

<script language="JavaScript" src="<%= contextPath %>/html/js/calendar/calendar_stripped.js"></script>
<script language="JavaScript"" src="<%= contextPath %>/html/js/calendar/calendar-setup_stripped.js"></script>

<script language="JavaScript">

	<%
	String[] calendarDays = CalendarUtil.getDays(locale, "EEEE");
	%>

	Calendar._DN = new Array(
		"<%= calendarDays[0] %>",
		"<%= calendarDays[1] %>",
		"<%= calendarDays[2] %>",
		"<%= calendarDays[3] %>",
		"<%= calendarDays[4] %>",
		"<%= calendarDays[5] %>",
		"<%= calendarDays[6] %>",
		"<%= calendarDays[0] %>"
	);

	<%
	calendarDays = CalendarUtil.getDays(locale, "EEE");
	%>

	Calendar._SDN = new Array(
		"<%= calendarDays[0] %>",
		"<%= calendarDays[1] %>",
		"<%= calendarDays[2] %>",
		"<%= calendarDays[3] %>",
		"<%= calendarDays[4] %>",
		"<%= calendarDays[5] %>",
		"<%= calendarDays[6] %>",
		"<%= calendarDays[0] %>"
	);

	<%
	String[] calendarMonths = CalendarUtil.getMonths(locale);
	%>

	Calendar._MN = new Array(
		"<%= calendarMonths[0] %>",
		"<%= calendarMonths[1] %>",
		"<%= calendarMonths[2] %>",
		"<%= calendarMonths[3] %>",
		"<%= calendarMonths[4] %>",
		"<%= calendarMonths[5] %>",
		"<%= calendarMonths[6] %>",
		"<%= calendarMonths[7] %>",
		"<%= calendarMonths[8] %>",
		"<%= calendarMonths[9] %>",
		"<%= calendarMonths[10] %>",
		"<%= calendarMonths[11] %>"
	);

	<%
	calendarMonths = CalendarUtil.getMonths(locale, "MMM");
	%>

	Calendar._SMN = new Array(
		"<%= calendarMonths[0] %>",
		"<%= calendarMonths[1] %>",
		"<%= calendarMonths[2] %>",
		"<%= calendarMonths[3] %>",
		"<%= calendarMonths[4] %>",
		"<%= calendarMonths[5] %>",
		"<%= calendarMonths[6] %>",
		"<%= calendarMonths[7] %>",
		"<%= calendarMonths[8] %>",
		"<%= calendarMonths[9] %>",
		"<%= calendarMonths[10] %>",
		"<%= calendarMonths[11] %>"
	);

	Calendar._TT = {};

	Calendar._TT["ABOUT"] = "<%= LanguageUtil.get(pageContext, "date-selection") %>";
	Calendar._TT["ABOUT"] = Calendar._TT["ABOUT"].replace("{0}", String.fromCharCode(0x2039));
	Calendar._TT["ABOUT"] = Calendar._TT["ABOUT"].replace("{1}", String.fromCharCode(0x203a));

	Calendar._TT["ABOUT_TIME"] = "";
	Calendar._TT["CLOSE"] = "<%= LanguageUtil.get(pageContext, "close") %>";
	Calendar._TT["DAY_FIRST"] = "Display %s First";
	Calendar._TT["DRAG_TO_MOVE"] = "";
	Calendar._TT["GO_TODAY"] = "<%= LanguageUtil.get(pageContext, "today") %>";
	Calendar._TT["INFO"] = "<%= LanguageUtil.get(pageContext, "help") %>";
	Calendar._TT["NEXT_MONTH"] = "<%= LanguageUtil.get(pageContext, "next-month") %>";
	Calendar._TT["NEXT_YEAR"] = "<%= LanguageUtil.get(pageContext, "next-year") %>";
	Calendar._TT["PART_TODAY"] = "";
	Calendar._TT["PREV_MONTH"] = "<%= LanguageUtil.get(pageContext, "previous-month") %>";
	Calendar._TT["PREV_YEAR"] = "<%= LanguageUtil.get(pageContext, "previous-year") %>";
	Calendar._TT["SEL_DATE"] = "<%= LanguageUtil.get(pageContext, "select-date") %>";
	Calendar._TT["SUN_FIRST"] = "";
	Calendar._TT["TIME_PART"] = "";
	Calendar._TT["TODAY"] = "<%= LanguageUtil.get(pageContext, "today") %>";
	Calendar._TT["WK"] = "";

	Calendar._TT["DEF_DATE_FORMAT"] = "%Y-%m-%d";
	Calendar._TT["TT_DATE_FORMAT"] = "%a, %b %e";

	Calendar._TT["WEEKEND"] = "0,6";
</script>